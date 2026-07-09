@echo off
setlocal
cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker was not found.
  pause
  exit /b 1
)

docker compose down
if errorlevel 1 (
  echo Docker services could not be stopped.
  pause
  exit /b 1
)

echo AIoT Log System has stopped. Database data is retained.
pause
