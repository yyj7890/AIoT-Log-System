param()

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$templateFile = Join-Path $projectRoot 'config/hivemq-remote.env.example'
$localConfigDirectory = Join-Path $projectRoot 'docker/local'
$remoteEnvFile = Join-Path $localConfigDirectory 'hivemq-remote.env'

function New-RandomSecret {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

if (-not (Test-Path -LiteralPath $templateFile -PathType Leaf)) {
    throw "Remote MQTT template is missing: $templateFile"
}

New-Item -ItemType Directory -Force -Path $localConfigDirectory | Out-Null
if (-not (Test-Path -LiteralPath $remoteEnvFile -PathType Leaf)) {
    Copy-Item -LiteralPath $templateFile -Destination $remoteEnvFile
}

$lines = [System.Collections.Generic.List[string]](Get-Content -LiteralPath $remoteEnvFile -Encoding utf8)
$mysqlIndex = -1
$databaseIndex = -1
$databasePassword = ''
for ($index = 0; $index -lt $lines.Count; $index++) {
    if ($lines[$index] -match '^MYSQL_ROOT_PASSWORD=(.*)$') {
        $mysqlIndex = $index
        if ([string]::IsNullOrWhiteSpace($Matches[1])) {
            $lines[$index] = 'MYSQL_ROOT_PASSWORD=' + (New-RandomSecret)
        }
        $databasePassword = $lines[$index].Substring('MYSQL_ROOT_PASSWORD='.Length)
    }
    if ($lines[$index] -match '^DB_PASSWORD=(.*)$') {
        $databaseIndex = $index
    }
}
if ($mysqlIndex -lt 0) { $databasePassword = New-RandomSecret; $lines.Add('MYSQL_ROOT_PASSWORD=' + $databasePassword) }
if ($databaseIndex -lt 0) { $lines.Add('DB_PASSWORD=' + $databasePassword) }
else { $lines[$databaseIndex] = 'DB_PASSWORD=' + $databasePassword }
Set-Content -LiteralPath $remoteEnvFile -Value $lines -Encoding utf8

$requiredKeys = @('MQTT_MODE', 'MQTT_BROKER_URL', 'MQTT_USERNAME', 'MQTT_PASSWORD')
$values = @{}
foreach ($line in $lines) {
    if ($line -match '^([A-Z0-9_]+)=(.*)$') {
        $values[$Matches[1]] = $Matches[2]
    }
}
foreach ($key in $requiredKeys) {
    $value = [string]$values[$key]
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match '<[^>]+>') {
        throw "Set $key in the ignored private file docker/local/hivemq-remote.env, then run this command again."
    }
}
if ($values['MQTT_MODE'] -ne 'remote') {
    throw 'MQTT_MODE must be remote in docker/local/hivemq-remote.env.'
}
if ($values['MQTT_BROKER_URL'] -notmatch '^ssl://') {
    throw 'MQTT_BROKER_URL must start with ssl:// so TLS certificate validation and SNI use the HiveMQ hostname.'
}

Write-Host 'Remote Docker private configuration is ready. HiveMQ credentials remain only in docker/local/hivemq-remote.env.'
