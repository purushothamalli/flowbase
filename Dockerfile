# Multi-stage build Dockerfile for FlowBase
# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the packaged executable JAR
COPY --from=build /app/target/engine-0.0.1-SNAPSHOT.jar app.jar

# Create a non-root group and user for security compliance
RUN groupadd -r flowbase && useradd -r -g flowbase flowbase
USER flowbase

# Expose standard port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
