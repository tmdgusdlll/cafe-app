FROM eclipse-temurin:17-jre

WORKDIR /app

# bootJar 결과물을 컨테이너 내부로 복사
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]