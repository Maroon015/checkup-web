# Two-stage build:
#   1. "build" has the full JDK and Maven, compiles the code and makes the jar.
#   2. The final image only has a Java runtime + the jar, so it's smaller and has fewer
#      tools an attacker could use.

# ---------- 1. build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy the build files first and download dependencies. Docker caches this layer, so
# changing only source code doesn't re-download every library.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run in CI (GitHub Actions); the image build just packages.
RUN ./mvnw -B -q -DskipTests package

# ---------- 2. run ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Don't run as root inside the container.
RUN useradd --system --uid 1001 checkup
COPY --from=build /app/target/checkup-web-0.0.1-SNAPSHOT.jar app.jar
USER checkup

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
