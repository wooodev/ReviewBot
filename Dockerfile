# ── Build Stage ────────────────────────────────────────────────────────────────
# gradle 이미지를 사용하므로 gradlew wrapper 불필요
FROM gradle:8.12-jdk21-alpine AS builder
WORKDIR /workspace

# 의존성 캐시 레이어 (소스 변경 시에도 재다운로드 방지)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon -q 2>/dev/null || true

COPY src src
RUN gradle bootJar --no-daemon -x test

# ── Runtime Stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S reviewbot && adduser -S reviewbot -G reviewbot

COPY --from=builder /workspace/build/libs/*.jar app.jar

RUN chown reviewbot:reviewbot app.jar
USER reviewbot

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
