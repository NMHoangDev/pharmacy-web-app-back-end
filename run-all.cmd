@echo off
setlocal
REM Simple launcher to start gateway and core microservices in separate windows.
REM Prereqs: JDK 17+, Maven, MySQL running with schemas created, Kafka running.

set ROOT=%~dp0
pushd "%ROOT%"

echo.
echo === Java version ===
java -version
echo.
echo === Maven version ===
mvn -v
echo.
REM --- Start Docker dependencies (Kafka, Keycloak) ---
echo Starting Docker dependencies: Kafka and Keycloak (detached)...
REM Build all modules once to speed up individual starts and avoid repeated downloads
echo Running a root Maven build (skip tests) to ensure modules are available...
mvn -DskipTests install
if %ERRORLEVEL% neq 0 echo Warning: root mvn install failed - individual service starts may rebuild

REM Verify Docker is available
docker --version 1>nul 2>nul
if %ERRORLEVEL% neq 0 (
	echo Warning: Docker not found in PATH. Containers may fail to start.
)
echo Removing existing containers if present...
docker rm -f kafka 2>nul || echo no kafka container
docker rm -f keycloak 2>nul || echo no keycloak container

echo Starting Kafka container (bootstrap on localhost:9092)...
docker run --name kafka -d -p 9092:9092 -e KAFKA_PROCESS_ROLES=broker,controller -e KAFKA_NODE_ID=1 -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:29093 -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:29093 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 apache/kafka:latest
if %ERRORLEVEL% neq 0 echo Warning: failed to start kafka container

echo Starting Keycloak container (host port 4040 -> container 8080)...
docker run --name keycloak -d -p 4040:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:24.0.5 start-dev
if %ERRORLEVEL% neq 0 echo Warning: failed to start keycloak container

REM Give docker services a few seconds to initialize (adjust if necessary)
echo Waiting 15s for docker services to initialize...
timeout /t 15 /nobreak >nul

REM Gateway (port configured in gateway/src/main/resources/application.yml)
start "gateway" cmd /k "mvn -DskipTests -pl gateway clean spring-boot:run"

REM User-facing services
start "identity-service" cmd /k "mvn -DskipTests -pl user-server/identity-service clean spring-boot:run"
start "user-service" cmd /k "mvn -DskipTests -pl user-server/user-service clean spring-boot:run"
start "catalog-service" cmd /k "mvn -DskipTests -pl user-server/catalog-service clean spring-boot:run"
start "inventory-service" cmd /k "mvn -DskipTests -pl user-server/inventory-service clean spring-boot:run"
start "order-service" cmd /k "mvn -DskipTests -pl user-server/order-service clean spring-boot:run"
start "payment-service" cmd /k "mvn -DskipTests -pl user-server/payment-service clean spring-boot:run"
start "media-service" cmd /k "mvn -DskipTests -pl user-server/media-service clean spring-boot:run"
start "notification-service" cmd /k "mvn -DskipTests -pl user-server/notification-service clean spring-boot:run"
start "review-service" cmd /k "mvn -DskipTests -pl user-server/review-service clean spring-boot:run"
start "appointment-service" cmd /k "mvn -DskipTests -pl user-server/appointment-service clean spring-boot:run"

REM Admin services
start "admin-bff" cmd /k "mvn -DskipTests -pl admin-server/admin-bff-service clean spring-boot:run"
start "cms-service" cmd /k "mvn -DskipTests -pl admin-server/cms-service clean spring-boot:run"
start "reporting-service" cmd /k "mvn -DskipTests -pl admin-server/reporting-service clean spring-boot:run"
start "audit-service" cmd /k "mvn -DskipTests -pl admin-server/audit-service clean spring-boot:run"
start "settings-service" cmd /k "mvn -DskipTests -pl admin-server/settings-service clean spring-boot:run"

popd
endlocal
