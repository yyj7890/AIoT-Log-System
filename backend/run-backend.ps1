$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;D:\AI\IOT\tools\apache-maven-3.9.16\bin;$env:Path"

mvn -s D:\AI\IOT\tools\maven-settings.xml spring-boot:run

