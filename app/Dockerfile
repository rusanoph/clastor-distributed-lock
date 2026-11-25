FROM eclipse-temurin:21-jdk@sha256:8ad118b146aa92da815b576441acd6b42b38236797ad0b6edc8c200b85bb0734 AS build
WORKDIR /src
COPY gradlew gradle.properties settings.gradle.kts settings-gradle.lockfile build.gradle.kts LICENSE ./
COPY gradle/ gradle/
COPY config/ config/
COPY domain/ domain/
COPY adapter/ adapter/
COPY app/ app/
RUN sh ./gradlew --no-daemon :app:bootJar

FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70
WORKDIR /app
LABEL org.opencontainers.image.source="https://github.com/rusanoph/clastor-distributed-lock"

COPY --from=build /src/app/build/libs/app.jar ./app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/v1/locks/held || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
