# Usa una imagen base de OpenJDK 21
FROM openjdk:21-jdk-slim

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo .jar compilado de tu proyecto al contenedor
COPY target/*.jar app.jar

# Expone el puerto en el que se ejecutará la aplicación
EXPOSE 8080

# El comando para ejecutar la aplicación cuando se inicie el contenedor
ENTRYPOINT ["java","-jar","app.jar"]
