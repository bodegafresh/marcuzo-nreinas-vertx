# 🏰 N-Reinas Distribuido: Demostración Educativa

**Una implementación completa del problema de las N-Reinas usando arquitectura distribuida, patrones de diseño y mejores prácticas de desarrollo.**

---

## 📚 **Objetivos Educativos**

Este proyecto fue diseñado como herramienta educativa para demostrar:

1. **🚀 Ventajas y Desventajas de Vert.x**
2. **🏗️ Patrones de Diseño en Sistemas Distribuidos**
3. **📊 Importancia de las Métricas en Sistemas Distribuidos**
4. **🐳 Gestión de Despliegues con Docker**

---

## 🧩 **El Problema de las N-Reinas**

### ¿Qué es el Problema de las N-Reinas?

El problema de las N-Reinas consiste en **colocar N reinas en un tablero de ajedrez de NxN** de manera que **ninguna reina pueda atacar a otra**.

#### Reglas del Ajedrez para las Reinas:

- ✅ **Movimiento horizontal**: Una reina puede moverse en cualquier dirección horizontal
- ✅ **Movimiento vertical**: Una reina puede moverse en cualquier dirección vertical
- ✅ **Movimiento diagonal**: Una reina puede moverse en cualquier dirección diagonal

#### Restricciones del Problema:

- ❌ **No dos reinas en la misma fila**
- ❌ **No dos reinas en la misma columna**
- ❌ **No dos reinas en la misma diagonal**

### Ejemplo: 4-Reinas

```
Solución 1:     Solución 2:
. Q . .         . . Q .
. . . Q         Q . . .
Q . . .         . . . Q
. . Q .         . Q . .
```

### Complejidad Computacional

- **Espacio de búsqueda**: N! permutaciones
- **Para N=8**: ~40,320 configuraciones posibles
- **Para N=12**: ~479,001,600 configuraciones
- **Algoritmo clásico**: Backtracking con poda

---

## 🚀 **1. Vert.x: Ventajas y Desventajas**

### ¿Qué es Vert.x?

**Eclipse Vert.x** es un toolkit para construir aplicaciones reactivas en la JVM, diseñado para ser:

- **Asíncrono y no-bloqueante**
- **Orientado a eventos**
- **Altamente escalable**

### ✅ **Ventajas de Vert.x**

#### **1. Modelo de Concurrencia Superior**

```java
// ❌ Modelo tradicional (bloqueante)
public String processRequest() {
    Thread.sleep(1000); // Bloquea el hilo
    return "result";
}

// ✅ Modelo Vert.x (no-bloqueante)
public void processRequest(Handler<AsyncResult<String>> handler) {
    vertx.setTimer(1000, id -> handler.handle(Future.succeededFuture("result")));
}
```

#### **2. Event Bus Distribuido**

```java
// Comunicación entre componentes sin acoplamiento directo
eventBus.send("worker.process", data, reply -> {
    if (reply.succeeded()) {
        // Manejar respuesta asíncrona
    }
});
```

#### **3. Escalabilidad Horizontal**

- **Event Loop por CPU core**
- **Miles de conexiones concurrentes**
- **Memoria eficiente**

#### **4. Ecosistema Rico**

- **Vert.x Web**: Enrutamiento HTTP
- **Vert.x Cluster**: Clustering automático
- **Vert.x Metrics**: Integración con Prometheus/Micrometer

### ❌ **Desventajas de Vert.x**

#### **1. Curva de Aprendizaje**

```java
// Callback Hell - puede volverse complejo
vertx.eventBus().request("service1", data1, reply1 -> {
    if (reply1.succeeded()) {
        vertx.eventBus().request("service2", reply1.result().body(), reply2 -> {
            if (reply2.succeeded()) {
                vertx.eventBus().request("service3", reply2.result().body(), reply3 -> {
                    // Anidamiento profundo...
                });
            }
        });
    }
});
```

#### **2. Debugging Complejo**

- **Stack traces asíncronos difíciles de seguir**
- **Estado distribuido entre event loops**
- **Debugging de condiciones de carrera**

#### **3. Paradigma Mental Diferente**

- **Pensamiento asíncrono requerido**
- **Manejo de estado compartido complejo**
- **Testing de código asíncrono más difícil**

### 📊 **Comparación con Otras Tecnologías**

| Característica       | Vert.x       | Spring Boot        | Node.js    | Akka        |
| -------------------- | ------------ | ------------------ | ---------- | ----------- |
| **Modelo**           | Event-driven | Thread-per-request | Event Loop | Actor Model |
| **Rendimiento**      | ⭐⭐⭐⭐⭐   | ⭐⭐⭐             | ⭐⭐⭐⭐   | ⭐⭐⭐⭐⭐  |
| **Facilidad de uso** | ⭐⭐⭐       | ⭐⭐⭐⭐⭐         | ⭐⭐⭐⭐   | ⭐⭐        |
| **Ecosistema**       | ⭐⭐⭐⭐     | ⭐⭐⭐⭐⭐         | ⭐⭐⭐⭐⭐ | ⭐⭐⭐      |
| **Escalabilidad**    | ⭐⭐⭐⭐⭐   | ⭐⭐⭐             | ⭐⭐⭐⭐   | ⭐⭐⭐⭐⭐  |

---

## 🏗️ **2. Patrones de Diseño Distribuidos**

### **Arquitectura del Sistema**

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENTE HTTP                                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │ HTTP Request
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                 MAIN VERTICLE                                   │
│              (Gateway Pattern)                                  │
└─────────────────────┬───────────────────────────────────────────┘
                      │ Event Bus
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│               ORQUESTADOR VERTICLE                              │
│              (Orchestrator Pattern)                             │
└─────────────────────┬───────────────────────────────────────────┘
                      │ Event Bus
                      ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ STATE MANAGER   │    │ WORKER VERTICLE │    │ WORKER VERTICLE │
│   VERTICLE      │◄──►│      #0         │    │      #N         │
│ (State Pattern) │    │ (Worker Pattern)│    │ (Worker Pattern)│
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### **Patrones Implementados**

#### **1. Gateway Pattern (API Gateway)**

```java
// MainVerticle.java - Punto único de entrada
public class MainVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);

        // Ruteo centralizado
        router.get("/solve").handler(this::handleSolveRequest);
        router.get("/status").handler(this::handleStatusRequest);

        // Métricas centralizadas
        setupMetrics(router);
    }
}
```

**Ventajas:**

- ✅ Punto único de entrada
- ✅ Autenticación/autorización centralizada
- ✅ Rate limiting global
- ✅ Métricas unificadas

#### **2. Orchestrator Pattern (Coordinación)**

```java
// OrquestadorVerticle.java - Coordina flujo de trabajo
public class OrquestadorVerticle extends AbstractVerticle {
    private void handleNQueensRequest(Message<Object> message) {
        // 1. Validar entrada
        // 2. Desplegar workers
        // 3. Coordinar ejecución
        // 4. Agregar resultados
        // 5. Responder al cliente

        deployWorkers(N, numWorkers)
            .compose(this::monitorExecution)
            .onSuccess(result -> message.reply(result))
            .onFailure(error -> message.fail(500, error.getMessage()));
    }
}
```

**Ventajas:**

- ✅ Separación de responsabilidades
- ✅ Flujo de trabajo claro
- ✅ Manejo centralizado de errores
- ✅ Fácil testing y debugging

#### **3. Worker Pattern (Procesamiento Distribuido)**

```java
// WorkerVerticle.java - Unidad de trabajo
public class WorkerVerticle extends AbstractVerticle {
    @Override
    public void start() {
        // Registrar consumer para trabajo específico
        eventBus.consumer("worker." + workerId + ".process", this::processWork);

        // Iniciar procesamiento asignado
        if (workerId == 0) {
            // Worker principal ejecuta algoritmo completo
            solveNQueensComplete();
        }
    }
}
```

**Ventajas:**

- ✅ Escalabilidad horizontal
- ✅ Aislamiento de fallos
- ✅ Distribución de carga
- ✅ Recuperación independiente

#### **4. State Pattern (Gestión de Estado Distribuido)**

```java
// StateManagerVerticle.java - Estado compartido
public class StateManagerVerticle extends AbstractVerticle {
    private final Map<String, Set<List<Integer>>> stateCache = new ConcurrentHashMap<>();

    @Override
    public void start() {
        // Gestión centralizada de estados válidos
        eventBus.consumer("state.update", this::updateState);
        eventBus.consumer("state.get", this::getState);
        eventBus.consumer("solution.found", this::storeSolution);
    }
}
```

**Ventajas:**

- ✅ Estado consistente
- ✅ Cache distribuido
- ✅ Sincronización centralizada
- ✅ Recuperación de estado

#### **5. Circuit Breaker Pattern (Resiliencia)**

```java
// Implementación de tolerancia a fallos
public class ResilientWorkerVerticle extends AbstractVerticle {
    private CircuitBreaker circuitBreaker;

    @Override
    public void start() {
        circuitBreaker = CircuitBreaker.create("worker-cb", vertx)
            .setMaxFailures(5)
            .setTimeout(30000)
            .setResetTimeout(60000);
    }

    private void processWithCircuitBreaker(JsonObject data) {
        circuitBreaker.execute(promise -> {
            processWork(data)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        });
    }
}
```

### **Principios SOLID Aplicados**

#### **S - Single Responsibility Principle**

- `MainVerticle`: Solo maneja HTTP y enrutamiento
- `OrquestadorVerticle`: Solo coordina flujo de trabajo
- `WorkerVerticle`: Solo procesa N-Reinas
- `StateManagerVerticle`: Solo gestiona estado compartido

#### **O - Open/Closed Principle**

```java
// Extensible sin modificar código existente
public abstract class BaseWorkerVerticle extends AbstractVerticle {
    protected abstract void processWork(JsonObject data);
}

public class NQueensWorkerVerticle extends BaseWorkerVerticle {
    @Override
    protected void processWork(JsonObject data) {
        // Implementación específica para N-Reinas
    }
}
```

#### **D - Dependency Inversion Principle**

```java
// Dependencia de abstracciones, no implementaciones
public interface StateManager {
    Future<Void> updateState(String key, JsonArray states);
    Future<JsonArray> getState(String key);
}

public class DistributedStateManager implements StateManager {
    // Implementación específica con Event Bus
}
```

---

## 📊 **3. Importancia de las Métricas en Sistemas Distribuidos**

### **¿Por qué son Críticas las Métricas?**

En sistemas distribuidos, las métricas son **esenciales** porque:

1. **Visibilidad**: Los componentes están dispersos
2. **Debugging**: Los errores pueden propagarse entre servicios
3. **Performance**: Cuellos de botella no evidentes
4. **Escalabilidad**: Decisiones basadas en datos reales
5. **SLA/SLO**: Cumplimiento de acuerdos de servicio

### **Tipos de Métricas Implementadas**

#### **1. Métricas de Negocio (Business Metrics)**

```java
// Métricas específicas del dominio N-Reinas
Counter nqueensRequestsCounter = Counter.builder("nqueens_requests_total")
    .description("Total de solicitudes de N-Reinas")
    .tag("application", "nreinas")
    .register(prometheusRegistry);

Counter solutionsFoundCounter = Counter.builder("worker_solutions_found_total")
    .description("Soluciones encontradas por workers")
    .tag("worker", "nreinas")
    .register(prometheusRegistry);
```

**¿Qué nos dice?**

- Volumen de uso del sistema
- Efectividad del algoritmo
- Patrones de uso por tamaño de problema

#### **2. Métricas de Performance (Performance Metrics)**

```java
// Tiempo de respuesta y throughput
Timer httpRequestsTimer = Timer.builder("http_requests_duration_seconds")
    .description("Duración de peticiones HTTP")
    .tag("application", "nreinas")
    .register(prometheusRegistry);

Timer workerExecutionTimer = Timer.builder("worker_execution_time_seconds")
    .description("Tiempo de ejecución del WorkerVerticle")
    .tag("worker", "nreinas")
    .register(prometheusRegistry);
```

**¿Qué nos dice?**

- Latencia percibida por el usuario
- Tiempo de procesamiento interno
- Identificación de cuellos de botella

#### **3. Métricas de Error (Error Metrics)**

```java
// Tasa de errores y fallos
Counter httpRequestsErrorCounter = Counter.builder("http_requests_errors_total")
    .description("Total de errores en peticiones HTTP")
    .tag("application", "nreinas")
    .register(prometheusRegistry);

Counter workerErrorCounter = Counter.builder("worker_errors_total")
    .description("Número de errores en el WorkerVerticle")
    .tag("worker", "nreinas")
    .register(prometheusRegistry);
```

**¿Qué nos dice?**

- Estabilidad del sistema
- Calidad del código
- Impacto en la experiencia del usuario

#### **4. Métricas de Sistema (System Metrics)**

```java
// Recursos del sistema y JVM
new JvmMemoryMetrics().bindTo(prometheusRegistry);
new ProcessorMetrics().bindTo(prometheusRegistry);
new UptimeMetrics().bindTo(prometheusRegistry);
```

**¿Qué nos dice?**

- Utilización de recursos
- Necesidades de escalamiento
- Salud general del sistema

### **The Four Golden Signals**

Siguiendo las mejores prácticas de Google SRE:

#### **1. Latency (Latencia)**

```promql
# P95 de tiempo de respuesta
histogram_quantile(0.95, rate(http_requests_duration_seconds_bucket[5m]))
```

#### **2. Traffic (Tráfico)**

```promql
# Requests por segundo
rate(http_requests_total[5m])
```

#### **3. Errors (Errores)**

```promql
# Tasa de error
rate(http_requests_errors_total[5m]) / rate(http_requests_total[5m])
```

#### **4. Saturation (Saturación)**

```promql
# Uso de memoria JVM
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

### **Alertas Inteligentes**

#### **Alertas por Nivel de Severidad**

```yaml
# monitoring/rules/nreinas_alerts.yml
groups:
  - name: nreinas_critical
    rules:
      - alert: ApplicationDown
        expr: up{job="nreinas-app"} == 0
        for: 30s
        labels:
          severity: critical
        annotations:
          summary: "🚨 Aplicación N-Reinas está caída"

      - alert: HighErrorRate
        expr: rate(http_requests_errors_total[5m]) / rate(http_requests_total[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "⚠️ Alta tasa de errores: {{ $value }}%"
```

### **Dashboards para Diferentes Audiencias**

#### **1. Dashboard Ejecutivo**

- ✅ Uptime del sistema
- ✅ Requests exitosos vs fallidos
- ✅ Tiempo promedio de respuesta
- ✅ Número de usuarios activos

#### **2. Dashboard de Desarrollo**

- ✅ Tiempo de ejecución por método
- ✅ Errores por componente
- ✅ Uso de memoria detallado
- ✅ Logs de error recientes

#### **3. Dashboard de Operaciones**

- ✅ CPU y memoria por contenedor
- ✅ Tráfico de red
- ✅ Espacio en disco
- ✅ Estado de servicios dependientes

---

## 🐳 **4. Gestión de Despliegues con Docker**

### **¿Por qué Docker en Sistemas Distribuidos?**

#### **Beneficios Clave:**

1. **Consistencia**: "Funciona en mi máquina" → "Funciona en todas partes"
2. **Aislamiento**: Dependencias encapsuladas
3. **Escalabilidad**: Fácil replicación horizontal
4. **Portabilidad**: Mismo contenedor en dev/staging/prod
5. **Versionado**: Tags inmutables para rollbacks

### **Estrategia de Containerización**

#### **1. Multi-Stage Build**

```dockerfile
# Dockerfile optimizado para producción
FROM openjdk:17-jdk-slim AS builder

# Etapa de construcción
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src/ src/
RUN ./gradlew shadowJar --no-daemon

# Etapa de runtime
FROM openjdk:17-jre-slim
WORKDIR /app

# Crear usuario no-root para seguridad
RUN groupadd -r nreinas && useradd -r -g nreinas nreinas

# Copiar artefacto desde builder
COPY --from=builder /app/build/libs/*-fat.jar app.jar
COPY prometheus.yml ./

# Configurar usuario y permisos
RUN chown -R nreinas:nreinas /app
USER nreinas

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/status || exit 1

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

**Ventajas:**

- ✅ Imagen final más pequeña
- ✅ Sin herramientas de build en producción
- ✅ Mejor seguridad
- ✅ Startup más rápido

#### **2. Docker Compose para Orquestación Local**

```yaml
# docker-compose.yml - Stack completo
version: "3.8"

services:
  # Aplicación principal
  nreinas-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Xmx1g -Xms512m
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/status"]
      interval: 30s
      timeout: 10s
      retries: 3
    depends_on:
      - prometheus
    networks:
      - monitoring

  # Stack de monitoreo
  prometheus:
    image: prom/prometheus:v2.48.0
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:10.2.2
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
    volumes:
      - ./monitoring/grafana:/var/lib/grafana/dashboards
    networks:
      - monitoring

networks:
  monitoring:
    driver: bridge
```

#### **3. Estrategias de Despliegue**

##### **Blue-Green Deployment**

```bash
#!/bin/bash
# deploy-blue-green.sh

# 1. Construir nueva versión (Green)
docker-compose -f docker-compose.green.yml up -d

# 2. Health check en Green
curl -f http://localhost:8081/status

# 3. Cambiar tráfico de Blue a Green
# (Actualizar load balancer/proxy)

# 4. Detener versión anterior (Blue)
docker-compose -f docker-compose.blue.yml down
```

##### **Rolling Deployment**

```bash
#!/bin/bash
# deploy-rolling.sh

# Actualizar instancias una por una
for instance in {1..3}; do
    echo "Actualizando instancia $instance"
    docker-compose up -d --scale nreinas-app=$instance
    sleep 30
done
```

##### **Canary Deployment**

```yaml
# docker-compose.canary.yml
version: "3.8"
services:
  nreinas-app:
    image: nreinas:latest
    deploy:
      replicas: 4

  nreinas-app-canary:
    image: nreinas:canary
    deploy:
      replicas: 1 # Solo 20% del tráfico
```

### **Mejores Prácticas Implementadas**

#### **1. Seguridad**

```dockerfile
# Usuario no-root
RUN groupadd -r nreinas && useradd -r -g nreinas nreinas
USER nreinas

# Secrets management
ENV POSTGRES_PASSWORD_FILE=/run/secrets/postgres_password
```

#### **2. Observabilidad**

```dockerfile
# Labels para metadata
LABEL maintainer="devops@company.com"
LABEL version="1.0.0"
LABEL description="N-Reinas Distribuido"

# Health checks
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/status
```

#### **3. Performance**

```dockerfile
# Configuración JVM optimizada
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xmx1g"

# Layer caching eficiente
COPY gradle/ gradle/
RUN ./gradlew dependencies  # Cache de dependencias

COPY src/ src/
RUN ./gradlew shadowJar     # Solo si cambió el código
```

#### **4. Configuración Externa**

```yaml
# docker-compose.yml
services:
  nreinas-app:
    environment:
      - DATABASE_URL=${DATABASE_URL}
      - PROMETHEUS_ENDPOINT=${PROMETHEUS_ENDPOINT}
    volumes:
      - ./config:/app/config:ro
```

---

## 🚀 **Guía de Instalación y Pruebas**

### **Prerequisitos**

```bash
# Verificar versiones mínimas
docker --version          # >= 20.0
docker-compose --version  # >= 2.0
java --version            # >= 17
```

### **Opción 1: Despliegue Completo con Docker**

```bash
# 1. Clonar repositorio
git clone <repo-url>
cd marcuzo-nreinas-vertx

# 2. Desplegar stack completo
./deploy.sh

# 3. Verificar servicios
curl http://localhost:8080/status
curl http://localhost:9090/targets  # Prometheus
open http://localhost:3000          # Grafana (admin/admin123)
```

### **Opción 2: Desarrollo Local**

```bash
# 1. Construir aplicación
./gradlew clean shadowJar

# 2. Ejecutar localmente
./demo.sh

# 3. Probar endpoints
curl "http://localhost:8080/solve?n=4&workers=2"
curl "http://localhost:8080/metrics"
```

### **Ejemplos de Pruebas**

#### **1. Pruebas de Funcionalidad**

```bash
# Problema pequeño (4-Reinas)
curl "http://localhost:8080/solve?n=4&workers=2"
# Respuesta esperada: 2 soluciones

# Problema clásico (8-Reinas)
curl "http://localhost:8080/solve?n=8&workers=4"
# Respuesta esperada: 92 soluciones

# Problema grande (12-Reinas)
curl "http://localhost:8080/solve?n=12&workers=6"
# Respuesta esperada: 14,200 soluciones
```

#### **2. Pruebas de Performance**

```bash
# Test de carga con Apache Bench
ab -n 100 -c 10 "http://localhost:8080/solve?n=6&workers=3"

# Monitorear métricas durante la carga
watch curl -s http://localhost:8080/metrics | grep http_requests
```

#### **3. Pruebas de Resiliencia**

```bash
# Simular fallo de contenedor
docker-compose restart nreinas-app

# Verificar recuperación automática
while true; do curl -s http://localhost:8080/status; sleep 1; done
```

### **Verificación de Métricas**

#### **Prometheus Queries**

```promql
# Tasa de requests
rate(http_requests_total[5m])

# Tiempo de respuesta P95
histogram_quantile(0.95, rate(http_requests_duration_seconds_bucket[5m]))

# Uso de memoria JVM
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# Errores por minuto
increase(http_requests_errors_total[1m])
```

#### **Grafana Dashboards**

1. **Acceder**: http://localhost:3000 (admin/admin123)
2. **Importar**: Dashboards pre-configurados en `/monitoring/grafana/dashboards/`
3. **Explorar**: Métricas en tiempo real durante las pruebas

---

## 📖 **Estructura del Proyecto**

```
marcuzo-nreinas-vertx/
├── src/main/java/cl/marcuzo/nreinas/
│   ├── MainVerticle.java              # 🌐 API Gateway + Métricas
│   └── verticle/
│       ├── OrquestadorVerticle.java   # 🎭 Orchestrator Pattern
│       ├── WorkerVerticle.java        # ⚡ Worker Pattern + Algoritmo
│       └── StateManagerVerticle.java  # 💾 State Pattern + Cache
├── monitoring/                        # 📊 Stack de Monitoreo
│   ├── prometheus.yml                 # Configuración de scraping
│   ├── rules/nreinas_alerts.yml       # Reglas de alertas
│   ├── alertmanager.yml               # Configuración de alertas
│   └── grafana/
│       ├── dashboards/                # Dashboards pre-configurados
│       └── provisioning/              # Auto-configuración
├── docker-compose.yml                 # 🐳 Orquestación completa
├── Dockerfile                         # 📦 Imagen optimizada
├── deploy.sh                          # 🚀 Script de despliegue
├── demo.sh                            # 🎪 Demostración local
└── README.md                          # 📚 Este archivo
```

---

## 🎯 **Puntos Clave de Aprendizaje**

### **1. Vert.x en Acción**

- ✅ **Event Bus**: Comunicación desacoplada entre componentes
- ✅ **Asíncrono**: Manejo no-bloqueante de operaciones
- ✅ **Escalabilidad**: Múltiples workers coordinados
- ⚠️ **Complejidad**: Debugging y testing asíncrono

### **2. Patrones Distribuidos**

- ✅ **Gateway**: Punto único de entrada y control
- ✅ **Orchestrator**: Coordinación de flujos complejos
- ✅ **Worker**: Procesamiento distribuido y escalable
- ✅ **State Management**: Consistencia en sistemas distribuidos

### **3. Métricas Cruciales**

- ✅ **Observabilidad**: Visibilidad total del sistema
- ✅ **Alertas**: Detección proactiva de problemas
- ✅ **Dashboards**: Información accionable para diferentes roles
- ✅ **Performance**: Optimización basada en datos reales

### **4. Despliegues Modernos**

- ✅ **Containerización**: Consistencia y portabilidad
- ✅ **Orquestación**: Gestión simplificada de dependencias
- ✅ **Automatización**: Despliegues repetibles y confiables
- ✅ **Monitoring**: Visibilidad durante y después del despliegue

---

## 🎓 **Ejercicios Propuestos**

### **Nivel Básico**

1. **Modificar el algoritmo** para usar diferentes estrategias de backtracking
2. **Agregar nuevas métricas** específicas del algoritmo (profundidad de búsqueda, podas)
3. **Crear alertas personalizadas** para diferentes umbrales de performance

### **Nivel Intermedio**

1. **Implementar Circuit Breaker** para manejar fallos de workers
2. **Agregar persistencia** para guardar soluciones en base de datos
3. **Crear API REST completa** con CRUD de configuraciones

### **Nivel Avanzado**

1. **Implementar clustering real** con múltiples nodos físicos
2. **Agregar autenticación/autorización** con JWT
3. **Crear pipeline CI/CD** con testing automatizado y despliegue blue-green

---

## 🤝 **Contribuciones**

¡Las contribuciones son bienvenidas! Este proyecto está diseñado para ser educativo y evolutivo.

### **Áreas de Mejora**

- 🔄 **Algoritmos**: Implementar diferentes estrategias de resolución
- 📊 **Métricas**: Agregar más métricas de negocio y técnicas
- 🏗️ **Arquitectura**: Explorar otros patrones distribuidos
- 🐳 **DevOps**: Mejorar estrategias de despliegue y monitoring

---

## 📚 **Referencias y Lecturas Adicionales**

### **Vert.x**

- [Documentación Oficial de Vert.x](https://vertx.io/docs/)
- [Vert.x in Action - Book](https://www.manning.com/books/vertx-in-action)

### **Sistemas Distribuidos**

- [Designing Data-Intensive Applications](https://dataintensive.net/)
- [Building Microservices - Sam Newman](https://samnewman.io/books/building_microservices/)

### **Monitoreo y Observabilidad**

- [Site Reliability Engineering - Google](https://sre.google/books/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Best Practices](https://grafana.com/docs/grafana/latest/best-practices/)

### **Docker y Containerización**

- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [12-Factor App Methodology](https://12factor.net/)

---

**Desarrollado con ❤️ para propósitos educativos**

_Este proyecto demuestra conceptos reales utilizados en sistemas distribuidos de producción, adaptados para fines de aprendizaje y experimentación._
