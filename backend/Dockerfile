FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY maven-settings-docker.xml /root/.m2/settings.xml
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -s /root/.m2/settings.xml \
    -Dmaven.wagon.http.retryHandler.count=5 \
    dependency:go-offline

COPY src ./src
RUN cp src/main/resources/application.example.yml src/main/resources/application.yml
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -s /root/.m2/settings.xml \
    -Dmaven.wagon.http.retryHandler.count=5 \
    clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends mosquitto \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/aiot-log-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
