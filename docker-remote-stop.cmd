@echo off
setlocal
cd /d "%~dp0"
docker compose -f docker-compose.remote.yml down
