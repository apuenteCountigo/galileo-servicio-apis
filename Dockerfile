# Usa la imagen base de OpenJDK
FROM openjdk:17-alpine

# Configura JAVA_OPTS para que Java use el almacén de certificados de la JVM
ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=$JAVA_HOME/lib/security/cacerts -Djavax.net.ssl.trustStorePassword=changeit"

# Copia el certificado descargado al contenedor
COPY traccar-galileo.guardiacivil.es.pem /usr/local/share/ca-certificates/traccar-galileo.guardiacivil.es.pem

# Agregar el certificado al almacén de certificados de la JVM
RUN keytool -importcert -noprompt -trustcacerts -alias traccar-cert \
    -file /usr/local/share/ca-certificates/traccar-galileo.guardiacivil.es.pem \
    -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit

# Crear un volumen temporal
VOLUME /tmp

# Copiar el archivo JAR de la aplicación
COPY ./servicio-apis.jar /servicio-apis.jar

# Usa JAVA_OPTS en el comando de inicio para asegurarse de que se aplica al proceso
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /servicio-apis.jar"]