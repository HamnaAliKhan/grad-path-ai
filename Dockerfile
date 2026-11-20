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
RUN useradd --system --uid 10001 gradpath
WORKDIR /app
COPY --from=build /src/target/gradpath-ai.jar app.jar
USER gradpath
ENV GRADPATH_DOTENV=false
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
