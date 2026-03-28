FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy pre-built boot JAR (run `./gradlew clean bootJar -x test` locally first)
# Use explicit name to avoid copying the -plain.jar
COPY build/libs/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
