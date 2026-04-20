# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Skip tests to speed up and avoid environment issues
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install dependencies for yt-dlp
RUN apt-get update && apt-get install -y \
    python3 \
    curl \
    ffmpeg \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Install yt-dlp binary directly
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp

# Install Deno because recent yt-dlp YouTube extraction relies on a supported JS runtime.
RUN curl -fsSL https://deno.land/install.sh | DENO_INSTALL=/usr/local sh \
    && chmod a+rx /usr/local/bin/deno

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create downloads directory
RUN mkdir -p /app/downloads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
