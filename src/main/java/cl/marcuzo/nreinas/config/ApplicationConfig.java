package cl.marcuzo.nreinas.config;

/**
 * Application Configuration - Patrón Configuration + Constants
 *
 * Centraliza toda la configuración de la aplicación siguiendo el principio
 * Single Responsibility. Facilita el testing y la gestión de configuraciones
 * por ambiente.
 *
 * Patrones aplicados:
 * - Configuration Pattern: Centraliza configuración
 * - Constants Pattern: Define valores inmutables
 * - Factory Pattern: Para crear configuraciones específicas
 */
public final class ApplicationConfig {

    // Prevenir instanciación
    private ApplicationConfig() {}

    // ==================== CONFIGURACIÓN DE RED ====================

    /** Puerto HTTP por defecto */
    public static final int DEFAULT_HTTP_PORT = 8080;

    /** Host por defecto */
    public static final String DEFAULT_HOST = "0.0.0.0";

    /** Timeout de conexión en milisegundos */
    public static final long CONNECTION_TIMEOUT_MS = 30_000L;

    // ==================== CONFIGURACIÓN DE N-REINAS ====================

    /** Tamaño mínimo del tablero */
    public static final int MIN_BOARD_SIZE = 1;

    /** Tamaño máximo del tablero */
    public static final int MAX_BOARD_SIZE = 20;

    /** Tamaño por defecto del tablero */
    public static final int DEFAULT_BOARD_SIZE = 8;

    /** Número mínimo de workers */
    public static final int MIN_WORKERS = 1;

    /** Número máximo de workers */
    public static final int MAX_WORKERS = 10;

    /** Número por defecto de workers */
    public static final int DEFAULT_WORKERS = 4;

    // ==================== CONFIGURACIÓN DE TIMEOUTS ====================

    /** Timeout de ejecución por defecto en milisegundos */
    public static final long DEFAULT_EXECUTION_TIMEOUT_MS = 60_000L;

    /** Timeout de worker por defecto en milisegundos */
    public static final long DEFAULT_WORKER_TIMEOUT_MS = 30_000L;

    /** Timeout de monitoreo en milisegundos */
    public static final long MONITORING_TIMEOUT_MS = 65_000L;

    /** Intervalo de verificación de progreso en milisegundos */
    public static final long PROGRESS_CHECK_INTERVAL_MS = 1_000L;

    // ==================== CONFIGURACIÓN DE JVM ====================

    /** Memoria heap mínima recomendada en MB */
    public static final int MIN_HEAP_SIZE_MB = 256;

    /** Memoria heap máxima recomendada en MB */
    public static final int MAX_HEAP_SIZE_MB = 2048;

    /** Umbral de memoria para alertas (porcentaje) */
    public static final double MEMORY_ALERT_THRESHOLD = 0.80;

    // ==================== CONFIGURACIÓN DE MÉTRICAS ====================

    /** Intervalo de scraping de Prometheus en segundos */
    public static final int PROMETHEUS_SCRAPE_INTERVAL_SECONDS = 5;

    /** Puerto de métricas */
    public static final int METRICS_PORT = 8080;

    /** Path de métricas */
    public static final String METRICS_PATH = "/metrics";

    /** Prefijo para métricas de la aplicación */
    public static final String METRICS_PREFIX = "nreinas";

    // ==================== CONFIGURACIÓN DE EVENT BUS ====================

    /** Address para solicitudes del orquestador */
    public static final String ORCHESTRATOR_ADDRESS = "orquestador.nreinas";

    /** Address para actualización de estado */
    public static final String STATE_UPDATE_ADDRESS = "state.update";

    /** Address para obtener estado */
    public static final String STATE_GET_ADDRESS = "state.get";

    /** Address para soluciones encontradas */
    public static final String SOLUTION_FOUND_ADDRESS = "solution.found";

    /** Address para broadcast de soluciones */
    public static final String SOLUTION_BROADCAST_ADDRESS = "solution.broadcast";

    /** Address para resumen de estado */
    public static final String STATE_SUMMARY_ADDRESS = "state.summary";

    /** Address para reset de estado */
    public static final String STATE_RESET_ADDRESS = "state.reset";

    /** Prefijo para addresses de workers */
    public static final String WORKER_ADDRESS_PREFIX = "worker";

    // ==================== CONFIGURACIÓN DE CLUSTERING ====================

    /** Host de clustering por defecto */
    public static final String DEFAULT_CLUSTER_HOST = "localhost";

    /** Puerto de clustering (0 = automático) */
    public static final int DEFAULT_CLUSTER_PORT = 0;

    /** Nombre del cluster */
    public static final String CLUSTER_NAME = "nreinas-cluster";

    // ==================== CONFIGURACIÓN DE LOGGING ====================

    /** Nivel de log por defecto */
    public static final String DEFAULT_LOG_LEVEL = "INFO";

    /** Formato de timestamp para logs */
    public static final String LOG_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    // ==================== CONFIGURACIÓN DE HEALTH CHECKS ====================

    /** Intervalo de health check en segundos */
    public static final int HEALTH_CHECK_INTERVAL_SECONDS = 30;

    /** Timeout de health check en segundos */
    public static final int HEALTH_CHECK_TIMEOUT_SECONDS = 3;

    /** Reintentos de health check */
    public static final int HEALTH_CHECK_RETRIES = 3;

    /** Periodo de gracia para startup en segundos */
    public static final int HEALTH_CHECK_START_PERIOD_SECONDS = 5;

    // ==================== CONFIGURACIÓN DE CIRCUIT BREAKER ====================

    /** Número máximo de fallos antes de abrir circuit breaker */
    public static final int CIRCUIT_BREAKER_MAX_FAILURES = 5;

    /** Timeout de circuit breaker en milisegundos */
    public static final long CIRCUIT_BREAKER_TIMEOUT_MS = 30_000L;

    /** Timeout de reset de circuit breaker en milisegundos */
    public static final long CIRCUIT_BREAKER_RESET_TIMEOUT_MS = 60_000L;

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Valida si el tamaño del tablero está en rango válido
     *
     * @param boardSize Tamaño del tablero
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidBoardSize(int boardSize) {
        return boardSize >= MIN_BOARD_SIZE && boardSize <= MAX_BOARD_SIZE;
    }

    /**
     * Valida si el número de workers está en rango válido
     *
     * @param numWorkers Número de workers
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidWorkerCount(int numWorkers) {
        return numWorkers >= MIN_WORKERS && numWorkers <= MAX_WORKERS;
    }

    /**
     * Calcula el timeout recomendado basado en el tamaño del problema
     *
     * @param boardSize Tamaño del tablero
     * @return Timeout recomendado en milisegundos
     */
    public static long calculateRecommendedTimeout(int boardSize) {
        // Timeout base + factor exponencial para problemas más grandes
        long baseTimeout = DEFAULT_EXECUTION_TIMEOUT_MS;
        if (boardSize <= 8) {
            return baseTimeout;
        } else if (boardSize <= 12) {
            return baseTimeout * 2;
        } else {
            return baseTimeout * 4;
        }
    }

    /**
     * Obtiene el address del Event Bus para un worker específico
     *
     * @param workerId ID del worker
     * @return Address del Event Bus
     */
    public static String getWorkerAddress(int workerId) {
        return WORKER_ADDRESS_PREFIX + "." + workerId + ".process";
    }

    /**
     * Obtiene configuración de memoria JVM recomendada
     *
     * @param boardSize Tamaño del tablero
     * @param numWorkers Número de workers
     * @return Memoria recomendada en MB
     */
    public static int getRecommendedHeapSize(int boardSize, int numWorkers) {
        // Cálculo heurístico basado en complejidad del problema
        int baseMemory = MIN_HEAP_SIZE_MB;
        int complexityFactor = boardSize * numWorkers;

        if (complexityFactor > 100) {
            return Math.min(MAX_HEAP_SIZE_MB, baseMemory * 4);
        } else if (complexityFactor > 50) {
            return Math.min(MAX_HEAP_SIZE_MB, baseMemory * 2);
        } else {
            return baseMemory;
        }
    }
}
