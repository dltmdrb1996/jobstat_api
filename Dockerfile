FROM eclipse-temurin:24-jre-alpine

ARG JAR_FILE=build/libs/jobstat-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# JVM 옵션 추가 (메모리 설정 및 G1GC 적용)
ENV JAVA_OPTS="-Xms1G -Xmx1G -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:MaxMetaspaceSize=256m -XX:+HeapDumpOnOutOfMemoryError"
#ENV JAVA_OPTS="-Xms1G -Xmx1224m -XX:+ZGenerational -XX:MaxMetaspaceSize=256m -XX:MaxGCPauseMillis=200 -XX:+ZProactive"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
