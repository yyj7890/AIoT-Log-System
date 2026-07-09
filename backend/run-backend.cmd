@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;D:\AI\IOT\tools\apache-maven-3.9.16\bin;%PATH%"

mvn -s D:\AI\IOT\tools\maven-settings.xml spring-boot:run

