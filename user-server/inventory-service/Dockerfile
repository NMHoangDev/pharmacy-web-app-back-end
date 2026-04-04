FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests -pl user-server/inventory-service -am install
RUN mvn -q -DskipTests -pl user-server/inventory-service package spring-boot:repackage
RUN JAR_FILE=$(find /app/user-server/inventory-service/target -maxdepth 1 -type f -name "*.jar" ! -name "*.original" ! -name "*-plain.jar" | head -n 1) && test -n "$JAR_FILE" && cp "$JAR_FILE" /tmp/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /tmp/app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
