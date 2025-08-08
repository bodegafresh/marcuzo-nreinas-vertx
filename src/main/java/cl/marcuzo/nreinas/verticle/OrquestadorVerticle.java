package cl.marcuzo.nreinas.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OrquestadorVerticle coordina la solución distribuida de N-Reinas usando programación dinámica.
 * Gestiona el despliegue de workers, el StateManager y coordina el flujo de trabajo.
 */
public class OrquestadorVerticle extends AbstractVerticle {

  private EventBus eventBus;
  private int workersDeployed = 0;
  private List<String> deploymentIds = new ArrayList<>();

  @Override
  public void start() {
    eventBus = vertx.eventBus();

    eventBus.consumer("orquestador.nreinas", this::handleNQueensRequest);

    System.out.println("OrquestadorVerticle iniciado - Listo para recibir solicitudes");
  }

  private void handleNQueensRequest(io.vertx.core.eventbus.Message<Object> message) {
    JsonObject request = (JsonObject) message.body();
    int N = request.getInteger("N", 8);
    int numWorkers = request.getInteger("workers", Math.min(N, 4)); // Máximo 4 workers por defecto

    System.out.println("Orquestador recibió solicitud para N = " + N + " con " + numWorkers + " workers");

    long startTime = System.currentTimeMillis();

    // Resetear estado anterior
    resetState().onSuccess(v -> {
      // Desplegar StateManager primero
      deployStateManager().onSuccess(stateManagerId -> {
        // Luego desplegar workers
        deployWorkers(N, numWorkers).onSuccess(workerIds -> {
          // Iniciar monitoreo de progreso
          monitorProgress(N, startTime, message);
        }).onFailure(error -> {
          System.out.println("Error desplegando workers: " + error.getMessage());
          message.reply(new JsonObject().put("error", "Failed to deploy workers"));
        });
      }).onFailure(error -> {
        System.out.println("Error desplegando StateManager: " + error.getMessage());
        message.reply(new JsonObject().put("error", "Failed to deploy StateManager"));
      });
    });
  }

  /**
   * Resetea el estado del sistema para una nueva ejecución
   */
  private Future<Void> resetState() {
    Promise<Void> promise = Promise.promise();

    // Undeployar verticles anteriores
    List<Future<Void>> undeployFutures = new ArrayList<>();
    for (String deploymentId : deploymentIds) {
      Promise<Void> undeployPromise = Promise.promise();
      vertx.undeploy(deploymentId, undeployPromise);
      undeployFutures.add(undeployPromise.future());
    }
    deploymentIds.clear();
    workersDeployed = 0;

    if (undeployFutures.isEmpty()) {
      promise.complete();
    } else {
      CompositeFuture.all(new ArrayList<>(undeployFutures)).onComplete(ar -> {
        if (ar.succeeded()) {
          System.out.println("Verticles anteriores undeployados correctamente");
        }
        // Continuar incluso si hay errores de undeploy

        // Resetear estado en StateManager
        eventBus.request("state.reset", new JsonObject(), reply -> {
          if (reply.succeeded()) {
            System.out.println("Estado del sistema reseteado");
          }
          promise.complete();
        });
      });
    }

    return promise.future();
  }

  /**
   * Despliega el StateManager
   */
  private Future<String> deployStateManager() {
    Promise<String> promise = Promise.promise();

    vertx.deployVerticle(StateManagerVerticle.class.getName(), res -> {
      if (res.succeeded()) {
        String deploymentId = res.result();
        deploymentIds.add(deploymentId);
        System.out.println("StateManagerVerticle desplegado correctamente: " + deploymentId);
        promise.complete(deploymentId);
      } else {
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }

  /**
   * Despliega los workers de manera distribuida
   */
  private Future<List<String>> deployWorkers(int N, int numWorkers) {
    Promise<List<String>> promise = Promise.promise();
    List<Future<String>> deploymentFutures = new ArrayList<>();

    // Calcular distribución de filas entre workers
    int rowsPerWorker = N / numWorkers;
    int remainingRows = N % numWorkers;

    for (int i = 0; i < numWorkers; i++) {
      final int workerId = i;
      int startRow = i * rowsPerWorker + Math.min(i, remainingRows);
      int endRow = startRow + rowsPerWorker - 1 + (i < remainingRows ? 1 : 0);

      JsonObject workerConfig = new JsonObject()
          .put("workerId", workerId)
          .put("N", N)
          .put("startRow", startRow)
          .put("endRow", endRow);

      Promise<String> deployPromise = Promise.promise();
      vertx.deployVerticle(WorkerVerticle.class.getName(),
          new DeploymentOptions().setConfig(workerConfig), res -> {
            if (res.succeeded()) {
              String deploymentId = res.result();
              deploymentIds.add(deploymentId);
              workersDeployed++;
              System.out.println("WorkerVerticle #" + workerId + " desplegado para filas " +
                               startRow + "-" + endRow + " (" + deploymentId + ")");
              deployPromise.complete(deploymentId);
            } else {
              System.out.println("Fallo al desplegar WorkerVerticle #" + workerId + ": " + res.cause().getMessage());
              deployPromise.fail(res.cause());
            }
          });

      deploymentFutures.add(deployPromise.future());
    }

    CompositeFuture.all(new ArrayList<>(deploymentFutures)).onComplete(ar -> {
      if (ar.succeeded()) {
        List<String> deploymentIds = new ArrayList<>();
        for (int i = 0; i < ar.result().size(); i++) {
          deploymentIds.add(ar.result().resultAt(i));
        }
        promise.complete(deploymentIds);
      } else {
        promise.fail(ar.cause());
      }
    });

    return promise.future();
  }

  /**
   * Monitorea el progreso de la solución
   */
  private void monitorProgress(int N, long startTime, io.vertx.core.eventbus.Message<Object> originalMessage) {
    AtomicInteger checkCount = new AtomicInteger(0);

    // Verificar progreso cada segundo
    long timerId = vertx.setPeriodic(1000, id -> {
      int checks = checkCount.incrementAndGet();

      eventBus.request("state.summary", new JsonObject(), reply -> {
        if (reply.succeeded()) {
          JsonObject summary = (JsonObject) reply.result().body();
          int totalSolutions = summary.getInteger("totalSolutions", 0);
          JsonArray solutions = summary.getJsonArray("solutions", new JsonArray());

          long elapsedTime = System.currentTimeMillis() - startTime;
          System.out.println("Progreso [" + elapsedTime + "ms]: " + totalSolutions + " soluciones encontradas");

          // Determinar si hemos terminado
          boolean shouldFinish = false;
          String finishReason = "";

          if (totalSolutions > 0) {
            // Hemos encontrado al menos una solución, podemos terminar
            shouldFinish = true;
            finishReason = "soluciones encontradas";
          } else if (checks >= 30) {
            // Timeout después de 30 segundos
            shouldFinish = true;
            finishReason = "timeout alcanzado";
          } else if (elapsedTime > 60000) {
            // Timeout después de 1 minuto
            shouldFinish = true;
            finishReason = "tiempo límite excedido";
          }

          if (shouldFinish) {
            vertx.cancelTimer(id);

            JsonObject response = new JsonObject()
                .put("N", N)
                .put("totalSolutions", totalSolutions)
                .put("solutions", solutions)
                .put("elapsedTimeMs", elapsedTime)
                .put("workersDeployed", workersDeployed)
                .put("status", "completed")
                .put("reason", finishReason);

            System.out.println("Orquestación completada: " + finishReason + " en " + elapsedTime + "ms");
            originalMessage.reply(response);
          }
        } else {
          System.out.println("Error obteniendo resumen: " + reply.cause().getMessage());
        }
      });
    });

    // Timeout de seguridad
    vertx.setTimer(65000, id -> {
      vertx.cancelTimer(timerId);
      long elapsedTime = System.currentTimeMillis() - startTime;

      JsonObject response = new JsonObject()
          .put("N", N)
          .put("status", "timeout")
          .put("elapsedTimeMs", elapsedTime)
          .put("workersDeployed", workersDeployed)
          .put("message", "Operación cancelada por timeout de seguridad");

      originalMessage.reply(response);
    });
  }
}
