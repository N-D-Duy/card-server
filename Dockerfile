FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*-all.jar app.jar
COPY *.properties ./
EXPOSE 8888
ENTRYPOINT ["java","-jar","app.jar"]
