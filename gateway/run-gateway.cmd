@echo off
REM Run from gateway folder. Reads SERVER_PORT from identity-service/.env to construct services.auth.uri and starts the gateway.
cd /d "%~dp0"
set "IDENTITY_ENV=..\user-server\identity-service\.env"
if exist "%IDENTITY_ENV%" (
  for /f "usebackq tokens=1* delims==" %%A in ("%IDENTITY_ENV%") do (
    echo %%A | findstr /b "#" >nul
    if errorlevel 1 (
      if /I "%%A"=="SERVER_PORT" set "SERVER_PORT=%%B"
    )
  )
) else (
  echo Warning: %IDENTITY_ENV% not found. Will use default port 8081 for identity.
)
if defined SERVER_PORT (
  set "SERVICES_AUTH_URI=http://localhost:%SERVER_PORT%"
) else (
  set "SERVICES_AUTH_URI=http://localhost:8081"
)
echo Starting gateway forwarding /api/auth/** to %SERVICES_AUTH_URI% ...
mvn spring-boot:run -Dspring-boot.run.arguments="--services.auth.uri=%SERVICES_AUTH_URI%"
