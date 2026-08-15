FROM openjdk:21-ea-1-jdk-oracle
LABEL authors="Felipe Mattos"
WORKDIR /app
COPY  build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar","app.jar"]

# docker build -t arpeggio-api -f Dockerfile .