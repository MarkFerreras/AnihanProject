# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Cache Gradle wrapper + dependencies in their own layer before copying source,
# so editing application code doesn't force a full re-download on every build.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:25-jre AS run
WORKDIR /app

RUN useradd --system --uid 1001 spring
COPY --from=build /app/build/libs/*.jar app.jar
USER spring

# Render assigns the listen port via $PORT at runtime; application.properties
# reads it with a local-dev fallback of 8080.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
