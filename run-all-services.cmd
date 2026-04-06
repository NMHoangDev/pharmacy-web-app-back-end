@echo off
REM Script to run all services: user-server, admin-bff-service, and gateway-service
REM Each service runs in a separate command window

setlocal enabledelayedexpansion

set BASE_DIR=%~dp0
set MAVEN_CMD=mvn -DskipTests clean spring-boot:run

echo Starting all services...
echo.

REM Start Admin BFF Service
echo Starting Admin BFF Service...
start "Admin BFF Service" cmd /k "cd /d "%BASE_DIR%admin-server\admin-bff-service" && %MAVEN_CMD%"
timeout /t 2 /nobreak

REM Start all User Server sub-services in separate windows
echo Starting User Server sub-services...
timeout /t 2 /nobreak

start "Appointment Service" cmd /k "cd /d "%BASE_DIR%user-server\appointment-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "AI Service" cmd /k "cd /d "%BASE_DIR%user-server\ai-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Branch Service" cmd /k "cd /d "%BASE_DIR%user-server\branch-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Cart Service" cmd /k "cd /d "%BASE_DIR%user-server\cart-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Catalog Service" cmd /k "cd /d "%BASE_DIR%user-server\catalog-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Content Service" cmd /k "cd /d "%BASE_DIR%user-server\content-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Discount Service" cmd /k "cd /d "%BASE_DIR%user-server\discount-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Identity Service" cmd /k "cd /d "%BASE_DIR%user-server\identity-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Inventory Service" cmd /k "cd /d "%BASE_DIR%user-server\inventory-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Media Service" cmd /k "cd /d "%BASE_DIR%user-server\media-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Notification Service" cmd /k "cd /d "%BASE_DIR%user-server\notification-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Order Service" cmd /k "cd /d "%BASE_DIR%user-server\order-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Payment Service" cmd /k "cd /d "%BASE_DIR%user-server\payment-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Pharmacist Service" cmd /k "cd /d "%BASE_DIR%user-server\pharmacist-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "Review Service" cmd /k "cd /d "%BASE_DIR%user-server\review-service" && %MAVEN_CMD%"
timeout /t 1 /nobreak
start "User Service" cmd /k "cd /d "%BASE_DIR%user-server\user-service" && %MAVEN_CMD%"
timeout /t 2 /nobreak

REM Start Gateway Service after downstream services
echo Starting Gateway Service...
start "Gateway Service" cmd /k "cd /d "%BASE_DIR%platform\gateway" && %MAVEN_CMD%"

echo.
echo All services are starting in separate windows.
echo Close each window to stop the respective service.
pause
