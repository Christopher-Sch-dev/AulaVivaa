# ==========================================
# STAGE 1: BUILD
# ==========================================
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy gradle wrapper and config files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY backend/build.gradle.kts backend/
# If there are other modules referenced in settings.gradle.kts, copy their build files too.
# Based on file listing, we have 'app' and 'backend'. 'app' is android, ignored here.
# We focus on root and backend.

# Grant execution rights on the gradlew wrapper
RUN chmod +x ./gradlew

# Download dependencies (this layer will be cached if dependencies don't change)
# We run for the specific module :backend
RUN ./gradlew :backend:dependencies --no-daemon

# Copy source code
COPY backend/src backend/src
# Copy root src if any? Root seems empty of src based on previous ls.

# Build the application
RUN ./gradlew :backend:bootJar --no-daemon -x test

# ==========================================
# STAGE 2: RUNTIME
# ==========================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Security: Create non-root user
RUN addgroup --system javauser && adduser --system --ingroup javauser --no-create-home javauser
USER javauser

# Copy DB certificate if needed (Supabase usually doesn't strictly require a custom cert bundle for java if standard CAs are present, but keep in mind)

# Copy the built jar
# The jar will be in backend/build/libs/
COPY --from=builder /app/backend/build/libs/*.jar app.jar

EXPOSE 8080

# JVM Flags optimized for containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
