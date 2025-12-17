# Build Stage
FROM gradle:jdk21-jammy AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Run Stage
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
