# Docker 빌드 안에서 Gradle 빌드까지 끝내 runner의 Java 설치 의존을 없앰
FROM eclipse-temurin:17-jdk AS build

# Gradle wrapper가 프로젝트 기준 경로에서 실행되도록 작업 디렉터리를 고정함
WORKDIR /app

# 의존성 캐시를 재사용하기 위해 빌드 설정과 wrapper를 먼저 복사함
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Linux 컨테이너 안에서 Gradle wrapper를 실행할 수 있게 권한을 맞춤
RUN chmod +x ./gradlew

# 소스 복사 전 의존성을 먼저 받아 Docker layer cache 효율을 높임
RUN ./gradlew dependencies --no-daemon

# 실제 애플리케이션 소스를 복사해 테스트 통과 후 운영 jar를 생성함
COPY src ./src
RUN ./gradlew clean test bootJar --no-daemon

# 실행 이미지는 JRE만 포함해 운영 이미지 크기를 줄임
FROM eclipse-temurin:17-jre

# Spring Boot jar를 실행할 기준 디렉터리를 고정함
WORKDIR /app

# 컨테이너 기본 실행 profile을 운영 설정으로 맞춤
ENV SPRING_PROFILES_ACTIVE=prod

# nginx가 호스트 포트로 연결할 컨테이너 내부 포트를 명시함
EXPOSE 8080

# 빌드 단계에서 생성된 Spring Boot 실행 jar만 런타임 이미지에 포함함
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 시작 시 Spring Boot 애플리케이션을 실행함
ENTRYPOINT ["java", "-jar", "app.jar"]
