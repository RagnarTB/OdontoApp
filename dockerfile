# ---- Etapa 1: Construcción con Maven ----
FROM maven:3.9-eclipse-temurin-21 AS builder

# Establecer directorio de trabajo
WORKDIR /build

# Copiar solo el pom.xml para descargar dependencias eficientemente
COPY pom.xml .
# Descargar dependencias (esto se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar el resto del código fuente
COPY src ./src

# Compilar y empaquetar la aplicación, omitiendo tests
RUN mvn package -DskipTests

# ---- Etapa 2: Imagen Final ----
FROM eclipse-temurin:21-jdk-slim

WORKDIR /app

# Copiar el JAR construido desde la etapa 'builder'
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]