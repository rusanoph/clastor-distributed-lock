FROM eclipse-temurin:25-jdk@sha256:ce2ef9d2d67a68de5e196d4de9d743327c44a10b57fe02ec39f572c652cacfd3 AS build
WORKDIR /src
COPY gradlew gradle.properties settings.gradle.kts settings-gradle.lockfile build.gradle.kts LICENSE ./
COPY gradle/ gradle/
COPY config/ config/
COPY domain/ domain/
COPY adapter/ adapter/
COPY app/ app/
RUN sh ./gradlew --no-daemon :app:bootJar

FROM eclipse-temurin:25-jre-alpine@sha256:69c2a26e9894016e274c880fa542956d005b3755f84db765145185e87405bd89
WORKDIR /app
LABEL org.opencontainers.image.source="https://github.com/rusanoph/clastor-distributed-lock"

COPY --from=build /src/app/build/libs/app.jar ./app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/v1/locks/held || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
