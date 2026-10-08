# ---------- Étape 1 : compiler avec Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1. D'abord le module commun, installé dans le dépôt Maven du conteneur
COPY movie-booking-commons/pom.xml movie-booking-commons/pom.xml
COPY movie-booking-commons/src movie-booking-commons/src
RUN mvn -f movie-booking-commons/pom.xml clean install -DskipTests

# 2. Ensuite le service
COPY booking-service/pom.xml booking-service/pom.xml
COPY booking-service/src booking-service/src
RUN mvn -f booking-service/pom.xml clean package -DskipTests

# ---------- Étape 2 : image finale légère ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/booking-service/target/*.jar app.jar

EXPOSE 9191
ENTRYPOINT ["java", "-jar", "app.jar"]