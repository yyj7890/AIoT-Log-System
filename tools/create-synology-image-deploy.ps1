param(
    [string]$OutputDirectory = (Join-Path (Split-Path -Parent $PSScriptRoot) 'dist/synology-image-deploy')
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputParent = Split-Path -Parent $OutputDirectory
$zipPath = Join-Path $outputParent 'aiot-synology-image-deploy.zip'
$composeSource = Join-Path $projectRoot 'docker-compose.ghcr.yml'
$schemaSource = Join-Path $projectRoot 'sql/schema.sql'
$initializerSource = Join-Path $PSScriptRoot 'initialize-synology-docker-config.sh'

foreach ($path in @($composeSource, $schemaSource, $initializerSource)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required deployment source is missing: $path"
    }
}

New-Item -ItemType Directory -Force -Path $OutputDirectory, (Join-Path $OutputDirectory 'sql'), (Join-Path $OutputDirectory 'tools') | Out-Null
$legacyDemoSql = Join-Path $OutputDirectory 'sql/init-data.sql'
if (Test-Path -LiteralPath $legacyDemoSql -PathType Leaf) {
    Remove-Item -LiteralPath $legacyDemoSql -Force
}
Copy-Item -LiteralPath $composeSource -Destination (Join-Path $OutputDirectory 'docker-compose.yml') -Force
Copy-Item -LiteralPath $schemaSource -Destination (Join-Path $OutputDirectory 'sql/schema.sql') -Force
Copy-Item -LiteralPath $initializerSource -Destination (Join-Path $OutputDirectory 'tools/initialize-synology-docker-config.sh') -Force

@'
# Synology Container Manager image deployment

This package intentionally contains no frontend or backend source code. It uses the prebuilt
`ghcr.io/yyj7890/aiot-log-backend` and `ghcr.io/yyj7890/aiot-log-frontend` images.
It creates an empty database schema for new users; no demo devices, logs, or tags are imported.

1. Import or pull both images in Container Manager.
2. Upload this folder to `/volume1/docker/aiot-image`.
3. Through SSH as an administrator, run:

   sudo -i
   cd /volume1/docker/aiot-image
   sh tools/initialize-synology-docker-config.sh

4. In Container Manager, create a project using this folder's `docker-compose.yml`.

Do not share the generated `.env` or `docker/local/` directory.
'@ | Set-Content -LiteralPath (Join-Path $OutputDirectory 'README.txt') -Encoding utf8

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
