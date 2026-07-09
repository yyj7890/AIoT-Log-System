@echo off
setlocal

set "PROJECT_ROOT=D:\AI\IOT"

net session >nul 2>&1
if errorlevel 1 (
  echo Stopping services may require administrator permission.
  echo Requesting administrator permission...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\stop-all.ps1"

echo.
echo Stop command finished. You can close this window.
pause

endlocal
