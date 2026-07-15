@echo off
setlocal
cd /d "%~dp0"
docker compose -f docker-compose.remote.ghcr.yml down
