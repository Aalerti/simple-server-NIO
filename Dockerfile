FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY static ./static

RUN mvn clean compile

EXPOSE 8080

CMD ["mvn", "exec:java", "-Dexec.mainClass=Main"]