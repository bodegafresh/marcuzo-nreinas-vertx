package cl.marcuzo.nreinas.patterns;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * Orchestrator Interface - Patrón Orchestrator + Mediator
 *
 * Define el contrato para el orquestador que coordina la resolución distribuida
 * del problema de N-Reinas. Implementa el patrón Mediator para controlar
 * la comunicación entre componentes.
 *
 * Responsabilidades:
 * - Coordinar el flujo de trabajo entre múltiples workers
 * - Gestionar el ciclo de vida de workers
 * - Agregar y consolidar resultados
 * - Manejar fallos y recuperación
 *
 * Patrones aplicados:
 * - Orchestrator Pattern: Coordinación centralizada de servicios
 * - Mediator Pattern: Controla comunicación entre objetos
 * - Facade Pattern: Simplifica interfaz compleja
 * - Template Method: Define esqueleto de algoritmos
 */
public interface Orchestrator {

    /**
     * Procesa una solicitud de resolución de N-Reinas
     *
     * @param request Solicitud con parámetros N, workers, etc.
     * @return Future con el resultado completo de la resolución
     */
    Future<JsonObject> processNQueensRequest(JsonObject request);

    /**
     * Despliega workers necesarios para resolver el problema
     *
     * @param N Tamaño del tablero
     * @param numWorkers Número de workers a desplegar
     * @return Future con información de workers desplegados
     */
    Future<JsonObject> deployWorkers(int N, int numWorkers);

    /**
     * Monitorea el progreso de la ejecución distribuida
     *
     * @param N Tamaño del problema
     * @param startTime Tiempo de inicio para cálculo de duración
     * @return Future con estado del progreso
     */
    Future<JsonObject> monitorProgress(int N, long startTime);

    /**
     * Consolida resultados de múltiples workers
     *
     * @return Future con resultados consolidados
     */
    Future<JsonObject> consolidateResults();

    /**
     * Limpia recursos utilizados en la ejecución
     *
     * @return Future que se completa cuando la limpieza termina
     */
    Future<Void> cleanup();

    /**
     * Obtiene el estado actual del orquestador
     *
     * @return Future con estado actual en formato JsonObject
     */
    Future<JsonObject> getStatus();

    /**
     * Cancela una ejecución en progreso
     *
     * @param reason Razón de la cancelación
     * @return Future que se completa cuando se cancela
     */
    Future<Void> cancelExecution(String reason);

    /**
     * Configura timeouts para la ejecución
     *
     * @param executionTimeoutMs Timeout de ejecución en milisegundos
     * @param workerTimeoutMs Timeout de workers en milisegundos
     */
    void setTimeouts(long executionTimeoutMs, long workerTimeoutMs);

    /**
     * Establece callback para notificaciones de eventos
     *
     * @param eventCallback Callback para eventos del orquestador
     */
    void setEventCallback(OrchestratorEventCallback eventCallback);

    /**
     * Interface para callbacks de eventos del orquestador
     */
    @FunctionalInterface
    interface OrchestratorEventCallback {
        void onEvent(String eventType, JsonObject eventData);
    }
}
