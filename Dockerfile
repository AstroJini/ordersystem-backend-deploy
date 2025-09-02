FROM openjdk:17-jdk-slim AS stage1

WORKDIR /app

COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
RUN chmod +x gradlew
RUN ./gradlew bootjar

# 두번쨰 스테이지
# 이미지 경량화를 위해 스테이지 분리
FROM openjdk:17-jdk-slim
WORKDIR /app
# stage1의 jar파일을 stage2로 copy
COPY --from=stage1 /app/build/libs/*.jar app.jar

# 실행 : CMD 또는 ENTRYPOINT를 통해 컨테이너 실행
ENTRYPOINT [ "java", "-jar", "app.jar" ]

# 도커 이미지 빌드
# docker build -t orderystem:v1.0 .

# 도커 컨테이너 실행
# docker내부에서 localhost를 찾는 설정은 루프백문제 발생
# docker run --name my-ordersystem -d -p 8081:8080 ordersystem:v1.0
# 도커 컨테이너 실행 시점에 docker.host.internal을 환경변수로 주입
# docker run --name my-ordersystem -d -p 8081:8080 -e SPRING_REDIS_HOST=host.docker.internal -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/ordersystem ordersystem:v1.0