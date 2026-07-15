@echo off
setlocal
cd /d "%~dp0"

PowerShell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\initialize-remote-docker-config.ps1"
if errorlevel 1 exit /b 1

docker compose -f docker-compose.remote.ghcr.yml pull
if errorlevel 1 exit /b 1
docker compose -f docker-compose.remote.ghcr.yml up -d
