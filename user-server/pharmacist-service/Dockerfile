FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests -pl user-server/pharmacist-service -am install
RUN mvn -q -DskipTests -pl user-server/pharmacist-service package spring-boot:repackage
RUN JAR_FILE=$(find /app/user-server/pharmacist-service/target -maxdepth 1 -type f -name "*.jar" ! -name "*.original" ! -name "*-plain.jar" | head -n 1) && test -n "$JAR_FILE" && cp "$JAR_FILE" /tmp/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /tmp/app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
