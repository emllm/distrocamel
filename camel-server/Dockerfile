FROM gradle:7.6.1-jdk11 AS builder

WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM openjdk:11-jre-slim

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Create directory for storing processed emails
RUN mkdir -p /app/emails

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
