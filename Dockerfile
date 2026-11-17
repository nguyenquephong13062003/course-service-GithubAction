# ---- Giai đoạn 1: build file jar ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy riêng gradle wrapper + file cấu hình trước để tận dụng cache của Docker
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
# Bỏ qua test khi build image (chạy test riêng bằng ./gradlew test)
RUN ./gradlew --no-daemon bootJar -x test \
    && cp "$(ls build/libs/*.jar | grep -v plain | head -n 1)" /workspace/app.jar

# ---- Giai đoạn 2: image chạy ứng dụng ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl dùng cho healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home appuser

COPY --from=build /workspace/app.jar app.jar

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
