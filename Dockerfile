# Usa la imagen base de OpenJDK
FROM openjdk:17-alpine

# Establece variables de entorno para el truststore
ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=/app/resources/keystore.jks -Djavax.net.ssl.trustStorePassword=GalileoTraccar"

# Crea un directorio para la aplicación
WORKDIR /app

COPY ./keystore.jks /app/resources/keystore.jks

# Crear un volumen temporal
#VOLUME /tmp

# Copiar el archivo JAR de la aplicación
COPY ./servicio-apis.jar /app/servicio-apis.jar

# Usa JAVA_OPTS en el comando de inicio para asegurarse de que se aplica al proceso
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/servicio-apis.jar"]