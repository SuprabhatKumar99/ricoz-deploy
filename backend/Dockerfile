FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S ricozknow && adduser -S ricozknow -G ricozknow
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER ricozknow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
