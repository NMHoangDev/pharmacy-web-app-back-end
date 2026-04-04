FROM eclipse-temurin:17-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=target/discount-service-*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 7020

ENTRYPOINT ["java","-jar","/app/app.jar"]
