package cl.marcuzo.nreinas.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StateManagerVerticle maneja el estado distribuido para la programación dinámica
 * de N-Reinas. Actúa como un cache distribuido de estados válidos.
 */
public class StateManagerVerticle extends AbstractVerticle {

    // Cache distribuido de estados válidos por fila
    private final Map<String, Set<List<Integer>>> stateCache = new ConcurrentHashMap<>();

    // Contador de soluciones encontradas
    private int solutionCount = 0;

    // Lista de todas las soluciones completas
    private final List<List<Integer>> completeSolutions = new ArrayList<>();

    @Override
    public void start() {
        EventBus eventBus = vertx.eventBus();

        // Maneja solicitudes para obtener estados válidos de una fila
        eventBus.consumer("state.get", message -> {
            JsonObject request = (JsonObject) message.body();
            int row = request.getInteger("row");
            String key = "row_" + row;

            Set<List<Integer>> states = stateCache.getOrDefault(key, new HashSet<>());
            JsonArray statesJson = new JsonArray();

            for (List<Integer> state : states) {
                JsonArray stateJson = new JsonArray();
                state.forEach(stateJson::add);
                statesJson.add(stateJson);
            }

            message.reply(new JsonObject().put("states", statesJson));
        });

        // Maneja actualizaciones de estados válidos
        eventBus.consumer("state.update", message -> {
            JsonObject request = (JsonObject) message.body();
            int row = request.getInteger("row");
            JsonArray newStates = request.getJsonArray("states");

            String key = "row_" + row;
            Set<List<Integer>> states = stateCache.computeIfAbsent(key, k -> new HashSet<>());

            for (Object stateObj : newStates) {
                JsonArray stateJson = (JsonArray) stateObj;
                List<Integer> state = new ArrayList<>();
                stateJson.forEach(col -> state.add((Integer) col));
                states.add(state);
            }

            System.out.println("StateManager: Actualizados " + newStates.size() +
                             " estados para fila " + row + ". Total estados: " + states.size());

            message.reply(new JsonObject().put("status", "updated"));
        });

        // Maneja soluciones completas
        eventBus.consumer("solution.found", message -> {
            JsonObject request = (JsonObject) message.body();
            JsonArray solutionJson = request.getJsonArray("solution");

            List<Integer> solution = new ArrayList<>();
            solutionJson.forEach(col -> solution.add((Integer) col));

            synchronized (completeSolutions) {
                completeSolutions.add(solution);
                solutionCount++;
            }

            System.out.println("StateManager: Solución #" + solutionCount + " encontrada: " + solution);

            // Broadcast a todos los workers que se encontró una nueva solución
            eventBus.publish("solution.broadcast", new JsonObject()
                .put("solutionNumber", solutionCount)
                .put("solution", solutionJson));
        });

        // Maneja solicitudes de resumen de resultados
        eventBus.consumer("state.summary", message -> {
            JsonArray solutionsJson = new JsonArray();
            synchronized (completeSolutions) {
                for (List<Integer> solution : completeSolutions) {
                    JsonArray solutionJson = new JsonArray();
                    solution.forEach(solutionJson::add);
                    solutionsJson.add(solutionJson);
                }
            }

            JsonObject summary = new JsonObject()
                .put("totalSolutions", solutionCount)
                .put("solutions", solutionsJson)
                .put("cacheSize", stateCache.size());

            message.reply(summary);
        });

        // Maneja reset del estado
        eventBus.consumer("state.reset", message -> {
            stateCache.clear();
            synchronized (completeSolutions) {
                completeSolutions.clear();
                solutionCount = 0;
            }
            System.out.println("StateManager: Estado reiniciado");
            message.reply(new JsonObject().put("status", "reset"));
        });

        System.out.println("StateManagerVerticle iniciado correctamente");
    }


}
