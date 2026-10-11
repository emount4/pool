# Build-only container. JavaFX runs on the host, never inside Docker.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B clean verify dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib
FROM scratch AS artifacts
COPY --from=build /build/target/pool-information-system-2.0.0.jar /
COPY --from=build /build/target/lib /lib
