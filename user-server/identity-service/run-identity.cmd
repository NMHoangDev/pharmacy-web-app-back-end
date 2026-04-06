@echo off
REM Run from identity-service folder. Starts the service using .env if present.
cd /d "%~dp0"
if not exist .env (
  echo Warning: .env file not found. Create .env with required values.
)
echo --- Using .env ---
if exist .env (type .env) else (echo (no .env present))
echo Starting identity-service on configured SERVER_PORT...
mvn spring-boot:run
