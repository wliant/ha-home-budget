# Multi-stage Dockerfile for Spring Boot development

FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Development mode - use spring-boot:run for hot reload
CMD ["mvn", "spring-boot:run", "-Dmaven.test.skip=true", "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]

# For production build (not used in dev):
# RUN mvn clean package -DskipTests
# FROM eclipse-temurin:17-jre
# COPY --from=build /app/target/*.jar app.jar
# ENTRYPOINT ["java", "-jar", "/app.jar"]
