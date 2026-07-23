# 앱 실행 이미지. CI(GitHub Actions)가 gradlew 로 만든 app.jar 를 JRE 위에 얹는다.
# JAR 은 아키텍처 무관(JVM 바이트코드)이고 base 는 multi-arch 라, buildx --platform linux/arm64 로 t4g(ARM) 이미지 생성 가능.
FROM eclipse-temurin:25-jre

WORKDIR /app

# 애플리케이션은 특권이 필요 없으므로 고정 UID/GID의 비-root 사용자로 실행한다.
# prod RollingFileAppender가 /app/logs를 생성·기록할 수 있도록 작업 디렉터리 소유권도 함께 설정한다.
RUN groupadd --gid 10001 app \
    && useradd --no-log-init --uid 10001 --gid app --home-dir /app --no-create-home --shell /usr/sbin/nologin app \
    && mkdir -p /app/logs \
    && chown -R app:app /app

COPY --chown=app:app app/build/libs/app.jar app.jar

# 작은 EC2 대비 컨테이너 메모리에 맞춰 힙 자동. 프로필은 SPRING_PROFILES_ACTIVE, 설정은 /config 마운트로 주입.
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
