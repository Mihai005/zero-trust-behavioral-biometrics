FROM maven:3.9-eclipse-temurin-26-alpine AS builder

WORKDIR /build

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:26-jre-alpine

WORKDIR /app

COPY --from=builder /build/target/*.jar gateway.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "gateway.jar"]
