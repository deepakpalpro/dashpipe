# Multi-stage build for dashpipe-api (Spring Boot fat jar).
# Build from repo root:
#   docker build -f dashpipe-api/Dockerfile -t dashpipe/api:local .

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY dashpipe-spi/pom.xml dashpipe-spi/pom.xml
COPY dashpipe-broker/pom.xml dashpipe-broker/pom.xml
COPY dashpipe-api/pom.xml dashpipe-api/pom.xml

RUN chmod +x mvnw \
  && ./mvnw -pl dashpipe-api -am dependency:go-offline -B -q || true

COPY dashpipe-spi dashpipe-spi
COPY dashpipe-broker dashpipe-broker
COPY dashpipe-api dashpipe-api

RUN ./mvnw -pl dashpipe-api -am package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S dashpipe && adduser -S dashpipe -G dashpipe
USER dashpipe

COPY --from=build /workspace/dashpipe-api/target/dashpipe-api-*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
