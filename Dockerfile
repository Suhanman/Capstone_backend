# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 의존성 레이어 캐시 최적화 (pom.xml만 먼저 복사)
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# 소스 복사 후 빌드
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 비루트 유저/그룹 생성 (k8s 보안 정책 대응)
RUN addgroup -S suhn && adduser -S suhn -G suhn

# 빌드 산출물 복사
COPY --from=builder /app/target/email-agent-0.0.1-SNAPSHOT.jar app.jar

# 파일 업로드 디렉토리 생성 및 권한 설정
# k8s PersistentVolumeClaim 마운트 경로와 맞출 것
RUN mkdir -p /app/uploads && chown -R suhn:suhn /app

# JVM 컨테이너 최적화 옵션
# - UseContainerSupport: cgroup 기반 메모리/CPU 제한 자동 인식 (Java 11+ 기본)
# - MaxRAMPercentage: 컨테이너 메모리의 75%를 힙으로 사용
# - security.egd: /dev/random 블로킹 방지 (컨테이너 환경 엔트로피 부족 대응)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# application.yml 하드코딩 경로를 컨테이너 경로로 오버라이드
# (k8s Deployment env 또는 ConfigMap으로 재정의 가능)
ENV APP_FILE_UPLOAD_DIR=/app/uploads

USER suhn

EXPOSE 8080

# exec 형식으로 PID 1에 Java 프로세스 배치 → k8s SIGTERM 정상 수신 (Graceful Shutdown)
CMD exec java $JAVA_OPTS \
    -Dapp.file.upload-dir=${APP_FILE_UPLOAD_DIR} \
    -jar app.jar
