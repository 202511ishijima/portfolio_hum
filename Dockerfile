FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY spring-app/.mvn spring-app/.mvn
COPY spring-app/mvnw spring-app/mvnw
COPY spring-app/pom.xml spring-app/pom.xml

RUN chmod +x ./spring-app/mvnw

WORKDIR /workspace/spring-app
RUN ./mvnw -B -DskipTests dependency:go-offline

COPY spring-app/src src
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app
COPY --from=build /workspace/spring-app/target/*.jar /app/app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Dserver.port=10000", "-jar", "/app/app.jar"]
