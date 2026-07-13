param(
  [string]$BrokerHost = "127.0.0.1",
  [int]$BrokerPort = 1883,
  [string]$DeviceCode = "XIAOZHI-001",
  [string]$EventType = "firmware_started",
  [string]$Level = "INFO",
  [string]$Message = "Firmware initialization completed"
)

$ErrorActionPreference = "Stop"

function Find-MosquittoPub {
  $command = Get-Command "mosquitto_pub.exe" -ErrorAction SilentlyContinue
  if ($command) {
    return $command.Source
  }
  foreach ($candidate in @(
    "C:\Program Files\mosquitto\mosquitto_pub.exe",
    "C:\Program Files (x86)\mosquitto\mosquitto_pub.exe"
  )) {
    if (Test-Path $candidate) {
      return $candidate
    }
  }
  return $null
}

$publisher = Find-MosquittoPub
if (-not $publisher) {
  throw "mosquitto_pub.exe not found."
}

$payload = @{
  deviceCode = $DeviceCode
  eventType = $EventType
  level = $Level
  message = $Message
  reportedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
}
$json = $payload | ConvertTo-Json -Compress
$topic = "aiot/device/$DeviceCode/log"
$tempFile = Join-Path $env:TEMP "aiot-mqtt-runtime-log-$([guid]::NewGuid().ToString('N')).json"

try {
  $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
  [System.IO.File]::WriteAllText($tempFile, $json, $utf8NoBom)
  & $publisher -h $BrokerHost -p $BrokerPort -t $topic -q 1 -f $tempFile
  if ($LASTEXITCODE -ne 0) {
    throw "mosquitto_pub failed with exit code $LASTEXITCODE"
  }
  Write-Host "Published MQTT runtime log to $topic"
  Write-Host $json
}
finally {
  if (Test-Path $tempFile) {
    Remove-Item -LiteralPath $tempFile -Force
  }
}
