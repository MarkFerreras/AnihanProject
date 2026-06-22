# ============================================================
# Dockerfile — Anihan SRMS (Spring Boot, Java 25, Gradle)
# Used by Render (runtime: docker) to build and run the full app
# (back-end + served frontend) as one container. See DEPLOYMENT.md.
# ============================================================

# ---- Stage 1: build the executable jar ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy the Gradle wrapper + build scripts first for better layer caching
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x ./gradlew

# Copy sources and build (skip tests — they need a DB; build is verified in CI/locally)
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# ---- Stage 2: slim runtime image ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/springboot-0.0.1-SNAPSHOT.jar app.jar

# Render injects $PORT; application.properties already reads ${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
