@echo off
setlocal
cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker was not found. Install and start Docker Desktop first.
  pause
  exit /b 1
)

PowerShell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\initialize-docker-config.ps1"
if errorlevel 1 (
  echo.
  echo Docker private configuration could not be prepared.
  pause
  exit /b 1
)

docker compose -f docker-compose.ghcr.yml up -d
if errorlevel 1 (
  echo.
  echo GHCR image startup failed. Run docker-ghcr-update.cmd to pull the latest images.
  pause
  exit /b 1
)

echo.
echo Waiting for the backend service...
for /l %%i in (1,1,60) do (
  curl.exe --silent --fail --output nul "http://127.0.0.1:8080/api/dashboard/summary" >nul 2>nul && goto backend_ready
  timeout /t 2 /nobreak >nul
)

echo Backend service did not become ready within 120 seconds.
echo Run "docker compose -f docker-compose.ghcr.yml ps" to inspect it.
pause
exit /b 1

:backend_ready
echo Waiting for the web service...
for /l %%i in (1,1,30) do (
  curl.exe --silent --fail --output nul "http://127.0.0.1/" >nul 2>nul && goto ready
  timeout /t 1 /nobreak >nul
)

echo Web service did not become ready within 30 seconds.
echo Run "docker compose -f docker-compose.ghcr.yml ps" to inspect it.
pause
exit /b 1

:ready
echo AIoT Log System is ready from GHCR images.
echo Web:     http://127.0.0.1/
echo Backend: http://127.0.0.1:8080/
echo MQTT:    127.0.0.1:1883
echo UDP discovery: 127.0.0.1:19830
start "" "http://127.0.0.1/"
pause
