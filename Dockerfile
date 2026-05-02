FROM eclipse-temurin:24-jre
WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 9090

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9090/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-Xms512m", \
    "-Xmx1536m", \
    "-jar", \
    "app.jar"]
