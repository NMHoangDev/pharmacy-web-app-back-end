FROM eclipse-temurin:17-jre

WORKDIR /app

ARG JAR_FILE=target/discount-service-*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 7020

ENTRYPOINT ["java","-jar","/app/app.jar"]
