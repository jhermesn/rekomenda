FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies in a separate layer to leverage Docker cache
RUN ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests -q

# ---- Runtime image ----
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

COPY --from=build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
