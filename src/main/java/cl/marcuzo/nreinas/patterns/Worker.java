package cl.marcuzo.nreinas.patterns;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * Worker Interface - Patrón Worker + Command
 *
 * Define el contrato para workers distribuidos que procesan el problema de N-Reinas.
 * Implementa el patrón Command para encapsular operaciones como objetos.
 *
 * Responsabilidades:
 * - Procesar unidades de trabajo de manera asíncrona
 * - Reportar progreso y resultados
 * - Manejar errores de manera elegante
 *
 * Patrones aplicados:
 * - Worker Pattern: Unidades de procesamiento distribuido
 * - Command Pattern: Encapsula operaciones como objetos
 * - Observer Pattern: Notifica cambios de estado
 * - Strategy Pattern: Diferentes algoritmos de resolución
 */
public interface Worker {

    /**
     * Procesa una unidad de trabajo específica
     *
     * @param workUnit Unidad de trabajo en formato JsonObject
     * @return Future con el resultado del procesamiento
     */
    Future<JsonObject> processWork(JsonObject workUnit);

    /**
     * Inicia el worker para comenzar a procesar trabajo
     *
     * @param config Configuración inicial del worker
     * @return Future que se completa cuando el worker está listo
     */
    Future<Void> start(JsonObject config);

    /**
     * Detiene el worker de manera elegante
     *
     * @return Future que se completa cuando el worker se detiene
     */
    Future<Void> stop();

    /**
     * Obtiene el estado actual del worker
     *
     * @return Future con el estado actual en formato JsonObject
     */
    Future<JsonObject> getStatus();

    /**
     * Obtiene las métricas de rendimiento del worker
     *
     * @return Future con métricas en formato JsonObject
     */
    Future<JsonObject> getMetrics();

    /**
     * Obtiene el ID único del worker
     *
     * @return String con el ID del worker
     */
    String getWorkerId();

    /**
     * Verifica si el worker está procesando trabajo actualmente
     *
     * @return Future<Boolean> indicando si está ocupado
     */
    Future<Boolean> isBusy();

    /**
     * Establece un callback para notificar progreso
     *
     * @param progressCallback Callback que recibe updates de progreso
     */
    void setProgressCallback(ProgressCallback progressCallback);

    /**
     * Interface para callbacks de progreso
     */
    @FunctionalInterface
    interface ProgressCallback {
        void onProgress(String workerId, JsonObject progress);
    }
}
