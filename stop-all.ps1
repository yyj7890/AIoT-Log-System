param(
    [switch]$KeepMySql
)

$ErrorActionPreference = "Stop"

function Get-PortProcessIds {
    param(
        [int]$Port
    )

    $lines = netstat -ano | Select-String ":$Port"
    $ids = New-Object System.Collections.Generic.HashSet[int]

    foreach ($line in $lines) {
        $text = $line.ToString().Trim()
        if ($text -notmatch "\sLISTENING\s+(\d+)$") {
            continue
        }

        [void]$ids.Add([int]$Matches[1])
    }

    return @($ids)
}

function Stop-PortProcess {
    param(
        [string]$Name,
        [int]$Port
    )

    $processIds = Get-PortProcessIds -Port $Port
    if ($processIds.Count -eq 0) {
        Write-Host "$Name is not running on port $Port"
        return
    }

    foreach ($processId in $processIds) {
        try {
            $process = Get-Process -Id $processId -ErrorAction Stop
            Write-Host "Stopping $Name on port $Port, PID $processId ($($process.ProcessName))..."
            Stop-Process -Id $processId -Force -ErrorAction Stop
            Write-Host "$Name stopped."
        }
        catch {
            Write-Host "Could not stop $Name PID $processId. Try running stop-all.cmd as administrator."
        }
    }
}

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

function Stop-MySqlGracefully {
    if (-not (Test-Port -HostName "127.0.0.1" -Port 3306)) {
        Write-Host "MySQL is not running on port 3306"
        return
    }

    $mysqlAdmin = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqladmin.exe"
    if (Test-Path $mysqlAdmin) {
        try {
            Write-Host "Stopping MySQL gracefully with mysqladmin..."
            & $mysqlAdmin --protocol=tcp --host=127.0.0.1 --user=root --password=root shutdown
            for ($i = 0; $i -lt 15; $i++) {
                if (-not (Test-Port -HostName "127.0.0.1" -Port 3306)) {
                    Write-Host "MySQL stopped."
                    return
                }
                Start-Sleep -Seconds 1
            }
            Write-Host "MySQL did not stop after mysqladmin shutdown."
        }
        catch {
            Write-Host "mysqladmin shutdown failed. Try running stop-all.cmd as administrator."
        }
    }

    Stop-PortProcess -Name "MySQL" -Port 3306
}

Stop-PortProcess -Name "Frontend" -Port 5173
Stop-PortProcess -Name "Backend" -Port 8080
Stop-PortProcess -Name "MQTT broker" -Port 1883

if ($KeepMySql) {
    Write-Host "Keeping MySQL running because -KeepMySql was specified."
}
else {
    Stop-MySqlGracefully
}

Write-Host "Stop command finished."
