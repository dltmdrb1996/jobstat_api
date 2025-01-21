FROM azul/zulu-openjdk:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]