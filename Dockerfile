# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Baixa dependencias em camada separada (cache) antes de copiar o codigo
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/parking-1.0.0.jar app.jar
EXPOSE 3003
ENTRYPOINT ["java", "-jar", "app.jar"]
