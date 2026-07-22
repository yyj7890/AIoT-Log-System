param(
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$FrontendUrl = "http://127.0.0.1:5173/"

function Test-Port {
    param(
        [string]$HostName,
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        $connected = $async.AsyncWaitHandle.WaitOne(1000, $false)
        if ($connected) {
            $client.EndConnect($async)
            return $true
        }
        return $false
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Wait-Port {
    param(
        [string]$Name,
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutSeconds = 45,
        [int]$PollIntervalMilliseconds = 200
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port -HostName $HostName -Port $Port) {
            Write-Host "$Name is ready on $HostName`:$Port"
            return
        }
        Start-Sleep -Milliseconds $PollIntervalMilliseconds
    }

    throw "$Name did not become ready on $HostName`:$Port within $TimeoutSeconds seconds."
}

function Start-MySql {
    if (Test-Port -HostName "127.0.0.1" -Port 3306) {
        Write-Host "MySQL already running on 127.0.0.1:3306"
        return
    }

    $mysqld = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe"
    $args = '--basedir="C:\Program Files\MySQL\MySQL Server 8.4" --datadir="' + $Root + '\mysql-data" --port=3306 --bind-address=127.0.0.1'

    Write-Host "Starting MySQL..."
    Start-Process -FilePath $mysqld -ArgumentList $args -WindowStyle Hidden | Out-Null
    Wait-Port -Name "MySQL" -HostName "127.0.0.1" -Port 3306 -TimeoutSeconds 45
}

function Ensure-Database {
    $mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"

    if (-not (Test-Path $mysql)) {
        Write-Host "MySQL client not found, skipping database creation check."
        return
    }

    Write-Host "Ensuring application database exists..."
    & $mysql --protocol=tcp --host=127.0.0.1 --user=root --password=root --execute="CREATE DATABASE IF NOT EXISTS aiot_log_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not create or access the aiot_log_system database."
    }
    Write-Host "Application database is ready; Flyway will validate or migrate its tables when the backend starts."
}

function Start-Backend {
    if (Test-Port -HostName "127.0.0.1" -Port 8080) {
        Write-Host "Backend already running on 127.0.0.1:8080"
        return
    }

    $backendDir = Join-Path $Root "backend"
    $credentialsFile = Join-Path $Root "config\mqtt-credentials.env"
    if (Test-Path $credentialsFile) {
        Get-Content $credentialsFile | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
                $parts = $line.Split("=", 2)
                [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1], "Process")
            }
        }
        Write-Host "Loaded local MQTT credentials for backend."
    }
    $stdout = Join-Path $backendDir "backend-run.log"
    $stderr = Join-Path $backendDir "backend-run.err.log"

    Write-Host "Starting backend..."
    Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c", "run-backend.cmd" `
        -WorkingDirectory $backendDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr | Out-Null

    Wait-Port -Name "Backend" -HostName "127.0.0.1" -Port 8080 -TimeoutSeconds 90
}

function Find-Mosquitto {
    $command = Get-Command "mosquitto.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @(
        "C:\Program Files\mosquitto\mosquitto.exe",
        "C:\Program Files (x86)\mosquitto\mosquitto.exe"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    return $null
}

function Start-MqttBroker {
    if (Test-Port -HostName "127.0.0.1" -Port 1883) {
        Write-Host "MQTT broker already running on 127.0.0.1:1883"
        return
    }

    $mosquitto = Find-Mosquitto
    if (-not $mosquitto) {
        Write-Host "Mosquitto not found, skipping MQTT broker startup."
        return
    }

    $mosquittoConfig = Join-Path $Root "config\mosquitto-lan.conf"
    if (-not (Test-Path $mosquittoConfig)) {
        throw "Mosquitto LAN config not found: $mosquittoConfig"
    }

    Write-Host "Starting MQTT broker..."
    Start-Process -FilePath $mosquitto -ArgumentList @("-c", $mosquittoConfig) -WindowStyle Hidden | Out-Null
    Wait-Port -Name "MQTT broker" -HostName "127.0.0.1" -Port 1883 -TimeoutSeconds 20
}

function Start-Frontend {
    if (Test-Port -HostName "127.0.0.1" -Port 5173) {
        Write-Host "Frontend already running on 127.0.0.1:5173"
        return
    }

    $frontendDir = Join-Path $Root "frontend"
    $stdout = Join-Path $frontendDir "frontend-run.log"
    $stderr = Join-Path $frontendDir "frontend-run.err.log"

    Write-Host "Starting frontend..."
    Start-Process -FilePath "npm.cmd" `
        -ArgumentList "run", "dev" `
        -WorkingDirectory $frontendDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr | Out-Null

    Wait-Port -Name "Frontend" -HostName "127.0.0.1" -Port 5173 -TimeoutSeconds 60
}

function Open-Frontend {
    param(
        [string]$Url
    )

    Write-Host "Opening $Url"

    try {
        Start-Process -FilePath "explorer.exe" -ArgumentList $Url | Out-Null
        return
    }
    catch {
        Write-Host "explorer.exe could not open the browser, trying url.dll..."
    }

    try {
        Start-Process -FilePath "rundll32.exe" -ArgumentList "url.dll,FileProtocolHandler", $Url | Out-Null
        return
    }
    catch {
        Write-Host "url.dll could not open the browser, trying cmd start..."
    }

    try {
        & cmd.exe /c start "" $Url
        return
    }
    catch {
        Write-Host "Could not open browser automatically. Please open this URL manually:"
        Write-Host $Url
    }
}

Start-MySql
Ensure-Database
Start-MqttBroker
Start-Backend
Start-Frontend

if (-not $NoBrowser) {
    Open-Frontend -Url $FrontendUrl
}

Write-Host "All services are ready."
Write-Host "Frontend: $FrontendUrl"
Write-Host "Backend:  http://127.0.0.1:8080/"
Write-Host "MySQL:    127.0.0.1:3306"
Write-Host "MQTT:     127.0.0.1:1883"
