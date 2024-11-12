FROM openjdk:17-alpine
# Copia el certificado descargado al contenedor
COPY traccar-galileo.guardiacivil.es.pem /usr/local/share/ca-certificates/traccar-galileo.guardiacivil.es.pem
# Agregar el certificado al almacén de certificados de la JVM
RUN keytool -importcert -noprompt -trustcacerts -alias traccar-cert \
    -file /usr/local/share/ca-certificates/traccar-galileo.guardiacivil.es.pem \
    -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit
VOLUME /tmp
ADD ./servicio-apis.jar servicio-apis.jar
ENTRYPOINT ["java","-jar","/servicio-apis.jar"]
