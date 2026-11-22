# The server image. Build the server first on the host, then the image:
#
#   ./gradlew :server:installDist
#   docker compose up --build
#
# Gradle runs on the host because the project also contains the Android app, which needs the
# Android SDK; the image only needs the finished server and a Java runtime.
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S drivemetrics && adduser -S drivemetrics -G drivemetrics
WORKDIR /app
COPY build/install/server/ /app/
USER drivemetrics

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -qO- http://localhost:8080/health || exit 1
ENTRYPOINT ["/app/bin/server"]
