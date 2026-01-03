FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy build files
COPY build/libs/*.jar app.jar

# Copy config files
COPY db.properties .
COPY bank.properties .
COPY minio.properties .

# Expose port
EXPOSE 8888

# Run application
CMD ["java", "-jar", "app.jar"]

