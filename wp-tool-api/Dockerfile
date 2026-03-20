FROM eclipse-temurin:17-jre

WORKDIR /app

ARG JAR_FILE=target/wp-tool-api-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Base directory for app outputs; internal subdirectories are created by the app.
ENV APP_WORK_DIR=/app/work-dir

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]