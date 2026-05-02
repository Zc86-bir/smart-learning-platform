# Build stage
FROM maven:3.9-eclipse-temurin-24 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

# Run stage
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/target/lib ./lib

EXPOSE 9090

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-Xms512m", \
    "-Xmx1536m", \
    "-jar", \
    "app.jar"]
