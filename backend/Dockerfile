# Build stage - dependencies are resolved in their own layer so source-only
# changes do not re-download the world on every deploy.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Runtime stage - JRE only, no build tooling in the shipped image.
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --create-home --shell /bin/bash app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage keeps the heap inside Render's 512 MB free tier rather than
# letting the JVM size itself against the host's total memory.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=70 -jar app.jar"]
