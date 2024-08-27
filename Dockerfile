FROM openjdk:17-alpine
VOLUME /tmp
ADD ./servicio-apis.jar servicio-apis.jar
ENTRYPOINT ["java","-jar","/servicio-apis.jar"]
