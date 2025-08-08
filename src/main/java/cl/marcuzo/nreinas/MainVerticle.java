package cl.marcuzo.nreinas;

import cl.marcuzo.nreinas.config.ApplicationConfig;
import cl.marcuzo.nreinas.metrics.MetricsManager;
import cl.marcuzo.nreinas.verticle.OrquestadorVerticle;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;


/**
 * MainVerticle - Patrón Gateway + Facade
 *
 * Implementa el patrón API Gateway actuando como punto único de entrada
 * al sistema distribuido de N-Reinas. Coordina el enrutamiento HTTP,
 * gestión de métricas y comunicación con el orquestador.
 *
 * Responsabilidades:
 * - Gateway HTTP con enrutamiento
 * - Gestión centralizada de métricas
 * - Validación de entrada y manejo de errores
 * - Configuración del sistema distribuido
 *
 * Patrones aplicados:
 * - Gateway Pattern: Punto único de entrada
 * - Facade Pattern: Simplifica interfaz compleja
 * - Chain of Responsibility: Pipeline de request processing
 * - Observer Pattern: Métricas y monitoreo
 */
public class MainVerticle extends AbstractVerticle {

  private MetricsManager metricsManager;

  @Override
  public void start(Promise<Void> startPromise) {
    MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)).setEnabled(true);

    // Configurar opciones de Vert.x con clustering habilitado
    VertxOptions vertxOptions = new VertxOptions()
        .setMetricsOptions(metricsOptions);

    // Crear instancia de Vert.x con clustering
    Vertx.clusteredVertx(vertxOptions, res -> {
      if (res.succeeded()) {
        vertx = res.result();
        System.out.println("Vert.x cluster iniciado correctamente");
        initializeApplication(startPromise);
      } else {
        System.out.println("Error iniciando cluster: " + res.cause().getMessage());
        // Fallback a modo no-cluster
        vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(metricsOptions));
        System.out.println("Iniciando en modo no-cluster como fallback");
        initializeApplication(startPromise);
      }
    });
  }

    private void initializeApplication(Promise<Void> startPromise) {
    try {
      // Inicializar gestor de métricas
      metricsManager = MetricsManager.getInstance();
      System.out.println("✅ MetricsManager inicializado");

      // Desplegar OrquestadorVerticle
      vertx.deployVerticle(OrquestadorVerticle.class.getName(), res -> {
        if (res.succeeded()) {
          System.out.println("✅ OrquestadorVerticle desplegado correctamente");
          setupHttpServer(startPromise);
        } else {
          System.out.println("❌ Error al desplegar el OrquestadorVerticle: " + res.cause().getMessage());
          startPromise.fail(res.cause());
        }
      });

    } catch (Exception e) {
      System.out.println("❌ Error durante la inicialización: " + e.getMessage());
      startPromise.fail(e);
    }
  }

  /**
   * Configura el servidor HTTP con enrutamiento y middleware
   */
  private void setupHttpServer(Promise<Void> startPromise) {
    Router router = Router.router(vertx);

    // Middleware global para CORS y logging
    setupGlobalMiddleware(router);

    // Configurar rutas
    setupRoutes(router);

    // Iniciar servidor HTTP
    startHttpServer(router, startPromise);
  }

  /**
   * Configura middleware global (Patrón Chain of Responsibility)
   */
  private void setupGlobalMiddleware(Router router) {
    // CORS middleware
    router.route().handler(context -> {
      context.response()
          .putHeader("Access-Control-Allow-Origin", "*")
          .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
          .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

      if ("OPTIONS".equals(context.request().method().toString())) {
        context.response().setStatusCode(200).end();
      } else {
        context.next();
      }
    });

    // Request logging middleware
    router.route().handler(context -> {
      String method = context.request().method().toString();
      String path = context.request().path();
      long startTime = System.currentTimeMillis();

      System.out.println("📥 " + method + " " + path);

      context.addBodyEndHandler(v -> {
        long duration = System.currentTimeMillis() - startTime;
        int statusCode = context.response().getStatusCode();
        System.out.println("📤 " + method + " " + path + " " + statusCode + " (" + duration + "ms)");
      });

      context.next();
    });
  }

  /**
   * Configura todas las rutas de la aplicación
   */
  private void setupRoutes(Router router) {
    // Ruta de métricas
    router.get(ApplicationConfig.METRICS_PATH).handler(this::handleMetricsRequest);

    // Rutas de la aplicación
    router.get("/solve").handler(this::handleSolveRequest);
    router.get("/status").handler(this::handleStatusRequest);
    router.get("/").handler(this::handleIndexRequest);

    // Ruta de health check
    router.get("/health").handler(this::handleHealthRequest);

    // Manejador de rutas no encontradas
    router.route().last().handler(this::handleNotFound);
  }

  /**
   * Maneja requests de métricas
   */
  private void handleMetricsRequest(RoutingContext context) {
    try {
      String metrics = metricsManager.getMetricsAsPrometheusFormat();
      context.response()
          .putHeader("content-type", "text/plain; version=0.0.4")
          .end(metrics);
    } catch (Exception e) {
      System.out.println("❌ Error obteniendo métricas: " + e.getMessage());
      context.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", "Error interno del servidor").encode());
    }
  }

  /**
   * Inicia el servidor HTTP
   */
  private void startHttpServer(Router router, Promise<Void> startPromise) {
    vertx.createHttpServer()
        .requestHandler(router)
        .listen(ApplicationConfig.DEFAULT_HTTP_PORT, ApplicationConfig.DEFAULT_HOST, http -> {
          if (http.succeeded()) {
            System.out.println("🚀 HTTP server iniciado en puerto " + ApplicationConfig.DEFAULT_HTTP_PORT);
            System.out.println("📊 Métricas disponibles en: http://localhost:" + ApplicationConfig.DEFAULT_HTTP_PORT + ApplicationConfig.METRICS_PATH);
            startPromise.complete();
          } else {
            System.out.println("❌ Fallo al iniciar HTTP server: " + http.cause().getMessage());
            startPromise.fail(http.cause());
          }
        });
  }

    /**
   * Maneja solicitudes de resolución de N-Reinas
   * Implementa validación, métricas y orquestación
   */
  private void handleSolveRequest(RoutingContext context) {
    Timer.Sample timerSample = metricsManager.startHttpTimer();
    Timer.Sample nqueensTimer = metricsManager.startNQueensTimer();

    metricsManager.incrementHttpRequests();
    metricsManager.incrementActiveSessions();

    try {
      // Extraer y validar parámetros
      RequestParameters params = extractAndValidateParameters(context);
      if (params == null) {
        metricsManager.decrementActiveSessions();
        return; // Error ya manejado en extractAndValidateParameters
      }

      metricsManager.incrementNQueensRequests();

      System.out.println("🏰 === Iniciando resolución N-Reinas ===");
      System.out.println("📊 N = " + params.N + ", Workers = " + params.workers);

      long startTime = System.currentTimeMillis();
      JsonObject request = new JsonObject()
          .put("N", params.N)
          .put("workers", params.workers)
          .put("requestId", generateRequestId())
          .put("startTime", startTime);

      // Enviar solicitud al orquestador
      io.vertx.core.eventbus.DeliveryOptions options = new io.vertx.core.eventbus.DeliveryOptions()
          .setSendTimeout(ApplicationConfig.calculateRecommendedTimeout(params.N));

      vertx.eventBus().request(ApplicationConfig.ORCHESTRATOR_ADDRESS, request, options, reply -> {

        long elapsedTime = System.currentTimeMillis() - startTime;
        metricsManager.stopHttpTimer(timerSample);
        metricsManager.stopNQueensTimer(nqueensTimer);
        metricsManager.decrementActiveSessions();

        System.out.println("✅ === Resolución completada en " + elapsedTime + " ms ===");

        if (reply.succeeded()) {
          handleSuccessfulResponse(context, reply.result().body(), startTime, elapsedTime);
        } else {
          handleErrorResponse(context, reply.cause(), elapsedTime);
        }
      });

    } catch (NumberFormatException e) {
      handleValidationError(context, "Parámetros inválidos",
          "N y workers deben ser números enteros", timerSample, nqueensTimer);
    } catch (Exception e) {
      handleValidationError(context, "Error interno",
          "Error inesperado: " + e.getMessage(), timerSample, nqueensTimer);
    }
  }

  /**
   * Clase para encapsular parámetros de request
   */
  private static class RequestParameters {
    final int N;
    final int workers;

    RequestParameters(int N, int workers) {
      this.N = N;
      this.workers = workers;
    }
  }

  /**
   * Extrae y valida parámetros de la request
   */
  private RequestParameters extractAndValidateParameters(RoutingContext context) {
    try {
      int N = Integer.parseInt(context.request().getParam("n", String.valueOf(ApplicationConfig.DEFAULT_BOARD_SIZE)));
      int workers = Integer.parseInt(context.request().getParam("workers", String.valueOf(ApplicationConfig.DEFAULT_WORKERS)));

      // Validar N
      if (!ApplicationConfig.isValidBoardSize(N)) {
        respondWithError(context, 400, "Tamaño de tablero inválido",
            "N debe estar entre " + ApplicationConfig.MIN_BOARD_SIZE + " y " + ApplicationConfig.MAX_BOARD_SIZE, N);
        return null;
      }

      // Validar workers
      if (!ApplicationConfig.isValidWorkerCount(workers)) {
        respondWithError(context, 400, "Número de workers inválido",
            "Workers debe estar entre " + ApplicationConfig.MIN_WORKERS + " y " + ApplicationConfig.MAX_WORKERS, workers);
        return null;
      }

      return new RequestParameters(N, workers);

    } catch (NumberFormatException e) {
      respondWithError(context, 400, "Parámetros inválidos",
          "N y workers deben ser números enteros", null);
      return null;
    }
  }

  /**
   * Maneja respuesta exitosa
   */
  private void handleSuccessfulResponse(RoutingContext context, Object responseBody, long startTime, long elapsedTime) {
    JsonObject response = (JsonObject) responseBody;

    // Agregar metadata adicional
    response.put("requestStartTime", startTime);
    response.put("totalElapsedTimeMs", elapsedTime);
    response.put("timestamp", System.currentTimeMillis());

    // Registrar métricas de soluciones encontradas
    int solutionsFound = response.getInteger("totalSolutions", 0);
    if (solutionsFound > 0) {
      metricsManager.recordSolutionsFound(solutionsFound);
    }

    context.response()
        .putHeader("content-type", "application/json")
        .end(response.encodePrettily());
  }

  /**
   * Maneja respuesta de error
   */
  private void handleErrorResponse(RoutingContext context, Throwable cause, long elapsedTime) {
    metricsManager.incrementHttpErrors();
    System.out.println("❌ Error en la resolución: " + cause.getMessage());

    JsonObject errorResponse = new JsonObject()
        .put("error", "Error interno del servidor")
        .put("message", cause.getMessage())
        .put("elapsedTimeMs", elapsedTime)
        .put("timestamp", System.currentTimeMillis());

    context.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(errorResponse.encodePrettily());
  }

  /**
   * Maneja errores de validación
   */
  private void handleValidationError(RoutingContext context, String error, String message,
                                   Timer.Sample timerSample, Timer.Sample nqueensTimer) {
    metricsManager.incrementHttpErrors();
    metricsManager.stopHttpTimer(timerSample);
    metricsManager.stopNQueensTimer(nqueensTimer);
    metricsManager.decrementActiveSessions();

    respondWithError(context, 400, error, message, null);
  }

  /**
   * Utilitario para respuestas de error
   */
  private void respondWithError(RoutingContext context, int statusCode, String error, String message, Object provided) {
    JsonObject errorResponse = new JsonObject()
        .put("error", error)
        .put("message", message)
        .put("timestamp", System.currentTimeMillis());

    if (provided != null) {
      errorResponse.put("provided", provided);
    }

    context.response()
        .setStatusCode(statusCode)
        .putHeader("content-type", "application/json")
        .end(errorResponse.encodePrettily());
  }

  /**
   * Genera ID único para requests
   */
  private String generateRequestId() {
    return "req_" + System.currentTimeMillis() + "_" + Math.random();
  }

  /**
   * Maneja requests de health check
   */
  private void handleHealthRequest(RoutingContext context) {
    JsonObject health = new JsonObject()
        .put("status", "UP")
        .put("timestamp", System.currentTimeMillis())
        .put("uptime", System.currentTimeMillis()) // Simplificado
        .put("version", "1.0.0")
        .put("environment", "development");

    context.response()
        .putHeader("content-type", "application/json")
        .end(health.encodePrettily());
  }

  /**
   * Maneja requests de estado del sistema
   */
  private void handleStatusRequest(RoutingContext context) {
    vertx.eventBus().request(ApplicationConfig.STATE_SUMMARY_ADDRESS, new JsonObject(), reply -> {
      JsonObject response = new JsonObject()
          .put("server", "N-Reinas Distribuido")
          .put("status", "running")
          .put("clustering", vertx.isClustered())
          .put("timestamp", System.currentTimeMillis())
          .put("metricsEnabled", true)
          .put("version", "1.0.0");

      if (reply.succeeded()) {
        JsonObject stateData = (JsonObject) reply.result().body();
        response.put("currentState", stateData);
      } else {
        response.put("stateManager", "not available");
      }

      context.response()
          .putHeader("content-type", "application/json")
          .end(response.encodePrettily());
    });
  }

  /**
   * Maneja requests de la página principal con documentación
   */
  private void handleIndexRequest(RoutingContext context) {
    String html = buildDocumentationPage();
    context.response()
        .putHeader("content-type", "text/html; charset=utf-8")
        .end(html);
  }

  /**
   * Maneja rutas no encontradas
   */
  private void handleNotFound(RoutingContext context) {
    JsonObject notFound = new JsonObject()
        .put("error", "Endpoint no encontrado")
        .put("path", context.request().path())
        .put("method", context.request().method().toString())
        .put("timestamp", System.currentTimeMillis())
        .put("availableEndpoints", new JsonObject()
            .put("GET /", "Documentación de la API")
            .put("GET /solve", "Resolver N-Reinas")
            .put("GET /status", "Estado del sistema")
            .put("GET /health", "Health check")
            .put("GET /metrics", "Métricas de Prometheus"));

    context.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(notFound.encodePrettily());
  }

  /**
   * Construye la página de documentación HTML
   */
  private String buildDocumentationPage() {
    return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>N-Reinas Distribuido - API</title>
            <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 40px; background: #f5f5f5; }
                .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }
                h2 { color: #34495e; margin-top: 30px; }
                .endpoint { background: #ecf0f1; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #3498db; }
                .method { background: #27ae60; color: white; padding: 4px 8px; border-radius: 3px; font-weight: bold; margin-right: 10px; }
                .url { font-family: monospace; background: #34495e; color: white; padding: 4px 8px; border-radius: 3px; }
                .example { background: #2c3e50; color: #ecf0f1; padding: 15px; border-radius: 5px; margin: 10px 0; font-family: monospace; overflow-x: auto; }
                .parameter { background: #f39c12; color: white; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; }
                .architecture { background: #e8f4fd; padding: 20px; border-radius: 5px; margin: 20px 0; }
                .pattern { background: #fff3cd; padding: 10px; margin: 5px 0; border-radius: 3px; border-left: 4px solid #ffc107; }
                ul { line-height: 1.6; }
                .metrics-section { background: #d4edda; padding: 20px; border-radius: 5px; margin: 20px 0; }
                .footer { text-align: center; margin-top: 40px; color: #7f8c8d; border-top: 1px solid #bdc3c7; padding-top: 20px; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🏰 N-Reinas Distribuido - API Educativa</h1>

                <p><strong>Sistema distribuido para resolver el problema de las N-Reinas usando Vert.x, patrones de diseño y observabilidad completa.</strong></p>

                <div class="architecture">
                    <h2>🏗️ Arquitectura del Sistema</h2>
                    <p>Este sistema implementa múltiples patrones de diseño distribuidos:</p>
                    <div class="pattern"><strong>Gateway Pattern:</strong> MainVerticle como punto único de entrada</div>
                    <div class="pattern"><strong>Orchestrator Pattern:</strong> OrquestadorVerticle coordina workers</div>
                    <div class="pattern"><strong>Worker Pattern:</strong> WorkerVerticles procesan en paralelo</div>
                    <div class="pattern"><strong>State Pattern:</strong> StateManagerVerticle gestiona estado compartido</div>
                </div>

                <h2>📡 Endpoints Disponibles</h2>

                <div class="endpoint">
                    <span class="method">GET</span>
                    <span class="url">/solve</span>
                    <p><strong>Resuelve el problema de N-Reinas de manera distribuida</strong></p>
                    <p><strong>Parámetros:</strong></p>
                    <ul>
                        <li><span class="parameter">n</span> - Tamaño del tablero (1-20, default: 8)</li>
                        <li><span class="parameter">workers</span> - Número de workers (1-10, default: 4)</li>
                    </ul>
                    <div class="example">curl "http://localhost:8080/solve?n=8&workers=4"</div>
                </div>

                <div class="endpoint">
                    <span class="method">GET</span>
                    <span class="url">/status</span>
                    <p><strong>Estado actual del sistema distribuido</strong></p>
                    <div class="example">curl "http://localhost:8080/status"</div>
                </div>

                <div class="endpoint">
                    <span class="method">GET</span>
                    <span class="url">/health</span>
                    <p><strong>Health check del servicio</strong></p>
                    <div class="example">curl "http://localhost:8080/health"</div>
                </div>

                <div class="endpoint">
                    <span class="method">GET</span>
                    <span class="url">/metrics</span>
                    <p><strong>Métricas en formato Prometheus</strong></p>
                    <div class="example">curl "http://localhost:8080/metrics"</div>
                </div>

                <div class="metrics-section">
                    <h2>📊 Métricas y Monitoreo</h2>
                    <p>El sistema implementa <strong>The Four Golden Signals</strong> de observabilidad:</p>
                    <ul>
                        <li><strong>Latency:</strong> Tiempo de respuesta de requests y workers</li>
                        <li><strong>Traffic:</strong> Volumen de requests por segundo</li>
                        <li><strong>Errors:</strong> Tasa de errores HTTP y de workers</li>
                        <li><strong>Saturation:</strong> Uso de memoria JVM y recursos del sistema</li>
                    </ul>
                    <p><strong>Stack de Monitoreo:</strong> Prometheus + Grafana + AlertManager</p>
                </div>

                <h2>🧩 El Problema de las N-Reinas</h2>
                <p>Colocar <strong>N reinas</strong> en un tablero de <strong>NxN</strong> de manera que ninguna reina pueda atacar a otra:</p>
                <ul>
                    <li>❌ No dos reinas en la misma fila</li>
                    <li>❌ No dos reinas en la misma columna</li>
                    <li>❌ No dos reinas en la misma diagonal</li>
                </ul>

                <h2>⚡ Ventajas de Vert.x</h2>
                <ul>
                    <li><strong>Asíncrono y no-bloqueante:</strong> Miles de conexiones concurrentes</li>
                    <li><strong>Event Bus distribuido:</strong> Comunicación desacoplada entre componentes</li>
                    <li><strong>Escalabilidad horizontal:</strong> Event Loop por CPU core</li>
                    <li><strong>Ecosistema rico:</strong> Web, clustering, métricas integradas</li>
                </ul>

                <h2>⚠️ Consideraciones de Vert.x</h2>
                <ul>
                    <li><strong>Curva de aprendizaje:</strong> Paradigma asíncrono requiere pensamiento diferente</li>
                    <li><strong>Debugging complejo:</strong> Stack traces asíncronos más difíciles de seguir</li>
                    <li><strong>Callback complexity:</strong> Anidamiento profundo sin Futures/Promises</li>
                </ul>

                <h2>🐳 Despliegue con Docker</h2>
                <div class="example">
# Despliegue completo con stack de monitoreo
./deploy.sh

# Solo la aplicación localmente
./demo.sh

# Ver logs
docker-compose logs -f nreinas-app
                </div>

                <h2>🎯 Ejemplos de Uso</h2>
                <div class="example">
# Problema clásico de 8-Reinas
curl "http://localhost:8080/solve?n=8&workers=4"

# Problema pequeño de 4-Reinas
curl "http://localhost:8080/solve?n=4&workers=2"

# Problema grande de 12-Reinas
curl "http://localhost:8080/solve?n=12&workers=6"

# Ver estado del sistema
curl "http://localhost:8080/status"

# Health check
curl "http://localhost:8080/health"
                </div>

                <div class="footer">
                    <p>🎓 <strong>Proyecto Educativo</strong> - Demostrando patrones distribuidos, observabilidad y mejores prácticas con Vert.x</p>
                    <p>📚 <a href="https://vertx.io/docs/" target="_blank">Documentación de Vert.x</a> |
                       📊 <a href="http://localhost:3000" target="_blank">Grafana Dashboards</a> |
                       🔍 <a href="http://localhost:9090" target="_blank">Prometheus</a></p>
                </div>
            </div>
        </body>
        </html>
        """;
  }
}

