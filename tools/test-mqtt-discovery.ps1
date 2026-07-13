param(
  [Parameter(Mandatory = $true)]
  [string]$TargetHost,
  [int]$Port = 19830,
  [string]$DeviceCode = "XIAOZHI-001",
  [string]$Mac = "AA:BB:CC:DD:EE:FF",
  [string]$Token = ""
)

$ErrorActionPreference = "Stop"

$nonce = [Guid]::NewGuid().ToString("N")
$request = @{
  protocol = "aiot-mqtt-discovery-v1"
  deviceCode = $DeviceCode
  mac = $Mac
  nonce = $nonce
  token = $Token
} | ConvertTo-Json -Compress

$client = [System.Net.Sockets.UdpClient]::new()
$client.Client.ReceiveTimeout = 3000
try {
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($request)
  [void]$client.Send($bytes, $bytes.Length, $TargetHost, $Port)

  $remote = [System.Net.IPEndPoint]::new([System.Net.IPAddress]::Any, 0)
  $responseBytes = $client.Receive([ref]$remote)
  $response = [System.Text.Encoding]::UTF8.GetString($responseBytes) | ConvertFrom-Json

  if ($response.protocol -ne "aiot-mqtt-discovery-v1" -or $response.nonce -ne $nonce) {
    throw "Discovery response protocol or nonce did not match the request."
  }

  Write-Host "Discovery response from $($remote.Address):$($remote.Port)"
  $response | ConvertTo-Json -Depth 5
}
finally {
  $client.Dispose()
}
