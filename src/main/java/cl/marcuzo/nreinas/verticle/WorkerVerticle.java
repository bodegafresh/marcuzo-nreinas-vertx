package cl.marcuzo.nreinas.verticle;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.backends.BackendRegistries;
import java.util.*;

/**
 * WorkerVerticle implementa la búsqueda distribuida de N-Reinas usando programación dinámica.
 * Cada worker se especializa en procesar un rango específico de filas o estados.
 */
public class WorkerVerticle extends AbstractVerticle {

  private Counter executionCounter;
  private Counter errorCounter;
  private Timer executionTimer;
  private Counter statesProcessedCounter;
  private Counter solutionsFoundCounter;

  private int workerId;
  private int N;
  private int startRow;
  private int endRow;
  private EventBus eventBus;

  @Override
  public void start() {
    PrometheusMeterRegistry prometheusRegistry =
        (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();

    // Definir métricas personalizadas
    executionCounter =
        Counter.builder("worker.executions").description("Número de ejecuciones del WorkerVerticle")
            .tag("worker", "nreinas").register(prometheusRegistry);

    errorCounter =
        Counter.builder("worker.errors").description("Número de errores en el WorkerVerticle")
            .tag("worker", "nreinas").register(prometheusRegistry);

    executionTimer =
        Timer.builder("worker.execution.time").description("Tiempo de ejecución del WorkerVerticle")
            .tag("worker", "nreinas").register(prometheusRegistry);

    statesProcessedCounter =
        Counter.builder("worker.states.processed").description("Estados procesados por el WorkerVerticle")
            .tag("worker", "nreinas").register(prometheusRegistry);

    solutionsFoundCounter =
        Counter.builder("worker.solutions.found").description("Soluciones encontradas por el WorkerVerticle")
            .tag("worker", "nreinas").register(prometheusRegistry);

    // Configuración del worker
    workerId = config().getInteger("workerId", 0);
    N = config().getInteger("N", 8);
    startRow = config().getInteger("startRow", 0);
    endRow = config().getInteger("endRow", N - 1);

    eventBus = vertx.eventBus();

    System.out.println("WorkerVerticle #" + workerId + " iniciado para filas " + startRow + "-" + endRow + " (N=" + N + ")");

    // Registrar consumer para trabajos específicos de este worker
    eventBus.consumer("worker." + workerId + ".process", this::processWorkUnit);

    // Escuchar broadcasts de soluciones encontradas
    eventBus.consumer("solution.broadcast", this::onSolutionFound);

    // Iniciar el procesamiento
    startProcessing();
  }

  private void startProcessing() {
    executionTimer.record(() -> {
      try {
        // Simplificar: solo el worker 0 procesará todo el tablero
        if (workerId == 0) {
          System.out.println("Worker #0 iniciando búsqueda completa de N-Reinas");
          solveNQueensComplete();
        } else {
          System.out.println("Worker #" + workerId + " en espera para trabajo específico");
          // Otros workers pueden procesar trabajos específicos enviados por el orquestador
        }
        executionCounter.increment();
      } catch (Exception e) {
        System.out.println("Error en WorkerVerticle #" + workerId + ": " + e.getMessage());
        errorCounter.increment();
      }
    });
  }

  /**
   * Resuelve N-Reinas de forma completa usando backtracking con DP
   */
  private void solveNQueensComplete() {
    System.out.println("Iniciando búsqueda completa de soluciones para N=" + N);

    List<List<Integer>> allSolutions = new ArrayList<>();
    List<Integer> currentSolution = new ArrayList<>();

    // Usar backtracking para encontrar todas las soluciones
    findAllSolutions(0, currentSolution, allSolutions);

    System.out.println("Búsqueda completada. Encontradas " + allSolutions.size() + " soluciones");

    // Enviar todas las soluciones encontradas al StateManager
    for (List<Integer> solution : allSolutions) {
      JsonArray solutionJson = new JsonArray();
      solution.forEach(solutionJson::add);

      eventBus.send("solution.found", new JsonObject().put("solution", solutionJson));
      solutionsFoundCounter.increment();
    }
  }

  /**
   * Algoritmo recursivo de backtracking para encontrar todas las soluciones
   */
  private void findAllSolutions(int row, List<Integer> currentSolution, List<List<Integer>> allSolutions) {
    if (row == N) {
      // Hemos colocado todas las reinas, esta es una solución válida
      allSolutions.add(new ArrayList<>(currentSolution));
      statesProcessedCounter.increment();
      return;
    }

    // Intentar colocar una reina en cada columna de la fila actual
    for (int col = 0; col < N; col++) {
      if (isValidPosition(currentSolution, col)) {
        // Colocar la reina
        currentSolution.add(col);

        // Recursivamente buscar soluciones para la siguiente fila
        findAllSolutions(row + 1, currentSolution, allSolutions);

        // Backtrack: remover la reina para probar la siguiente posición
        currentSolution.remove(currentSolution.size() - 1);
      }
    }
  }

  /**
   * Genera estados iniciales para la primera fila
   */
  private void generateInitialStates() {
    List<List<Integer>> initialStates = new ArrayList<>();

    // Para la primera fila, cada columna es un estado válido inicial
    for (int col = 0; col < N; col++) {
      List<Integer> state = new ArrayList<>();
      state.add(col);
      initialStates.add(state);
    }

    // Enviar estados iniciales al StateManager
    JsonArray statesJson = new JsonArray();
    for (List<Integer> state : initialStates) {
      JsonArray stateJson = new JsonArray();
      state.forEach(stateJson::add);
      statesJson.add(stateJson);
    }

    eventBus.request("state.update", new JsonObject()
        .put("row", 0)
        .put("states", statesJson), reply -> {
      if (reply.succeeded()) {
        System.out.println("Worker #" + workerId + ": Estados iniciales enviados");

        // Procesar siguiente fila si es responsabilidad de este worker
        if (endRow > 0) {
          processNextRow(0, initialStates);
        }
      }
    });
  }

  /**
   * Procesa estados para filas intermedias
   */
  private void processIntermediateStates() {
    // Solicitar estados de la fila anterior
    eventBus.request("state.get", new JsonObject().put("row", startRow - 1), reply -> {
      if (reply.succeeded()) {
        JsonObject response = (JsonObject) reply.result().body();
        JsonArray statesJson = response.getJsonArray("states");

        List<List<Integer>> previousStates = new ArrayList<>();
        for (Object stateObj : statesJson) {
          JsonArray stateJson = (JsonArray) stateObj;
          List<Integer> state = new ArrayList<>();
          stateJson.forEach(col -> state.add((Integer) col));
          previousStates.add(state);
        }

        if (!previousStates.isEmpty()) {
          processNextRow(startRow, previousStates);
        }
      }
    });
  }

  /**
   * Procesa la siguiente fila basándose en los estados previos
   */
  private void processNextRow(int currentRow, List<List<Integer>> previousStates) {
    int nextRow = currentRow + 1;

    if (nextRow >= N) {
      // Hemos llegado al final, los estados previos son soluciones completas
      for (List<Integer> solution : previousStates) {
        JsonArray solutionJson = new JsonArray();
        solution.forEach(solutionJson::add);

        eventBus.send("solution.found", new JsonObject().put("solution", solutionJson));
        solutionsFoundCounter.increment();
      }
      return;
    }

    List<List<Integer>> nextStates = new ArrayList<>();

    // Para cada estado previo, intentar agregar reinas en la siguiente fila
    for (List<Integer> prevState : previousStates) {
      for (int col = 0; col < N; col++) {
        if (isValidPosition(prevState, col)) {
          List<Integer> newState = new ArrayList<>(prevState);
          newState.add(col);
          nextStates.add(newState);
        }
      }
      statesProcessedCounter.increment();
    }

    System.out.println("Worker #" + workerId + ": Fila " + nextRow + " - " +
                      previousStates.size() + " estados previos → " + nextStates.size() + " estados válidos");

    if (!nextStates.isEmpty()) {
      // Enviar nuevos estados al StateManager
      JsonArray statesJson = new JsonArray();
      for (List<Integer> state : nextStates) {
        JsonArray stateJson = new JsonArray();
        state.forEach(stateJson::add);
        statesJson.add(stateJson);
      }

      eventBus.request("state.update", new JsonObject()
          .put("row", nextRow)
          .put("states", statesJson), updateReply -> {
        if (updateReply.succeeded()) {
          // Si este worker debe procesar la siguiente fila
          if (nextRow <= endRow) {
            // Continuar con la siguiente fila después de un pequeño delay
            vertx.setTimer(50, id -> processNextRow(nextRow, nextStates));
          }
        }
      });
    }
  }

  /**
   * Verifica si una posición es válida dado el estado actual
   */
  private boolean isValidPosition(List<Integer> currentState, int newCol) {
    int newRow = currentState.size();

    for (int row = 0; row < currentState.size(); row++) {
      int col = currentState.get(row);

      // Verificar columna
      if (col == newCol) return false;

      // Verificar diagonales
      if (Math.abs(row - newRow) == Math.abs(col - newCol)) return false;
    }

    return true;
  }

  /**
   * Maneja unidades de trabajo específicas enviadas a este worker
   */
  private void processWorkUnit(io.vertx.core.eventbus.Message<Object> message) {
    JsonObject workUnit = (JsonObject) message.body();
    int row = workUnit.getInteger("row");
    JsonArray statesJson = workUnit.getJsonArray("states");

    List<List<Integer>> states = new ArrayList<>();
    for (Object stateObj : statesJson) {
      JsonArray stateJson = (JsonArray) stateObj;
      List<Integer> state = new ArrayList<>();
      stateJson.forEach(col -> state.add((Integer) col));
      states.add(state);
    }

    processNextRow(row, states);
    message.reply(new JsonObject().put("status", "processing"));
  }

  /**
   * Maneja notificaciones de soluciones encontradas
   */
  private void onSolutionFound(io.vertx.core.eventbus.Message<Object> message) {
    JsonObject notification = (JsonObject) message.body();
    int solutionNumber = notification.getInteger("solutionNumber");
    System.out.println("Worker #" + workerId + ": Notificación - Solución #" + solutionNumber + " encontrada");
  }
}
