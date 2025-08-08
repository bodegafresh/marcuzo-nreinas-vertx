# Dockerfile para N-Reinas Distribuido
FROM openjdk:17-jdk-slim

# Metadatos
LABEL maintainer="marcuzo"
LABEL description="Solucionador Distribuido de N-Reinas con Vert.x"
LABEL version="1.0"

# Instalar herramientas necesarias
RUN apt-get update && \
    apt-get install -y curl procps && \
    rm -rf /var/lib/apt/lists/*

# Crear directorio de trabajo
WORKDIR /app

# Copiar archivos de construcción
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew

# Descargar dependencias (cache layer)
RUN ./gradlew dependencies --no-daemon

# Copiar código fuente
COPY src/ src/
COPY prometheus.yml ./

# Construir aplicación
RUN ./gradlew shadowJar --no-daemon

# Exponer puertos
EXPOSE 8080

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV VERTX_OPTS="-Dvertx.metrics.options.enabled=true"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/status || exit 1

# Comando por defecto
CMD ["sh", "-c", "java $JAVA_OPTS $VERTX_OPTS -jar build/libs/*-fat.jar"]
