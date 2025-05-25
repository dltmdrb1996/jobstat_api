FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE=build/libs/jobstat-config-server-0.0.1-SNAPSHOT-boot.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]