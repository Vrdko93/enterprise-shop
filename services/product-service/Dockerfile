FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY libs/ libs/
COPY services/ services/

RUN --mount=type=cache,id=enterprise-shop-maven,target=/root/.m2/repository \
    mvn -B -pl services/product-service -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder --chown=10001:10001 \
    /app/services/product-service/target/product-service-0.1.0.jar app.jar

USER 10001:10001

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]