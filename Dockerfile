FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
# O JAR é copiado do diretório target preenchido pelo GitHub Actions
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
