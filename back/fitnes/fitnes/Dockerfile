# Prva faza: build-ovanje aplikacije
FROM maven:3.8.7-eclipse-temurin-17 AS builder

# Postavljanje radnog direktorijuma
WORKDIR /app

# Kopiranje Maven pom.xml fajla da bi se keširale zavisnosti
COPY pom.xml .

# Preuzimanje zavisnosti
RUN mvn dependency:go-offline

# Kopiranje izvornog koda aplikacije
COPY src ./src

# Kompajliranje koda i pakovanje u JAR fajl
RUN mvn clean install -DskipTests

# Druga faza: kreiranje finalnog image-a
FROM eclipse-temurin:17-jre-focal

# Kopiranje JAR fajla iz prve faze
COPY --from=builder /app/target/*.jar app.jar

# Pokretanje aplikacije
ENTRYPOINT ["java","-jar","/app.jar"]
