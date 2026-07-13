@echo off
setlocal
cd /d "%~dp0"
docker compose -f docker-compose.ghcr.yml down
pause
