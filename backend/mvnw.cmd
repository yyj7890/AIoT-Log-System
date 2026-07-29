@echo off
setlocal EnableExtensions

set "WRAPPER_DIR=%~dp0.mvn\wrapper"
set "PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties"
if not exist "%PROPERTIES%" (
  echo Maven Wrapper configuration is missing: %PROPERTIES% 1>&2
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%PROPERTIES%") do (
  if "%%A"=="distributionUrl" set "DISTRIBUTION_URL=%%B"
)
if not defined DISTRIBUTION_URL (
  echo distributionUrl is missing from %PROPERTIES% 1>&2
  exit /b 1
)

for %%A in ("%DISTRIBUTION_URL%") do set "ARCHIVE_NAME=%%~nxA"
set "MAVEN_VERSION=%ARCHIVE_NAME:apache-maven-=%"
set "MAVEN_VERSION=%MAVEN_VERSION:-bin.zip=%"
if defined MAVEN_USER_HOME (set "WRAPPER_HOME=%MAVEN_USER_HOME%") else (set "WRAPPER_HOME=%USERPROFILE%\.m2")
set "DIST_DIR=%WRAPPER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%DIST_DIR%\apache-maven-%MAVEN_VERSION%"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runMaven
echo Downloading Apache Maven %MAVEN_VERSION% for this project...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; New-Item -ItemType Directory -Force -Path '%DIST_DIR%' | Out-Null; $archive=Join-Path '%DIST_DIR%' '%ARCHIVE_NAME%'; Invoke-WebRequest -UseBasicParsing '%DISTRIBUTION_URL%' -OutFile $archive; Expand-Archive -LiteralPath $archive -DestinationPath '%DIST_DIR%' -Force; Remove-Item -LiteralPath $archive -Force"
if errorlevel 1 (
  echo Maven download or extraction failed. Check your network or set MAVEN_USER_HOME. 1>&2
  exit /b 1
)

:runMaven
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
