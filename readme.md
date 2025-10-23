docker-compose down -v          borra registros existentes y reinicia la base de datos

mvn clean package -DskipTests            borra registros existentes y reinicia el jar

docker-compose up --build            contruye la orquestacion para ejecutar la aplicacion levantando backend y front y servidor




entrar a la db: docker exec -it odontoapp-mysql mysql -u root -p
 contraseña: leonardo
luego:
 SHOW DATABASES;
USE odontoapp_db;
SHOW TABLES;
