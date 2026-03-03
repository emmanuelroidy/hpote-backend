# -------------------------
# 1️⃣ Build Stage
# -------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests


# -------------------------
# 2️⃣ Run Stage
# -------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]