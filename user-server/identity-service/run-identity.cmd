@echo off
REM Run from identity-service folder. Copies .env from .env.example if missing and starts the service.
cd /d "%~dp0"
if not exist .env (
  if exist .env.example (
    copy .env.example .env >nul
    echo Created .env from .env.example. Edit .env if needed.
  ) else (
    echo Warning: .env and .env.example not found. Create .env with required values.
  )
)
echo --- Using .env ---
if exist .env (type .env) else (echo (no .env present))
echo Starting identity-service on configured SERVER_PORT...
mvn spring-boot:run
