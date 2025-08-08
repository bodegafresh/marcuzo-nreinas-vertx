# 🏰 N-Reinas Distribuido con Monitoreo Completo

Sistema distribuido para resolver el problema de las N-Reinas usando **Vert.x**, **programación dinámica** y **monitoreo completo** con **Prometheus** y **Grafana**.

## 🚀 Características

- ✅ **Aplicación Distribuida**: Vert.x con Event Bus y workers paralelos
- ✅ **Algoritmo Optimizado**: Backtracking con programación dinámica
- ✅ **Monitoreo Completo**: Prometheus + Grafana + AlertManager
- ✅ **Métricas Detalladas**: JVM, sistema, aplicación y contenedores
- ✅ **Dashboards Visuales**: Grafana con dashboards pre-configurados
- ✅ **Alertas Inteligentes**: AlertManager con reglas personalizadas
- ✅ **Containerización**: Docker + Docker Compose
- ✅ **Despliegue Automático**: Script de despliegue automatizado

## 🏗️ Arquitectura del Sistema

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   N-Reinas App  │    │   Prometheus    │    │     Grafana     │
│   (Port 8080)   │◄──►│   (Port 9090)   │◄──►│   (Port 3000)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  AlertManager   │    │  Node Exporter  │    │    cAdvisor     │
│   (Port 9093)   │    │   (Port 9100)   │    │   (Port 8081)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📦 Servicios Incluidos

| Servicio          | Puerto | Descripción                              |
| ----------------- | ------ | ---------------------------------------- |
| **N-Reinas App**  | 8080   | Aplicación principal con API REST        |
| **Prometheus**    | 9090   | Recolección y almacenamiento de métricas |
| **Grafana**       | 3000   | Dashboards y visualizaciones             |
| **AlertManager**  | 9093   | Gestión de alertas                       |
| **Node Exporter** | 9100   | Métricas del sistema host                |
| **cAdvisor**      | 8081   | Métricas de contenedores Docker          |

## 🚀 Despliegue Rápido

### Opción 1: Script Automático (Recomendado)

```bash
# Desplegar todo el stack
./deploy.sh

# Ver estado de servicios
./deploy.sh status

# Ver logs de un servicio
./deploy.sh logs nreinas-app

# Reiniciar servicio
./deploy.sh restart grafana

# Detener todo
./deploy.sh stop
```

### Opción 2: Manual con Docker Compose

```bash
# Construir la aplicación
./gradlew clean shadowJar

# Iniciar servicios
docker-compose up -d

# Ver logs
docker-compose logs -f nreinas-app

# Detener servicios
docker-compose down
```

## 🔧 Configuración Inicial

### Prerequisitos

- **Docker** >= 20.0
- **Docker Compose** >= 2.0
- **Java** >= 17 (para desarrollo)
- **Gradle** >= 7.0 (incluido con gradlew)

### Variables de Entorno (Opcionales)

```bash
# Configuración de memoria JVM
export JAVA_OPTS="-Xmx1g -Xms512m"

# Configuración de Vert.x
export VERTX_OPTS="-Dvertx.metrics.options.enabled=true"
```

## 📊 Acceso a los Servicios

### 🏰 Aplicación N-Reinas

- **URL Principal**: http://localhost:8080
- **API Resolver**: http://localhost:8080/solve?n=8&workers=4
- **Estado**: http://localhost:8080/status
- **Métricas**: http://localhost:8080/metrics

#### Ejemplos de Uso de la API

```bash
# Resolver 8-Reinas con 4 workers
curl "http://localhost:8080/solve?n=8&workers=4"

# Resolver 4-Reinas con 2 workers
curl "http://localhost:8080/solve?n=4&workers=2"

# Verificar estado del sistema
curl "http://localhost:8080/status"
```

### 📈 Grafana (Dashboards)

- **URL**: http://localhost:3000
- **Usuario**: `admin`
- **Contraseña**: `admin123`

#### Dashboards Disponibles

1. **N-Reinas Overview**: Métricas principales de la aplicación
2. **Sistema y Contenedores**: Métricas de infraestructura

### 📊 Prometheus (Métricas)

- **URL**: http://localhost:9090
- **Targets**: http://localhost:9090/targets
- **Alertas**: http://localhost:9090/alerts

### 🚨 AlertManager (Alertas)

- **URL**: http://localhost:9093
- **Configuración**: `monitoring/alertmanager.yml`

## 📈 Métricas Disponibles

### Métricas de Aplicación

- `http_requests_total`: Total de peticiones HTTP
- `http_requests_errors_total`: Errores en peticiones HTTP
- `http_requests_duration_seconds`: Duración de peticiones HTTP
- `nqueens_requests_total`: Solicitudes de N-Reinas
- `worker_executions_total`: Ejecuciones de workers
- `worker_solutions_found_total`: Soluciones encontradas
- `worker_states_processed_total`: Estados procesados
- `worker_errors_total`: Errores en workers
- `worker_execution_time_seconds`: Tiempo de ejecución de workers

### Métricas JVM

- `jvm_memory_used_bytes`: Memoria JVM utilizada
- `jvm_memory_max_bytes`: Memoria JVM máxima
- `process_cpu_seconds_total`: CPU del proceso

### Métricas del Sistema

- `node_cpu_seconds_total`: CPU del sistema
- `node_memory_*`: Métricas de memoria
- `node_filesystem_*`: Métricas de almacenamiento
- `node_network_*`: Métricas de red

### Métricas de Contenedores

- `container_cpu_usage_seconds_total`: CPU por contenedor
- `container_memory_usage_bytes`: Memoria por contenedor

## 🚨 Alertas Configuradas

### Alertas de Aplicación

- **Aplicación Caída**: Cuando la app no responde
- **Alto Uso de Memoria JVM**: >80% de memoria heap
- **Alta Tasa de Errores**: Muchos errores en workers
- **Tiempo de Respuesta Lento**: >10 segundos P95

### Alertas de Sistema

- **Alto Uso de CPU**: >80% CPU del sistema
- **Alto Uso de Memoria**: >85% memoria del sistema
- **Poco Espacio en Disco**: >90% uso de disco
- **Alto Uso de CPU en Contenedores**: >80% CPU por contenedor

## 🔍 Debugging y Troubleshooting

### Ver Logs

```bash
# Logs de la aplicación
docker-compose logs -f nreinas-app

# Logs de todos los servicios
docker-compose logs -f

# Logs de Prometheus
docker-compose logs -f prometheus

# Logs de Grafana
docker-compose logs -f grafana
```

### Verificar Métricas

```bash
# Verificar métricas de la aplicación
curl http://localhost:8080/metrics

# Verificar targets de Prometheus
curl http://localhost:9090/api/v1/targets

# Verificar estado de Prometheus
curl http://localhost:9090/api/v1/query?query=up
```

### Problemas Comunes

#### La aplicación no inicia

1. Verificar que no haya otros servicios en los puertos
2. Verificar logs: `docker-compose logs nreinas-app`
3. Verificar espacio en disco y memoria disponible

#### Prometheus no recolecta métricas

1. Verificar configuración: `monitoring/prometheus.yml`
2. Verificar targets: http://localhost:9090/targets
3. Verificar conectividad de red entre contenedores

#### Grafana no muestra datos

1. Verificar que Prometheus esté funcionando
2. Verificar datasource en Grafana
3. Verificar consultas en los dashboards

## 🛠️ Personalización

### Modificar Configuración de Prometheus

Editar `monitoring/prometheus.yml` para:

- Cambiar intervalos de scraping
- Agregar nuevos targets
- Modificar reglas de alertas

### Crear Dashboards Personalizados

1. Acceder a Grafana: http://localhost:3000
2. Crear nuevo dashboard
3. Agregar paneles con consultas PromQL
4. Exportar configuración JSON

### Modificar Alertas

Editar `monitoring/rules/nreinas_alerts.yml` para:

- Agregar nuevas reglas de alertas
- Modificar umbrales existentes
- Cambiar configuración de AlertManager

## 📚 Queries PromQL Útiles

### Rendimiento de la Aplicación

```promql
# Tasa de peticiones por segundo
rate(http_requests_total[5m])

# Latencia P95
histogram_quantile(0.95, rate(http_requests_duration_seconds_bucket[5m]))

# Tasa de errores
rate(http_requests_errors_total[5m]) / rate(http_requests_total[5m])
```

### Métricas de Workers

```promql
# Soluciones encontradas por segundo
rate(worker_solutions_found_total[5m])

# Estados procesados por segundo
rate(worker_states_processed_total[5m])

# Tiempo promedio de ejecución
avg(worker_execution_time_seconds)
```

### Métricas del Sistema

```promql
# Uso de CPU
100 - (avg(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# Uso de memoria
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# Uso de disco
(1 - (node_filesystem_avail_bytes / node_filesystem_size_bytes)) * 100
```

## 🤝 Contribuir

1. Fork el repositorio
2. Crear rama de feature (`git checkout -b feature/nueva-metrica`)
3. Commit cambios (`git commit -am 'Agregar nueva métrica'`)
4. Push a la rama (`git push origin feature/nueva-metrica`)
5. Crear Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👥 Autores

- **Marcuzo** - Desarrollo inicial y arquitectura de monitoreo

## 🙏 Agradecimientos

- Equipo de Vert.x por el excelente framework
- Comunidad de Prometheus y Grafana
- Todos los contribuidores del proyecto
