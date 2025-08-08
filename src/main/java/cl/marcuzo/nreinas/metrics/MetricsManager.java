package cl.marcuzo.nreinas.metrics;

import cl.marcuzo.nreinas.config.ApplicationConfig;
import io.micrometer.core.instrument.*;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics Manager - Patrón Singleton + Factory + Observer
 *
 * Gestiona todas las métricas de la aplicación de manera centralizada.
 * Implementa mejores prácticas de observabilidad y monitoreo.
 *
 * Métricas implementadas siguiendo "The Four Golden Signals":
 * 1. Latency - Tiempo de respuesta
 * 2. Traffic - Volumen de requests
 * 3. Errors - Tasa de errores
 * 4. Saturation - Utilización de recursos
 *
 * Patrones aplicados:
 * - Singleton Pattern: Una sola instancia de métricas
 * - Factory Pattern: Creación de métricas específicas
 * - Observer Pattern: Notificación de cambios
 * - Builder Pattern: Construcción fluida de métricas
 */
public class MetricsManager {

    private static volatile MetricsManager instance;
    private final PrometheusMeterRegistry prometheusRegistry;

    // ==================== MÉTRICAS DE NEGOCIO ====================

    private final Counter nqueensRequestsTotal;
    private final Counter nqueensSolutionsFound;
    private final Gauge nqueensActiveSessions;
    private final Timer nqueensExecutionTime;

    // ==================== MÉTRICAS HTTP ====================

    private final Counter httpRequestsTotal;
    private final Counter httpRequestsErrors;
    private final Timer httpRequestsDuration;
    private final Gauge httpActiveConnections;

    // ==================== MÉTRICAS DE WORKERS ====================

    private final Counter workerExecutions;
    private final Counter workerErrors;
    private final Counter workerStatesProcessed;
    private final Timer workerExecutionTime;
    private final Gauge workersActive;

    // ==================== MÉTRICAS DE SISTEMA ====================

    private final Gauge systemCpuUsage;
    private final Gauge systemMemoryUsage;
    private final Gauge jvmMemoryHeapUsed;
    private final Gauge jvmMemoryHeapMax;

    // ==================== MÉTRICAS PERSONALIZADAS ====================

    private final AtomicInteger activeSessionsCount = new AtomicInteger(0);
    private final AtomicInteger activeWorkersCount = new AtomicInteger(0);
    private final AtomicLong totalSolutionsFound = new AtomicLong(0);

    /**
     * Constructor privado para Singleton
     */
    private MetricsManager() {
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // Inicializar métricas de negocio
        this.nqueensRequestsTotal = createBusinessCounter(
            "nqueens_requests_total",
            "Total de solicitudes de N-Reinas procesadas"
        );

        this.nqueensSolutionsFound = createBusinessCounter(
            "nqueens_solutions_found_total",
            "Total de soluciones de N-Reinas encontradas"
        );

        this.nqueensActiveSessions = createBusinessGauge(
            "nqueens_active_sessions",
            "Número de sesiones activas de N-Reinas",
            activeSessionsCount
        );

        this.nqueensExecutionTime = createBusinessTimer(
            "nqueens_execution_duration_seconds",
            "Tiempo total de ejecución de N-Reinas"
        );

        // Inicializar métricas HTTP
        this.httpRequestsTotal = createHttpCounter(
            "http_requests_total",
            "Total de peticiones HTTP"
        );

        this.httpRequestsErrors = createHttpCounter(
            "http_requests_errors_total",
            "Total de errores en peticiones HTTP"
        );

        this.httpRequestsDuration = createHttpTimer(
            "http_requests_duration_seconds",
            "Duración de peticiones HTTP"
        );

        this.httpActiveConnections = Gauge.builder("http_active_connections", this, MetricsManager::getActiveConnections)
            .description("Conexiones HTTP activas")
            .tag("component", "http")
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry);

        // Inicializar métricas de workers
        this.workerExecutions = createWorkerCounter(
            "worker_executions_total",
            "Total de ejecuciones de workers"
        );

        this.workerErrors = createWorkerCounter(
            "worker_errors_total",
            "Total de errores en workers"
        );

        this.workerStatesProcessed = createWorkerCounter(
            "worker_states_processed_total",
            "Total de estados procesados por workers"
        );

        this.workerExecutionTime = createWorkerTimer(
            "worker_execution_duration_seconds",
            "Tiempo de ejecución de workers"
        );

        this.workersActive = createWorkerGauge(
            "workers_active",
            "Número de workers activos",
            activeWorkersCount
        );

        // Inicializar métricas de sistema
        this.systemCpuUsage = Gauge.builder("system_cpu_usage_percent", this, MetricsManager::getSystemCpuUsage)
            .description("Uso de CPU del sistema")
            .tag("component", "system")
            .register(prometheusRegistry);

        this.systemMemoryUsage = Gauge.builder("system_memory_usage_percent", this, MetricsManager::getSystemMemoryUsage)
            .description("Uso de memoria del sistema")
            .tag("component", "system")
            .register(prometheusRegistry);

        this.jvmMemoryHeapUsed = Gauge.builder("jvm_memory_heap_used_bytes", this, MetricsManager::getJvmHeapUsed)
            .description("Memoria heap JVM utilizada")
            .tag("component", "jvm")
            .register(prometheusRegistry);

        this.jvmMemoryHeapMax = Gauge.builder("jvm_memory_heap_max_bytes", this, MetricsManager::getJvmHeapMax)
            .description("Memoria heap JVM máxima")
            .tag("component", "jvm")
            .register(prometheusRegistry);

        // Registrar métricas JVM adicionales
        registerJvmMetrics();
    }

    /**
     * Obtiene la instancia singleton del MetricsManager
     */
    public static MetricsManager getInstance() {
        if (instance == null) {
            synchronized (MetricsManager.class) {
                if (instance == null) {
                    instance = new MetricsManager();
                }
            }
        }
        return instance;
    }

    /**
     * Obtiene el registry de Prometheus para exportación
     */
    public PrometheusMeterRegistry getPrometheusRegistry() {
        return prometheusRegistry;
    }

    /**
     * Obtiene las métricas en formato Prometheus
     */
    public String getMetricsAsPrometheusFormat() {
        return prometheusRegistry.scrape();
    }

    // ==================== MÉTODOS DE INCREMENTO ====================

    /**
     * Incrementa el contador de requests de N-Reinas
     */
    public void incrementNQueensRequests() {
        nqueensRequestsTotal.increment();
    }

    /**
     * Incrementa el contador de requests de N-Reinas con tags
     */
    public void incrementNQueensRequests(String method, String status) {
        Counter.builder("nqueens_requests_total")
            .tag("method", method)
            .tag("status", status)
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry)
            .increment();
    }

    /**
     * Registra soluciones encontradas
     */
    public void recordSolutionsFound(int count) {
        nqueensSolutionsFound.increment(count);
        totalSolutionsFound.addAndGet(count);
    }

    /**
     * Incrementa requests HTTP
     */
    public void incrementHttpRequests() {
        httpRequestsTotal.increment();
    }

    /**
     * Incrementa errores HTTP
     */
    public void incrementHttpErrors() {
        httpRequestsErrors.increment();
    }

    /**
     * Incrementa ejecuciones de worker
     */
    public void incrementWorkerExecutions() {
        workerExecutions.increment();
    }

    /**
     * Incrementa errores de worker
     */
    public void incrementWorkerErrors() {
        workerErrors.increment();
    }

    /**
     * Incrementa estados procesados
     */
    public void incrementStatesProcessed(int count) {
        workerStatesProcessed.increment(count);
    }

    // ==================== MÉTODOS DE TIMING ====================

    /**
     * Crea un timer sample para medir duración de N-Reinas
     */
    public Timer.Sample startNQueensTimer() {
        return Timer.start(prometheusRegistry);
    }

    /**
     * Finaliza medición de timer de N-Reinas
     */
    public void stopNQueensTimer(Timer.Sample sample) {
        sample.stop(nqueensExecutionTime);
    }

    /**
     * Crea un timer sample para medir duración HTTP
     */
    public Timer.Sample startHttpTimer() {
        return Timer.start(prometheusRegistry);
    }

    /**
     * Finaliza medición de timer HTTP
     */
    public void stopHttpTimer(Timer.Sample sample) {
        sample.stop(httpRequestsDuration);
    }

    /**
     * Crea un timer sample para medir duración de worker
     */
    public Timer.Sample startWorkerTimer() {
        return Timer.start(prometheusRegistry);
    }

    /**
     * Finaliza medición de timer de worker
     */
    public void stopWorkerTimer(Timer.Sample sample) {
        sample.stop(workerExecutionTime);
    }

    // ==================== MÉTODOS DE GESTIÓN DE ESTADO ====================

    /**
     * Incrementa sesiones activas
     */
    public void incrementActiveSessions() {
        activeSessionsCount.incrementAndGet();
    }

    /**
     * Decrementa sesiones activas
     */
    public void decrementActiveSessions() {
        activeSessionsCount.decrementAndGet();
    }

    /**
     * Incrementa workers activos
     */
    public void incrementActiveWorkers() {
        activeWorkersCount.incrementAndGet();
    }

    /**
     * Decrementa workers activos
     */
    public void decrementActiveWorkers() {
        activeWorkersCount.decrementAndGet();
    }

    // ==================== MÉTODOS PRIVADOS DE UTILIDAD ====================

    private Counter createBusinessCounter(String name, String description) {
        return Counter.builder(name)
            .description(description)
            .tag("component", "business")
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry);
    }

    private Timer createBusinessTimer(String name, String description) {
        return Timer.builder(name)
            .description(description)
            .tag("component", "business")
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry);
    }

    private Gauge createBusinessGauge(String name, String description, AtomicInteger value) {
        return Gauge.builder(name, value, AtomicInteger::get)
            .description(description)
            .tag("component", "business")
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry);
    }

    private Counter createHttpCounter(String name, String description) {
        return Counter.builder(name)
            .description(description)
            .tag("component", "http")
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry);
    }

    private Timer createHttpTimer(String name, String description) {
        return Timer.builder(name)
            .description(description)
            .tag("component", "http")
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry);
    }

    private Counter createWorkerCounter(String name, String description) {
        return Counter.builder(name)
            .description(description)
            .tag("component", "worker")
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry);
    }

    private Timer createWorkerTimer(String name, String description) {
        return Timer.builder(name)
            .description(description)
            .tag("component", "worker")
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry);
    }

    private Gauge createWorkerGauge(String name, String description, AtomicInteger value) {
        return Gauge.builder(name, value, AtomicInteger::get)
            .description(description)
            .tag("component", "worker")
            .tag("application", ApplicationConfig.METRICS_PREFIX)
            .register(prometheusRegistry);
    }

    private void registerJvmMetrics() {
        // Registrar métricas JVM estándar
        io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics jvmMemoryMetrics =
            new io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics();
        jvmMemoryMetrics.bindTo(prometheusRegistry);

        io.micrometer.core.instrument.binder.system.ProcessorMetrics processorMetrics =
            new io.micrometer.core.instrument.binder.system.ProcessorMetrics();
        processorMetrics.bindTo(prometheusRegistry);

        io.micrometer.core.instrument.binder.system.UptimeMetrics uptimeMetrics =
            new io.micrometer.core.instrument.binder.system.UptimeMetrics();
        uptimeMetrics.bindTo(prometheusRegistry);

        io.micrometer.core.instrument.binder.system.FileDescriptorMetrics fdMetrics =
            new io.micrometer.core.instrument.binder.system.FileDescriptorMetrics();
        fdMetrics.bindTo(prometheusRegistry);
    }

    // Métodos para obtener valores del sistema (simplificados para demostración)
    private double getActiveConnections() {
        // En implementación real, obtendría de Netty/Vert.x
        return Math.random() * 100;
    }

    private double getSystemCpuUsage() {
        // En implementación real, obtendría del OS
        return Math.random() * 100;
    }

    private double getSystemMemoryUsage() {
        // En implementación real, obtendría del OS
        return Math.random() * 100;
    }

    private double getJvmHeapUsed() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private double getJvmHeapMax() {
        return Runtime.getRuntime().maxMemory();
    }
}
