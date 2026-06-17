# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Force rebuild: timestamp 18:00:15
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Instalar fuentes necesarias para JasperReports
RUN apt-get update && apt-get install -y \
    fonts-liberation \
    fonts-liberation2 \
    fonts-dejavu \
    fonts-dejavu-core \
    fontconfig \
    libfreetype6 \
    xfonts-encodings \
    xfonts-utils \
    msttcorefonts \
    && fc-cache -f -v \
    && rm -rf /var/lib/apt/lists/*

# Copiar JAR compilado
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.awt.headless=false", "-jar", "app.jar"]