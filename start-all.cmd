@echo off
setlocal

set "PROJECT_ROOT=D:\AI\IOT"

netstat -ano | findstr /R /C:":3306 .*LISTENING" >nul
if errorlevel 1 (
  net session >nul 2>&1
  if errorlevel 1 (
    echo MySQL is not running and may require administrator permission to start.
    echo Requesting administrator permission...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
  )
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\start-all.ps1" -NoBrowser
if errorlevel 1 (
  echo.
  echo start-all failed. Please check the messages above.
  pause
  exit /b 1
)

start "" "http://127.0.0.1:5173/"

endlocal
