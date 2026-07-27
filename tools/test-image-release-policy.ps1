$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$publishPath = Join-Path $root '.github/workflows/publish-ghcr.yml'
$scheduledPath = Join-Path $root '.github/workflows/image-security.yml'
$tcrSyncPath = Join-Path $root '.github/workflows/sync-tcr.yml'
$remoteComposePath = Join-Path $root 'docker-compose.remote.ghcr.yml'
$remoteTcrComposePath = Join-Path $root 'docker-compose.remote.tcr.yml'
$frontendDockerfilePath = Join-Path $root 'frontend/Dockerfile'
$backendDockerfilePath = Join-Path $root 'backend/Dockerfile'
$backendPomPath = Join-Path $root 'backend/pom.xml'

$publish = Get-Content -Raw -Encoding UTF8 $publishPath
$scheduled = Get-Content -Raw -Encoding UTF8 $scheduledPath
$tcrSync = Get-Content -Raw -Encoding UTF8 $tcrSyncPath
$remoteCompose = Get-Content -Raw -Encoding UTF8 $remoteComposePath
$remoteTcrCompose = Get-Content -Raw -Encoding UTF8 $remoteTcrComposePath
$frontendDockerfile = Get-Content -Raw -Encoding UTF8 $frontendDockerfilePath
$backendDockerfile = Get-Content -Raw -Encoding UTF8 $backendDockerfilePath
$backendPom = Get-Content -Raw -Encoding UTF8 $backendPomPath

function Assert-Match {
    param(
        [string]$Text,
        [string]$Pattern,
        [string]$Message
    )
    if ($Text -notmatch $Pattern) {
        throw $Message
    }
}

Assert-Match $publish 'severity-cutoff:\s*critical' 'Publish workflow must block critical vulnerabilities.'
Assert-Match $publish 'only-fixed:\s*true' 'Publish workflow must block fixable vulnerabilities only.'
Assert-Match $publish 'actions/attest@[0-9a-f]{40}' 'Publish workflow must sign provenance with an immutable action SHA.'
Assert-Match $publish 'subject-digest:\s*\$\{\{\s*steps\.push\.outputs\.digest\s*\}\}' 'Attestation must bind the pushed digest.'
Assert-Match $publish 'type=raw,value=latest,enable=\$\{\{\s*github\.ref == ''refs/heads/main''\s*\}\}' 'Only main may publish latest.'
Assert-Match $publish 'type=ref,event=tag' 'Version tag publication must remain enabled.'

$scanIndex = $publish.IndexOf('Block fixable critical vulnerabilities')
$pushIndex = $publish.IndexOf('Push scanned image')
$attestIndex = $publish.IndexOf('Sign build provenance')
if ($scanIndex -lt 0 -or $pushIndex -lt 0 -or $attestIndex -lt 0 -or
    -not ($scanIndex -lt $pushIndex -and $pushIndex -lt $attestIndex)) {
    throw 'Publish order must be scan, push, then attest.'
}

Assert-Match $scheduled 'schedule:' 'Published-image scanning must run on a schedule.'
Assert-Match $scheduled 'anchore/scan-action@[0-9a-f]{40}' 'Scheduled scanner action must use an immutable SHA.'
Assert-Match $scheduled 'severity-cutoff:\s*critical' 'Scheduled scan must fail on critical vulnerabilities.'

$imageMatches = [regex]::Matches(
    $remoteCompose,
    'ghcr\.io/yyj7890/aiot-log-(?:backend|frontend):([A-Za-z0-9._-]+)'
)
$fixedTags = @($imageMatches | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique)
if ($fixedTags.Count -ne 1 -or $fixedTags[0] -notmatch '^v\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?$') {
    throw 'Remote GHCR Compose must pin both application images to one semantic-version tag.'
}

$tcrImageMatches = [regex]::Matches(
    $remoteTcrCompose,
    'ccr\.ccs\.tencentyun\.com/aiot-log-system/aiot-log-(?:backend|frontend):([A-Za-z0-9._-]+)'
)
$tcrFixedTags = @($tcrImageMatches | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique)
if ($tcrFixedTags.Count -ne 1 -or $tcrFixedTags[0] -ne $fixedTags[0]) {
    throw 'Remote Tencent TCR Compose must pin both images to the same fixed tag as GHCR.'
}

Assert-Match $tcrSync 'TARGET_REGISTRY:\s*ccr\.ccs\.tencentyun\.com/aiot-log-system' 'Tencent sync must use the approved namespace.'
Assert-Match $tcrSync 'secrets\.TCR_USERNAME' 'Tencent sync must use a repository username secret.'
Assert-Match $tcrSync 'secrets\.TCR_PASSWORD' 'Tencent sync must use a repository password secret.'
Assert-Match $tcrSync 'docker/setup-buildx-action@[0-9a-f]{40}' 'Tencent sync Buildx action must use an immutable SHA.'
Assert-Match $tcrSync 'docker/login-action@[0-9a-f]{40}' 'Tencent sync login action must use an immutable SHA.'
Assert-Match $tcrSync 'packages:\s*read' 'Tencent sync must have read-only access to the published GHCR image.'
Assert-Match $tcrSync 'docker pull "\$source_image"' 'Tencent images must be pulled from the scanned GHCR image without rebuilding.'
Assert-Match $tcrSync 'docker push "\$target_image"' 'Tencent images must be pushed to the approved TCR target.'
Assert-Match $tcrSync 'for attempt in 1 2 3' 'Tencent sync must retry transient registry transfers.'
Assert-Match $tcrSync 'Image config digest mismatch' 'Tencent sync must fail when GHCR and mirror image content differs.'

Assert-Match $frontendDockerfile '^FROM node:24-alpine3\.24 AS build' 'Frontend build image must use the maintained Node 24 Alpine line.'
Assert-Match $frontendDockerfile '(?m)^FROM nginx:1\.30-alpine$' 'Frontend runtime image must use the maintained Nginx 1.30 Alpine line.'
Assert-Match $backendDockerfile '^FROM maven:3\.9\.16-eclipse-temurin-17-noble AS build' 'Backend build image must use the maintained Maven/JDK 17 line.'
Assert-Match $backendDockerfile '(?m)^FROM eclipse-temurin:17-jre-noble$' 'Backend runtime image must use the maintained JRE 17 Noble line.'
Assert-Match $backendPom '<tomcat\.version>10\.1\.57</tomcat\.version>' 'Backend must keep the security-patched Tomcat 10.1.57 override until the Spring Boot line is upgraded.'

Write-Host 'Image release policy validated.'
Write-Host "Current fixed remote image tag: $($fixedTags[0])"
Write-Host 'Publish order: scan -> push -> signed provenance'
