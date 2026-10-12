# Render has no native Java runtime, so the API is deployed as a Docker image.
# Stage 1 builds the jar with Maven, stage 2 runs it on a small JRE image.

# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Download dependencies first so they are cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Run stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/campusconnect-backend.jar app.jar

# Keep memory usage inside Render's free 512 MB instance
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"

# Render provides PORT at runtime; application.properties reads it (server.port=${PORT:8080})
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
