param(
  [string]$BaseUrl = "http://127.0.0.1:8080",
  [string]$DeviceCode = "TEMP-HUM-001",
  [int]$Count = 5,
  [int]$IntervalSeconds = 2,
  [switch]$IncludeAbnormal
)

$ErrorActionPreference = "Stop"

for ($i = 1; $i -le $Count; $i++) {
  $isAbnormal = $IncludeAbnormal -and ($i % 3 -eq 0)
  $temperature = if ($isAbnormal) { 86 + (Get-Random -Minimum 0 -Maximum 6) } else { 24 + (Get-Random -Minimum 0 -Maximum 8) }
  $humidity = 45 + (Get-Random -Minimum 0 -Maximum 20)
  $voltage = if ($isAbnormal) { 198 + (Get-Random -Minimum 0 -Maximum 8) } else { 220 + (Get-Random -Minimum 0 -Maximum 10) }
  $signal = if ($isAbnormal) { -101 + (Get-Random -Minimum 0 -Maximum 5) } else { -70 + (Get-Random -Minimum 0 -Maximum 12) }

  $payload = @{
    deviceCode = $DeviceCode
    temperature = $temperature
    humidity = $humidity
    voltage = $voltage
    signalStrength = $signal
    status = if ($isAbnormal) { "ABNORMAL" } else { "NORMAL" }
    message = if ($isAbnormal) { "simulated abnormal report" } else { "simulated normal report" }
    reportedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
  }

  $json = $payload | ConvertTo-Json -Depth 5
  $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/device-reports" -ContentType "application/json" -Body $json
  $data = $response.data
  Write-Host "[$i/$Count] reportId=$($data.id) status=$($data.status) abnormal=$($data.abnormal) generatedLogId=$($data.generatedLogId)"

  if ($i -lt $Count) {
    Start-Sleep -Seconds $IntervalSeconds
  }
}
