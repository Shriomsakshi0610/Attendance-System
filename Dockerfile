# ========================================================
# Stage 1: Build Stage (Java 21 + Maven)
# ========================================================
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder

WORKDIR /app

# Copy Maven descriptor and wrapper files first to leverage Docker layer caching
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./
RUN chmod +x mvnw && mvn dependency:go-offline -B || true

# Copy project source tree
COPY src ./src

# Build production executable JAR (skipping unit tests during image packaging)
RUN mvn clean package -DskipTests -B

# Discover and stage the generated application JAR without assuming a fixed filename
RUN find target/ -maxdepth 1 -name "*.jar" ! -name "*original*" ! -name "*javadoc*" ! -name "*sources*" -exec cp {} app.jar \;

# ========================================================
# Stage 2: Runtime Stage (Lightweight JRE 21)
# ========================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create a dedicated non-root user and group for security
RUN groupadd -r spring && useradd -r -g spring -u 1001 spring

# Copy only the compiled executable JAR from the builder stage
COPY --from=builder --chown=spring:spring /app/app.jar app.jar

# Switch to non-root execution
USER spring:spring

# Expose default port (Render dynamically routes traffic via $PORT)
EXPOSE 8080

# Environment variables with sensible heap memory limits for free/starter tiers
ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Launch Spring Boot with dynamic port binding provided by Render's $PORT
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
