param()

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
$localConfigDirectory = Join-Path $projectRoot 'docker/local'

function New-RandomSecret {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return [Convert]::ToBase64String($bytes)
}

function Get-EnvValue([string]$Name) {
    foreach ($line in $script:envLines) {
        if ($line -match ('^' + [regex]::Escape($Name) + '=(.*)$')) {
            return $Matches[1]
        }
    }
    return ''
}

function Set-EnvValue([string]$Name, [string]$Value) {
    $expression = '^' + [regex]::Escape($Name) + '='
    for ($index = 0; $index -lt $script:envLines.Count; $index++) {
        if ($script:envLines[$index] -match $expression) {
            $script:envLines[$index] = "$Name=$Value"
            return
        }
    }
    $script:envLines.Add("$Name=$Value")
}

function Ensure-EnvValue([string]$Name, [scriptblock]$CreateValue) {
    $value = Get-EnvValue $Name
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match '^<.*>$') {
        $value = & $CreateValue
        Set-EnvValue $Name $value
    }
    return $value
}

function Get-LanIPv4 {
    $configuration = Get-NetIPConfiguration | Where-Object {
        $_.IPv4Address -and $_.IPv4DefaultGateway -and
        $_.IPv4Address.IPAddress -notlike '127.*' -and
        $_.IPv4Address.IPAddress -notlike '169.254.*'
    } | Select-Object -First 1
    if (-not $configuration) {
        return $null
    }
    return $configuration.IPv4Address.IPAddress
}

if (Test-Path -LiteralPath $envFile) {
    $script:envLines = [System.Collections.Generic.List[string]](Get-Content -LiteralPath $envFile -Encoding utf8)
} else {
    $script:envLines = [System.Collections.Generic.List[string]]::new()
    $script:envLines.Add('# Local Docker secrets. This file is ignored by Git.')
}

$mysqlPassword = Ensure-EnvValue 'MYSQL_ROOT_PASSWORD' { New-RandomSecret }
$mqttUsername = Ensure-EnvValue 'MQTT_USERNAME' { 'aiot-device' }
$mqttPassword = Ensure-EnvValue 'MQTT_PASSWORD' { New-RandomSecret }
Ensure-EnvValue 'MYSQL_PORT' { '3306' } | Out-Null
Ensure-EnvValue 'MQTT_PORT' { '1883' } | Out-Null
Ensure-EnvValue 'MQTT_DISCOVERY_PORT' { '19830' } | Out-Null
Ensure-EnvValue 'BACKEND_PORT' { '8080' } | Out-Null
Ensure-EnvValue 'WEB_PORT' { '80' } | Out-Null
$lanIPv4 = Get-LanIPv4
if ($lanIPv4) {
    Set-EnvValue 'MQTT_DISCOVERY_ENABLED' 'true'
    Set-EnvValue 'MQTT_DISCOVERY_BROKER_HOST' $lanIPv4
} else {
    Set-EnvValue 'MQTT_DISCOVERY_ENABLED' 'false'
    Set-EnvValue 'MQTT_DISCOVERY_BROKER_HOST' ''
}

Set-Content -LiteralPath $envFile -Value $script:envLines -Encoding utf8

New-Item -ItemType Directory -Force -Path $localConfigDirectory | Out-Null
$aclFile = Join-Path $localConfigDirectory 'mosquitto-acl.conf'
$passwordFile = Join-Path $localConfigDirectory 'mosquitto-passwords'
$mosquittoConfigFile = Join-Path $localConfigDirectory 'mosquitto-lan.conf'
$credentialEnvFile = Join-Path $localConfigDirectory 'mqtt-credentials.env'
$credentialPropertiesFile = Join-Path $localConfigDirectory 'mqtt-credentials.properties'

Set-Content -LiteralPath $aclFile -Encoding utf8 -Value @(
    '# Generated local Docker ACL. Do not commit this file.'
    "user $mqttUsername"
    'topic readwrite aiot/device/+/report'
    'topic readwrite aiot/device/+/log'
)
Set-Content -LiteralPath $mosquittoConfigFile -Encoding utf8 -Value @(
    'persistence true'
    'persistence_location /mosquitto/data/'
    'log_dest stdout'
    'listener 1883 0.0.0.0'
    'allow_anonymous false'
    'password_file /mosquitto/config/mosquitto-passwords'
    'acl_file /mosquitto/config/mosquitto-acl.conf'
)
Set-Content -LiteralPath $credentialEnvFile -Encoding utf8 -Value @(
    '# Generated local Docker credential. Do not commit or share this file.'
    "MQTT_USERNAME=$mqttUsername"
    "MQTT_PASSWORD=$mqttPassword"
)
Set-Content -LiteralPath $credentialPropertiesFile -Encoding utf8 -Value @(
    '# Generated local Docker credential for Spring Boot. Do not commit or share this file.'
    "mqtt.username=$mqttUsername"
    "mqtt.password=$mqttPassword"
)

if (-not (Test-Path -LiteralPath $passwordFile)) {
    $dockerArguments = @(
        'run', '--rm', '-v', ($localConfigDirectory + ':/work'),
        'eclipse-mosquitto:2', 'mosquitto_passwd', '-b', '-c',
        '/work/mosquitto-passwords', $mqttUsername, $mqttPassword
    )
    & docker @dockerArguments | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not create the Docker Mosquitto password file. Confirm Docker Desktop is running and try again.'
    }
}

Write-Host 'Docker private configuration is ready. Secrets remain only in ignored local files.'
