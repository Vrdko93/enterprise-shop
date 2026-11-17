# Stage 1: Build using Maven and JDK 21
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy the multi-module Maven project
COPY pom.xml .
COPY libs/ libs/
COPY services/ services/

# Build User Service and its shared dependencies
RUN mvn -B -pl services/user-service -am \
    package -DskipTests

# Stage 2: Run using Java 21 JRE
FROM eclipse-temurin:21-jre

WORKDIR /app

# Run as an unprivileged user
USER 10001:10001

# Copy only the executable application JAR
COPY --from=builder --chown=10001:10001 \
    /app/services/user-service/target/user-service-0.1.0.jar \
    app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]