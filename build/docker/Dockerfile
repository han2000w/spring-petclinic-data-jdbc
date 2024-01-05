FROM openjdk:17-jdk-slim
ARG UID=999
ADD build/libs/*SNAPSHOT.jar app.jar
RUN useradd -u $UID han2000w
RUN mkdir /logs && chown han2000w.han2000w /logs
EXPOSE 8080
USER han2000w
ENTRYPOINT ["sh", "-c", "java -Dlogging.file.name=/logs/spring-petclinic.log -Djava.security.egd=file:/dev/./urandom -server -jar app.jar"]
