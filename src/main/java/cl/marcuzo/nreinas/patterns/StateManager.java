package cl.marcuzo.nreinas.patterns;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * State Manager Interface - Patrón State + Repository
 *
 * Define el contrato para gestión de estado distribuido en el problema de N-Reinas.
 * Implementa el principio de Inversión de Dependencias (SOLID).
 *
 * Responsabilidades:
 * - Gestionar estados válidos de programación dinámica
 * - Almacenar y recuperar soluciones encontradas
 * - Mantener métricas de estado del sistema
 *
 * Patrones aplicados:
 * - Repository Pattern: Abstrae el almacenamiento de datos
 * - State Pattern: Gestiona estados de la aplicación
 * - Future Pattern: Operaciones asíncronas no-bloqueantes
 */
public interface StateManager {

    /**
     * Actualiza los estados válidos para una fila específica
     *
     * @param row Número de fila (0-based)
     * @param states Estados válidos en formato JsonArray
     * @return Future que se completa cuando la actualización termina
     */
    Future<Void> updateStates(int row, JsonArray states);

    /**
     * Obtiene los estados válidos para una fila específica
     *
     * @param row Número de fila (0-based)
     * @return Future con los estados en formato JsonArray
     */
    Future<JsonArray> getStates(int row);

    /**
     * Almacena una solución completa encontrada
     *
     * @param solution Solución completa en formato JsonArray
     * @return Future que se completa cuando se almacena la solución
     */
    Future<Void> storeSolution(JsonArray solution);

    /**
     * Obtiene un resumen del estado actual del sistema
     *
     * @return Future con resumen incluyendo total de soluciones, cache size, etc.
     */
    Future<JsonObject> getStateSummary();

    /**
     * Reinicia todo el estado del sistema
     *
     * @return Future que se completa cuando se reinicia el estado
     */
    Future<Void> resetState();

    /**
     * Obtiene todas las soluciones encontradas
     *
     * @return Future con array de todas las soluciones
     */
    Future<JsonArray> getAllSolutions();

    /**
     * Verifica si el sistema está listo para procesar
     *
     * @return Future<Boolean> indicando si está listo
     */
    Future<Boolean> isReady();
}
