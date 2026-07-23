param(
    [string]$OutputDirectory = (Join-Path (Split-Path -Parent $PSScriptRoot) 'dist/synology-image-deploy'),
    [switch]$Remote,
    [switch]$TencentRegistry
)

$ErrorActionPreference = 'Stop'
if ($TencentRegistry -and -not $Remote) {
    throw '-TencentRegistry can only be used together with -Remote.'
}
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputParent = Split-Path -Parent $OutputDirectory
$zipName = if ($Remote -and $TencentRegistry) {
    'aiot-synology-remote-tcr-image-deploy.zip'
} elseif ($Remote) {
    'aiot-synology-remote-image-deploy.zip'
} else {
    'aiot-synology-image-deploy.zip'
}
$zipPath = Join-Path $outputParent $zipName
$composeName = if ($Remote -and $TencentRegistry) {
    'docker-compose.remote.tcr.yml'
} elseif ($Remote) {
    'docker-compose.remote.ghcr.yml'
} else {
    'docker-compose.ghcr.yml'
}
$composeSource = Join-Path $projectRoot $composeName
$schemaSource = Join-Path $projectRoot 'sql/schema.sql'
$initializerSource = Join-Path $PSScriptRoot $(if ($Remote) { 'initialize-synology-remote-docker-config.sh' } else { 'initialize-synology-docker-config.sh' })
$remoteTemplateSource = Join-Path $projectRoot 'config/hivemq-remote.env.example'

$requiredSources = @($composeSource, $schemaSource, $initializerSource)
if ($Remote) { $requiredSources += $remoteTemplateSource }
foreach ($path in $requiredSources) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required deployment source is missing: $path"
    }
}

New-Item -ItemType Directory -Force -Path $OutputDirectory, (Join-Path $OutputDirectory 'tools') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $OutputDirectory 'sql') | Out-Null
if ($Remote) { New-Item -ItemType Directory -Force -Path (Join-Path $OutputDirectory 'config') | Out-Null }
$legacyDemoSql = Join-Path $OutputDirectory 'sql/init-data.sql'
if (Test-Path -LiteralPath $legacyDemoSql -PathType Leaf) {
    Remove-Item -LiteralPath $legacyDemoSql -Force
}
Copy-Item -LiteralPath $composeSource -Destination (Join-Path $OutputDirectory 'docker-compose.yml') -Force
Copy-Item -LiteralPath $schemaSource -Destination (Join-Path $OutputDirectory 'sql/schema.sql') -Force
Copy-Item -LiteralPath $initializerSource -Destination (Join-Path $OutputDirectory 'tools/initialize-synology-docker-config.sh') -Force
if ($Remote) { Copy-Item -LiteralPath $remoteTemplateSource -Destination (Join-Path $OutputDirectory 'config/hivemq-remote.env.example') -Force }

if ($Remote) {
$registryDescription = if ($TencentRegistry) {
    '`ccr.ccs.tencentyun.com/aiot-log-system/aiot-log-backend` and `ccr.ccs.tencentyun.com/aiot-log-system/aiot-log-frontend`'
} else {
    '`ghcr.io/yyj7890/aiot-log-backend` and `ghcr.io/yyj7890/aiot-log-frontend`'
}
@"
# Synology Container Manager remote HiveMQ deployment

This package intentionally contains no frontend or backend source code. It uses the prebuilt
$registryDescription images.
It connects the backend outbound to HiveMQ Cloud with TLS; it does not contain Mosquitto,
UDP discovery, or an exposed MQTT port.
The package keeps a frozen V1 compatibility schema for older published images. Flyway in
new backend images records that schema as baseline version 1 and applies later migrations.

1. Import or pull both images in Container Manager.
2. Upload this folder to `/volume1/docker/aiot-remote-image`.
3. Through SSH as an administrator, run:

   sudo -i
   cd /volume1/docker/aiot-remote-image
   sh tools/initialize-synology-docker-config.sh

4. Edit the ignored `docker/local/hivemq-remote.env` with private HiveMQ host, username,
   and password. Keep `MQTT_MODE=remote` and an `ssl://...:8883` broker URL.
5. In Container Manager, create a project using this folder's `docker-compose.yml`.

Do not share the generated `docker/local/` directory or its credentials.
"@ | Set-Content -LiteralPath (Join-Path $OutputDirectory 'README.txt') -Encoding utf8
} else {
@'
# Synology Container Manager image deployment

This package intentionally contains no frontend or backend source code. It uses the prebuilt
`ghcr.io/yyj7890/aiot-log-backend` and `ghcr.io/yyj7890/aiot-log-frontend` images.
The backend image uses Flyway to create or upgrade the database schema without deleting data.
New users receive empty tables; no demo devices, logs, or tags are imported.
The included `sql/schema.sql` is a frozen V1 compatibility bootstrap for older published images;
all schema changes after V1 belong in backend Flyway migrations.

1. Import or pull both images in Container Manager.
2. Upload this folder to `/volume1/docker/aiot-image`.
3. Through SSH as an administrator, run:

   sudo -i
   cd /volume1/docker/aiot-image
   sh tools/initialize-synology-docker-config.sh

4. In Container Manager, create a project using this folder's `docker-compose.yml`.

Do not share the generated `.env` or `docker/local/` directory.
'@ | Set-Content -LiteralPath (Join-Path $OutputDirectory 'README.txt') -Encoding utf8
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}
$archive = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem -LiteralPath $OutputDirectory -File -Recurse | ForEach-Object {
        $relativePath = $_.FullName.Substring($OutputDirectory.Length).TrimStart([char[]]@('\', '/')).Replace('\', '/')
        $entry = $archive.CreateEntry($relativePath, [System.IO.Compression.CompressionLevel]::Optimal)
        $input = [System.IO.File]::OpenRead($_.FullName)
        $output = $entry.Open()
        try {
            $input.CopyTo($output)
        } finally {
            $output.Dispose()
            $input.Dispose()
        }
    }
} finally {
    $archive.Dispose()
}

Write-Host "Synology image deployment package created: $OutputDirectory"
Write-Host "Portable ZIP created: $zipPath"
