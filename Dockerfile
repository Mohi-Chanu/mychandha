# syntax=docker/dockerfile:1.7@sha256:a57df69d0ea827fb7266491f2813635de6f17269be881f696fbfdf2d83dda33e
# docker/dockerfile:1.7, resolved 2026-07-25
FROM maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237 AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine@sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c
RUN apk upgrade --no-cache \
    && apk add --no-cache postgresql17-client \
    && addgroup -S -g 1000 rendersecrets \
    && addgroup -S mychandha \
    && adduser -S mychandha -G mychandha \
    && addgroup mychandha rendersecrets
WORKDIR /app
COPY --from=build /workspace/target/mychandha-platform-*.jar app.jar
COPY --chmod=0555 scripts/run-staging-bootstrap.sh /app/ops/run-bootstrap.sh
COPY --chmod=0555 scripts/run-staging-migration.sh /app/ops/run-migration.sh
COPY --chmod=0555 scripts/run-render-job-base-idle.sh /app/ops/run-idle.sh
COPY --chmod=0444 scripts/bootstrap-staging-database.sql /app/ops/bootstrap-staging-database.sql
COPY --chmod=0444 scripts/bootstrap-database-roles.sql /app/ops/bootstrap-database-roles.sql
USER mychandha
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
