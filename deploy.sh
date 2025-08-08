#!/bin/bash

# Script de despliegue para N-Reinas Distribuido con monitoreo

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir mensajes coloreados
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

# Función para verificar dependencias
check_dependencies() {
    print_status "Verificando dependencias..."

    if ! command -v docker &> /dev/null; then
        print_error "Docker no está instalado"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose no está instalado"
        exit 1
    fi

    print_success "Dependencias verificadas"
}

# Función para limpiar contenedores anteriores
cleanup() {
    print_status "Limpiando contenedores anteriores..."
    docker-compose down --remove-orphans -v 2>/dev/null || true
    docker system prune -f 2>/dev/null || true
    print_success "Limpieza completada"
}

# Función para construir la aplicación
build_app() {
    print_status "Construyendo aplicación N-Reinas..."
    ./gradlew clean shadowJar
    print_success "Aplicación construida"
}

# Función para iniciar servicios
start_services() {
    print_status "Iniciando servicios de monitoreo..."

    # Iniciar en orden: infraestructura primero, luego aplicación
    docker-compose up -d prometheus grafana alertmanager node-exporter cadvisor

    print_status "Esperando a que los servicios de infraestructura estén listos..."
    sleep 10

    print_status "Iniciando aplicación N-Reinas..."
    docker-compose up -d nreinas-app

    print_success "Servicios iniciados"
}

# Función para verificar el estado de los servicios
check_services() {
    print_status "Verificando estado de los servicios..."

    # Esperar a que todos los servicios estén listos
    sleep 20

    # Verificar cada servicio
    services=("nreinas-app:8080" "prometheus:9090" "grafana:3000" "alertmanager:9093")

    for service in "${services[@]}"; do
        IFS=':' read -r name port <<< "$service"
        if curl -s -f "http://localhost:$port" > /dev/null 2>&1; then
            print_success "$name está funcionando en puerto $port"
        else
            print_warning "$name puede no estar completamente listo en puerto $port"
        fi
    done
}

# Función para mostrar información de acceso
show_access_info() {
    print_success "¡Despliegue completado!"
    echo ""
    echo "🏰 N-Reinas Distribuido:"
    echo "   URL: http://localhost:8080"
    echo "   API: http://localhost:8080/solve?n=8&workers=4"
    echo "   Estado: http://localhost:8080/status"
    echo ""
    echo "📊 Prometheus (Métricas):"
    echo "   URL: http://localhost:9090"
    echo "   Targets: http://localhost:9090/targets"
    echo ""
    echo "📈 Grafana (Dashboards):"
    echo "   URL: http://localhost:3000"
    echo "   Usuario: admin"
    echo "   Contraseña: admin123"
    echo ""
    echo "🚨 AlertManager (Alertas):"
    echo "   URL: http://localhost:9093"
    echo ""
    echo "🔧 Métricas del Sistema:"
    echo "   Node Exporter: http://localhost:9100"
    echo "   cAdvisor: http://localhost:8081"
    echo ""
    echo "📋 Comandos útiles:"
    echo "   Ver logs: docker-compose logs -f [servicio]"
    echo "   Detener: docker-compose down"
    echo "   Reiniciar: docker-compose restart [servicio]"
    echo ""
}

# Función para probar la aplicación
test_application() {
    print_status "Probando la aplicación..."

    # Esperar un poco más para asegurar que todo esté listo
    sleep 10

    # Probar endpoint de estado
    if curl -s -f "http://localhost:8080/status" > /dev/null; then
        print_success "Endpoint de estado funcionando"
    else
        print_warning "Endpoint de estado no responde aún"
    fi

    # Probar resolución de N-Reinas pequeño
    print_status "Probando resolución de 4-Reinas..."
    response=$(curl -s "http://localhost:8080/solve?n=4&workers=2" || echo "error")

    if [[ $response == *"totalSolutions"* ]]; then
        print_success "Prueba de N-Reinas exitosa"
    else
        print_warning "La aplicación puede necesitar más tiempo para estar lista"
    fi
}

# Función principal
main() {
    echo "🚀 Desplegando N-Reinas Distribuido con Monitoreo"
    echo "=================================================="

    check_dependencies
    cleanup
    build_app
    start_services
    check_services
    test_application
    show_access_info

    print_success "¡Todo listo! 🎉"
}

# Manejar argumentos de línea de comandos
case "${1:-}" in
    "cleanup")
        cleanup
        ;;
    "status")
        docker-compose ps
        ;;
    "logs")
        docker-compose logs -f "${2:-}"
        ;;
    "restart")
        docker-compose restart "${2:-}"
        ;;
    "stop")
        docker-compose down
        ;;
    *)
        main
        ;;
esac
