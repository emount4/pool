# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package dependency:copy-dependencies \
    -DoutputDirectory=target/dependency

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/target/pool-information-system-1.0-SNAPSHOT.jar app.jar
COPY --from=build /workspace/target/dependency ./lib

RUN mkdir -p /app/exports

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-cp", "app.jar:lib/*", "ru.mirea.pool.Main"]
