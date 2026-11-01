# ---- Build stage: compile and package the Spring Boot jar ----
FROM eclipse-temurin:17-jdk-noble AS build
WORKDIR /workspace

# Copy the build definition first so dependency downloads are cached between source changes.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
# Guard against CRLF line endings if the repo was checked out on Windows without .gitattributes.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon dependencies > /dev/null

COPY src src
# Tests run in the developer/CI build, not in the image build.
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar -x test

# ---- Runtime stage: JRE only, non-root user ----
FROM eclipse-temurin:17-jre-noble
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/build/libs/fraud-service-*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
