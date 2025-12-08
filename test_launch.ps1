$ErrorActionPreference = "Stop"

Write-Host "=== STEP 1: Killing running emulators ===" -ForegroundColor Cyan
adb emu kill
Start-Sleep -Seconds 5

Write-Host "=== STEP 2: Starting Emulator (Pixel_6_Pro_x86-64) with 3GB RAM limit ===" -ForegroundColor Cyan
# Запускаем эмулятор в фоне этого процесса
Start-Process "G:\AndroidStudio\emulator\emulator.exe" -ArgumentList "-avd", "Pixel_6_Pro_x86-64", "-memory", "3072", "-no-snapshot-load" -NoNewWindow

Write-Host "Waiting for ADB connection..." -ForegroundColor Yellow
adb wait-for-device

Write-Host "=== STEP 3: Waiting for Android System Boot ===" -ForegroundColor Cyan
$booted = $false
$timeout = [DateTime]::Now.AddMinutes(5)

while (-not $booted) {
    if ([DateTime]::Now -gt $timeout) {
        Write-Error "Timeout waiting for emulator to boot!"
        exit 1
    }
    
    try {
        $status = adb shell getprop sys.boot_completed 2>$null
        if ($status -like "*1*") {
            $booted = $true
            Write-Host "`nAndroid Booted!" -ForegroundColor Green
        } else {
            Write-Host -NoNewline "."
            Start-Sleep -Seconds 3
        }
    } catch {
        Write-Host -NoNewline "!"
        Start-Sleep -Seconds 3
    }
}

# Даем системе продышаться после бута
Start-Sleep -Seconds 10

Write-Host "=== STEP 4: Starting UI Walker (Local LLM) ===" -ForegroundColor Cyan
Write-Host "Logs will be saved to: debug_local_final.log" -ForegroundColor Gray

# Запускаем скрипт, дублируя вывод в консоль и в файл
python ui_walker.py --instruction_file instruction.txt --max_steps 20 --local | Tee-Object -FilePath "debug_local_final.log"

Write-Host "`n=== TEST COMPLETED ===" -ForegroundColor Green
Read-Host "Press Enter to close this window..."