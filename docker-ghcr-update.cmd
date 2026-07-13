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

docker compose -f docker-compose.ghcr.yml pull
if errorlevel 1 (
  echo.
  echo Could not pull GHCR images. Confirm that the packages have been published and made public.
  pause
  exit /b 1
)

call "%~dp0docker-ghcr-start.cmd"
exit /b %errorlevel%
