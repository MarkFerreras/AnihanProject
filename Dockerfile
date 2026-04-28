# Build stage
FROM gradle:8.12.1-jdk25-corretto AS build
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

# Run stage
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]