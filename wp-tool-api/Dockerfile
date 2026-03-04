FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY wp-tool-core/pom.xml wp-tool-core/pom.xml
COPY wp-tool-api/pom.xml wp-tool-api/pom.xml

COPY wp-tool-core/src wp-tool-core/src
COPY wp-tool-api/src wp-tool-api/src

RUN mvn -f wp-tool-core/pom.xml clean install -DskipTests \
 && mvn -f wp-tool-api/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /workspace/wp-tool-api/target/wp-tool-api-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]