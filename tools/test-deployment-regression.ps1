param(
    [switch]$SkipComposeValidation
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$composeFiles = @(
    'docker-compose.yml',
    'docker-compose.remote.yml',
    'docker-compose.ghcr.yml',
    'docker-compose.remote.ghcr.yml'
)

function Assert-Condition {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Read-ProjectFile {
    param([string]$RelativePath)
    return Get-Content -LiteralPath (Join-Path $projectRoot $RelativePath) -Raw -Encoding utf8
}

foreach ($composeFile in $composeFiles) {
    $content = Read-ProjectFile $composeFile
    Assert-Condition ($content -match '(?m)^\s*-\s+mysql-data:/var/lib/mysql\s*$') `
        "$composeFile must preserve MySQL data in the mysql-data named volume."
    Assert-Condition ($content -match '(?ms)^volumes:\s*\r?\n\s+mysql-data:\s*$') `
        "$composeFile must declare the mysql-data named volume."
    Assert-Condition ($content -match 'jdbc:mysql://mysql:3306/aiot_log_system') `
        "$composeFile backend must use the Compose MySQL service."
    Assert-Condition ($content -match '(?ms)depends_on:\s*\r?\n\s+mysql:\s*\r?\n\s+condition:\s+service_healthy') `
        "$composeFile backend must wait for the MySQL health check."

    if (-not $SkipComposeValidation) {
        & docker compose -f (Join-Path $projectRoot $composeFile) config --no-interpolate --quiet
        if ($LASTEXITCODE -ne 0) {
            throw "Docker Compose validation failed for $composeFile."
        }
    }
}

$remoteSource = Read-ProjectFile 'docker-compose.remote.yml'
$remoteGhcr = Read-ProjectFile 'docker-compose.remote.ghcr.yml'
foreach ($content in @($remoteSource, $remoteGhcr)) {
    Assert-Condition ($content -notmatch '(?m)^\s+mosquitto:\s*$') `
        'Remote Compose must not start a local Mosquitto service.'
    Assert-Condition ($content -notmatch '19830') `
        'Remote Compose must not expose UDP MQTT discovery.'
    Assert-Condition ($content -match 'MQTT_MODE:\s*remote') `
        'Remote Compose must keep MQTT_MODE=remote.'
    Assert-Condition ($content -match 'MQTT_DISCOVERY_ENABLED:\s*"false"') `
        'Remote Compose must keep MQTT discovery disabled.'
}

$remoteTags = @([regex]::Matches(
    $remoteGhcr,
    'ghcr\.io/yyj7890/aiot-log-(?:backend|frontend):([A-Za-z0-9._-]+)'
) | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique)
Assert-Condition ($remoteTags.Count -eq 1) `
    'Remote GHCR frontend and backend must use the same fixed image tag.'
Assert-Condition ($remoteTags[0] -ne 'latest') `
    'Remote GHCR deployment must never use latest.'

$localGhcr = Read-ProjectFile 'docker-compose.ghcr.yml'
$localImageTagReferences = [regex]::Matches($localGhcr, '\$\{AIOT_IMAGE_TAG:-latest\}')
Assert-Condition ($localImageTagReferences.Count -eq 2) `
    'Local GHCR frontend and backend must share AIOT_IMAGE_TAG.'

$stopScripts = @(
    Get-Item -LiteralPath (Join-Path $projectRoot 'docker-stop.cmd')
    Get-ChildItem -LiteralPath $projectRoot -Filter 'docker-*-stop.cmd'
)
foreach ($stopScript in $stopScripts) {
    $content = Get-Content -LiteralPath $stopScript.FullName -Raw -Encoding utf8
    Assert-Condition ($content -notmatch '(?i)\bdown\s+(?:-[^\r\n]*\s)*-v\b') `
        "$($stopScript.Name) must not delete named volumes."
    Assert-Condition ($content -notmatch '(?i)\bvolume\s+rm\b') `
        "$($stopScript.Name) must not remove Docker volumes."
}

Write-Host 'Deployment regression checks passed.'
Write-Host "Validated Compose files: $($composeFiles.Count)"
Write-Host "Remote fixed image tag: $($remoteTags[0])"
Write-Host "Stop scripts checked: $($stopScripts.Count)"
