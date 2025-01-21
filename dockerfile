# 멀티 스테이지 빌드를 위한 빌더 이미지
FROM openjdk:21-slim as builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

# 실행 이미지
FROM openjdk:21-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]