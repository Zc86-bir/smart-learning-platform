FROM eclipse-temurin:24-jre
WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 9090

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/localhost/9090; echo -e "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3; head -1 <&3 | grep -q "200"'

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-Xms512m", \
    "-Xmx2g", \
    "-jar", \
    "app.jar"]
