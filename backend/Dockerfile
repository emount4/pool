FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY frontend .
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src src
COPY --from=frontend /frontend/dist src/main/resources/static
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/pool-backend-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
