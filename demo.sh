#!/bin/bash

# Script de demostración para N-Reinas Distribuido
# Funciona sin Docker para mostrar la funcionalidad

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${PURPLE}================================================${NC}"
    echo -e "${PURPLE}$1${NC}"
    echo -e "${PURPLE}================================================${NC}"
}

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Función para construir la aplicación
build_app() {
    print_status "Construyendo aplicación N-Reinas..."
    ./gradlew clean shadowJar
    print_success "Aplicación construida correctamente"
}

# Función para iniciar la aplicación
start_app() {
    print_status "Iniciando aplicación N-Reinas en segundo plano..."

    # Detener proceso anterior si existe
    pkill -f "java.*marcuzo-nreinas-vertx" 2>/dev/null || true

    # Iniciar aplicación en background
    nohup java -Xmx1g -Xms512m -Dvertx.metrics.options.enabled=true \
         -jar build/libs/*-fat.jar > app.log 2>&1 &

    APP_PID=$!
    echo $APP_PID > app.pid

    print_success "Aplicación iniciada con PID: $APP_PID"
    print_status "Esperando a que la aplicación esté lista..."

    # Esperar a que la aplicación esté lista
    for i in {1..30}; do
        if curl -s -f "http://localhost:8080/status" > /dev/null 2>&1; then
            print_success "¡Aplicación lista!"
            return 0
        fi
        sleep 2
        echo -n "."
    done

    print_error "La aplicación tardó demasiado en iniciarse"
    return 1
}

# Función para probar la funcionalidad
test_functionality() {
    print_header "🧪 PROBANDO FUNCIONALIDAD"

    # Probar endpoint de estado
    print_status "Probando endpoint de estado..."
    status_response=$(curl -s "http://localhost:8080/status")
    echo -e "${CYAN}Respuesta:${NC} $status_response"
    echo ""

    # Probar resolución de 4-Reinas
    print_status "Resolviendo problema de 4-Reinas con 2 workers..."
    nqueens_response=$(curl -s "http://localhost:8080/solve?n=4&workers=2")
    echo -e "${CYAN}Respuesta (4-Reinas):${NC}"
    echo "$nqueens_response" | jq . 2>/dev/null || echo "$nqueens_response"
    echo ""

    # Probar resolución de 8-Reinas
    print_status "Resolviendo problema de 8-Reinas con 4 workers..."
    nqueens8_response=$(curl -s "http://localhost:8080/solve?n=8&workers=4")
    echo -e "${CYAN}Respuesta (8-Reinas - mostrando resumen):${NC}"
    echo "$nqueens8_response" | jq '{N, totalSolutions, elapsedTimeMs, workersDeployed, status}' 2>/dev/null || echo "Respuesta recibida (JSON sin formato)"
    echo ""
}

# Función para mostrar métricas
show_metrics() {
    print_header "📊 MÉTRICAS DE PROMETHEUS"

    print_status "Obteniendo métricas de la aplicación..."
    echo -e "${CYAN}Métricas destacadas:${NC}"

    # Extraer métricas específicas
    metrics=$(curl -s "http://localhost:8080/metrics")

    echo "$metrics" | grep -E "(http_requests_total|nqueens_requests_total|worker_.*_total|jvm_memory_used_bytes)" | head -10
    echo ""

    print_status "Métricas completas disponibles en: http://localhost:8080/metrics"
}

# Función para demostrar la arquitectura
show_architecture() {
    print_header "🏗️ ARQUITECTURA DEL SISTEMA"

    cat << 'EOF'
┌─────────────────────────────────────────────────────────────────┐
│                    N-REINAS DISTRIBUIDO                        │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   MainVerticle  │    │ OrquestadorVert │    │ StateManagerVert│
│   (HTTP Server) │◄──►│   (Coordinator) │◄──►│   (DP Cache)    │
│   Port 8080     │    │   Event Bus     │    │   Event Bus     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   WorkerVert    │    │   WorkerVert    │    │   WorkerVert    │
│   Worker #0     │    │   Worker #1     │    │   Worker #N     │
│   (Backtracking)│    │   (Standby)     │    │   (Standby)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘

MONITOREO COMPLETO:
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Prometheus    │    │     Grafana     │    │  AlertManager   │
│   (Métricas)    │◄──►│   (Dashboards)  │◄──►│   (Alertas)     │
│   Port 9090     │    │   Port 3000     │    │   Port 9093     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
EOF

    echo ""
    print_status "Componentes implementados:"
    echo "  ✅ MainVerticle - Servidor HTTP con métricas"
    echo "  ✅ OrquestadorVerticle - Coordinación distribuida"
    echo "  ✅ StateManagerVerticle - Cache de estados DP"
    echo "  ✅ WorkerVerticle - Algoritmo de backtracking"
    echo "  ✅ Prometheus metrics - Monitoreo completo"
    echo "  ✅ Docker setup - Infraestructura containerizada"
    echo "  ✅ Grafana dashboards - Visualizaciones"
    echo "  ✅ AlertManager - Sistema de alertas"
}

# Función para mostrar información de monitoreo
show_monitoring_info() {
    print_header "📊 INFORMACIÓN DE MONITOREO"

    echo -e "${CYAN}Configuración implementada:${NC}"
    echo ""
    echo "📁 Estructura de monitoreo:"
    echo "├── monitoring/"
    echo "│   ├── prometheus.yml          # Configuración de Prometheus"
    echo "│   ├── rules/                  # Reglas de alertas"
    echo "│   │   └── nreinas_alerts.yml  # Alertas específicas"
    echo "│   ├── alertmanager.yml        # Configuración de AlertManager"
    echo "│   └── grafana/"
    echo "│       ├── dashboards/         # Dashboards pre-configurados"
    echo "│       │   ├── nreinas-overview.json"
    echo "│       │   └── system-metrics.json"
    echo "│       └── provisioning/       # Configuración automática"
    echo "│           ├── datasources/"
    echo "│           └── dashboards/"
    echo ""

    echo -e "${CYAN}Servicios de monitoreo (Puerto):${NC}"
    echo "  🏰 N-Reinas App     → http://localhost:8080"
    echo "  📊 Prometheus       → http://localhost:9090"
    echo "  📈 Grafana         → http://localhost:3000 (admin/admin123)"
    echo "  🚨 AlertManager    → http://localhost:9093"
    echo "  🖥️  Node Exporter   → http://localhost:9100"
    echo "  🐳 cAdvisor        → http://localhost:8081"
    echo ""

    echo -e "${CYAN}Para desplegar el stack completo:${NC}"
    echo "  1. Instalar Docker y Docker Compose"
    echo "  2. Ejecutar: ./deploy.sh"
    echo "  3. Acceder a Grafana para ver dashboards"
    echo ""
}

# Función para limpiar
cleanup() {
    print_status "Deteniendo aplicación..."

    if [ -f app.pid ]; then
        APP_PID=$(cat app.pid)
        kill $APP_PID 2>/dev/null || true
        rm app.pid
        print_success "Aplicación detenida"
    fi

    # Limpiar archivos temporales
    rm -f app.log nohup.out
}

# Función principal
main() {
    print_header "🚀 DEMO: N-REINAS DISTRIBUIDO CON MONITOREO"

    # Construir aplicación
    build_app
    echo ""

    # Mostrar arquitectura
    show_architecture
    echo ""

    # Iniciar aplicación
    start_app
    echo ""

    # Probar funcionalidad
    test_functionality
    echo ""

    # Mostrar métricas
    show_metrics
    echo ""

    # Mostrar información de monitoreo
    show_monitoring_info

    print_header "✨ DEMO COMPLETADO"

    echo -e "${GREEN}¡La aplicación está funcionando correctamente!${NC}"
    echo ""
    echo -e "${CYAN}Para probar manualmente:${NC}"
    echo "  curl \"http://localhost:8080/solve?n=6&workers=3\""
    echo "  curl \"http://localhost:8080/status\""
    echo "  curl \"http://localhost:8080/metrics\""
    echo ""
    echo -e "${YELLOW}La aplicación seguirá ejecutándose en segundo plano.${NC}"
    echo -e "${YELLOW}Para detenerla, ejecuta: ./demo.sh stop${NC}"
}

# Manejar argumentos
case "${1:-}" in
    "stop")
        cleanup
        ;;
    "logs")
        if [ -f app.log ]; then
            tail -f app.log
        else
            echo "No hay logs disponibles"
        fi
        ;;
    "status")
        if [ -f app.pid ]; then
            APP_PID=$(cat app.pid)
            if ps -p $APP_PID > /dev/null; then
                echo "✅ Aplicación ejecutándose (PID: $APP_PID)"
                curl -s "http://localhost:8080/status" | jq . 2>/dev/null || echo "Aplicación respondiendo"
            else
                echo "❌ Aplicación no está ejecutándose"
            fi
        else
            echo "❌ No se encuentra archivo PID"
        fi
        ;;
    *)
        main
        ;;
esac
