# Multi-stage build for dashflow-api (Spring Boot fat jar).
# Build from repo root:
#   docker build -f dashflow-api/Dockerfile -t dashflow/api:local .

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY dashflow-spi/pom.xml dashflow-spi/pom.xml
COPY dashflow-broker/pom.xml dashflow-broker/pom.xml
COPY dashflow-api/pom.xml dashflow-api/pom.xml

RUN chmod +x mvnw \
  && ./mvnw -pl dashflow-api -am dependency:go-offline -B -q || true

COPY dashflow-spi dashflow-spi
COPY dashflow-broker dashflow-broker
COPY dashflow-api dashflow-api

RUN ./mvnw -pl dashflow-api -am package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S dashflow && adduser -S dashflow -G dashflow
USER dashflow

COPY --from=build /workspace/dashflow-api/target/dashflow-api-*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
