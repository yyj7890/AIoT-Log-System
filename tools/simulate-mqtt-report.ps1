param(
  [string]$BrokerHost = "127.0.0.1",
  [int]$BrokerPort = 1883,
  [string]$DeviceCode = "TEMP-HUM-001",
  [switch]$Abnormal
)

$ErrorActionPreference = "Stop"

function Find-MosquittoPub {
  $command = Get-Command "mosquitto_pub.exe" -ErrorAction SilentlyContinue
  if ($command) {
    return $command.Source
  }

  $candidates = @(
    "C:\Program Files\mosquitto\mosquitto_pub.exe",
    "C:\Program Files (x86)\mosquitto\mosquitto_pub.exe"
  )
  foreach ($candidate in $candidates) {
    if (Test-Path $candidate) {
      return $candidate
    }
  }

  return $null
}

$publisher = Find-MosquittoPub
if (-not $publisher) {
  throw "mosquitto_pub.exe not found. Install Mosquitto first, or add mosquitto_pub.exe to PATH."
}

$payload = @{
  deviceCode = $DeviceCode
  temperature = if ($Abnormal) { 90 } else { 28 }
  humidity = if ($Abnormal) { 65 } else { 55 }
  voltage = if ($Abnormal) { 198 } else { 222 }
  signalStrength = if ($Abnormal) { -99 } else { -70 }
  status = if ($Abnormal) { "ABNORMAL" } else { "NORMAL" }
  message = if ($Abnormal) { "mqtt simulated abnormal report" } else { "mqtt simulated normal report" }
  reportedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
}

$json = $payload | ConvertTo-Json -Depth 5 -Compress
$topic = "aiot/device/$DeviceCode/report"

$tempFile = Join-Path $env:TEMP "aiot-mqtt-report-$([guid]::NewGuid().ToString('N')).json"
try {
  $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
  [System.IO.File]::WriteAllText($tempFile, $json, $utf8NoBom)
  & $publisher -h $BrokerHost -p $BrokerPort -t $topic -q 1 -f $tempFile
  if ($LASTEXITCODE -ne 0) {
    throw "mosquitto_pub failed with exit code $LASTEXITCODE"
  }
  Write-Host "Published MQTT report to $topic"
  Write-Host $json
}
finally {
  if (Test-Path $tempFile) {
    Remove-Item -LiteralPath $tempFile -Force
  }
}
