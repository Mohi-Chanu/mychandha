# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk upgrade --no-cache \
    && addgroup -S mychandha \
    && adduser -S mychandha -G mychandha
WORKDIR /app
COPY --from=build /workspace/target/mychandha-platform-*.jar app.jar
USER mychandha
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
