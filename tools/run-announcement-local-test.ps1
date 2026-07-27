param(
    [string]$RemoteEnvironmentFile = "D:\AI\IOT\docker\local\hivemq-remote.env",
    [switch]$EnableReminderScheduler
)

$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;D:\AI\IOT\tools\apache-maven-3.9.16\bin;$env:Path"

Get-Content -Encoding UTF8 $RemoteEnvironmentFile | ForEach-Object {
    # The remote environment file also contains Docker/MySQL variables.  This
    # local smoke test must inherit only MQTT settings and retain the existing
    # local datasource configuration.
    if ($_ -match '^(MQTT_[^=]+)=(.*)$') {
        Set-Item -Path ("Env:" + $matches[1]) -Value $matches[2]
    }
}

# Keep this local source test distinct from the deployed backend client.
$env:MQTT_CLIENT_ID = 'aiot-log-backend-remote-local-announcement-test'
$env:ANNOUNCEMENT_TEST_ENABLED = 'true'
if ($EnableReminderScheduler) {
    # Explicit opt-in: a local reminder smoke test may publish only the fixed
    # test Opus resource when an already-created reminder reaches its time.
    $env:REMINDER_SCHEDULER_ENABLED = 'true'
}

Set-Location 'D:\AI\IOT\backend'
mvn -s D:\AI\IOT\tools\maven-settings.xml spring-boot:run
