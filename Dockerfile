FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY build/libs/*.jar app.jar
COPY *.properties ./

EXPOSE 8888

ENTRYPOINT ["java","-jar","app.jar"]
