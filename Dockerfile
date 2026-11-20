# GradPath AI - container image (Java only).
# Build:  docker build -t gradpath-ai .
# Run:    docker run -p 8080:8080 -e DATABASE_URL=... -e OFFICER_ACCESS_CODE=... -e COOKIE_SECURE=true gradpath-ai
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/gradpath-ai.jar app.jar
# Run as an unprivileged user. UID 1000 already exists in the Ubuntu base image and is the UID that
# Hugging Face Spaces uses, so the same image works there, on Render and with plain Docker.
USER 1000:1000
ENV GRADPATH_DOTENV=false
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
