# AIoT Log Backend

Spring Boot backend for the AIoT intelligent device running log management system.

## Requirements

- JDK 17+
- Maven 3.9.16
- MySQL 8.x

Project decisions are recorded in:

```text
../docs/technical-decisions.md
../docs/development-log.md
../docs/project-status.md
../PROJECT_CONTEXT.md
```

## Database

Run these SQL files before starting the backend:

```text
../sql/schema.sql
../sql/init-data.sql
```

Update database connection settings in:

```text
src/main/resources/application.yml
```

Default settings:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aiot_log_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root
```

## Run

If your default `java` command points to Java 8, run Maven with JDK 17:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;D:\AI\IOT\tools\apache-maven-3.9.16\bin;$env:Path"
mvn -s D:\AI\IOT\tools\maven-settings.xml spring-boot:run
```

Current local backend verification uses `spring-boot:run` because `spring-boot:repackage` can fail on Windows if the target jar is locked during rename.

To start MySQL with the project-local data directory:

```powershell
Start-Process -FilePath 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' -ArgumentList '--basedir="C:\Program Files\MySQL\MySQL Server 8.4" --datadir="D:\AI\IOT\mysql-data" --port=3306 --bind-address=127.0.0.1' -WindowStyle Hidden
```

## Implemented APIs

Dashboard:

```text
GET /api/dashboard/summary
```

Device management:

```text
GET    /api/devices
GET    /api/devices/{id}
POST   /api/devices
PUT    /api/devices/{id}
DELETE /api/devices/{id}
```

Log management:

```text
GET    /api/logs
GET    /api/logs/{id}
POST   /api/logs
PUT    /api/logs/{id}
PATCH  /api/logs/{id}/status
DELETE /api/logs/{id}
```

Tag management:

```text
GET    /api/tags
POST   /api/tags
DELETE /api/tags/{id}
```

Enums:

```text
GET /api/enums
```
