@echo off
setlocal
cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker was not found. Install and start Docker Desktop first.
  pause
  exit /b 1
)

docker compose up -d --build
if errorlevel 1 (
  echo.
  echo Docker update failed. Review the error above.
  pause
  exit /b 1
)

echo.
echo AIoT Log System was rebuilt and started successfully.
echo Web: http://127.0.0.1/
pause
