FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies

COPY src src
RUN ./gradlew --no-daemon -x test bootJar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
