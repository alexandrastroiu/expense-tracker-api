# First build stage - build the application
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

# Second build stage - run the application
FROM eclipse-temurin:21-jre AS run

WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

COPY --from=build --chown=app:app /app/target/*.jar app.jar

USER app

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]