# Usa la imagen base de OpenJDK
FROM openjdk:17-alpine

# Configura la ubicación del keystore y copia el archivo en el contenedor
ENV KEYSTORE_PATH="/usr/local/lib/security/keystore.jks"
COPY ./keystore.jks ${KEYSTORE_PATH}

# Configura JAVA_OPTS para que Java use el almacén de certificados de la JVM
ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=${KEYSTORE_PATH} -Djavax.net.ssl.trustStorePassword=GalileoTraccar"

# Crear un volumen temporal
VOLUME /tmp

# Copiar el archivo JAR de la aplicación
COPY ./servicio-apis.jar /servicio-apis.jar

# Usa JAVA_OPTS en el comando de inicio para asegurarse de que se aplica al proceso
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /servicio-apis.jar"]