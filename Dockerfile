FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test && \
    ls -la /app/build/libs/ && \
    jar tf /app/build/libs/MedCardServer-all.jar | head -20

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copy Shadow JAR file (chứa tất cả dependencies)
COPY --from=builder /app/build/libs/MedCardServer-all.jar app.jar
COPY *.properties ./
EXPOSE 8888
ENTRYPOINT ["java","-jar","app.jar"]
