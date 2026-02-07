FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
# O JAR é copiado do diretório target preenchido pelo GitHub Actions
COPY target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
