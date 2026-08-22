# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd -r izischool && useradd -r -g izischool izischool
COPY --from=build /app/target/izischool-backend-0.0.1-SNAPSHOT.jar app.jar
USER izischool
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
