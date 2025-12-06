$adb = "G:\AndroidStudio\platform-tools\adb.exe"
$package = "org.bxkr.octodiary.debug"
$activity = "org.bxkr.octodiary.MainActivity"
$outputDir = "test_results"

if (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

Function Test-Launch {
    param (
        [string]$Name,
        [string]$Command
    )
    
    Write-Host "Testing: $Name"
    
    # Clear logcat
    & $adb logcat -c
    
    # Start Logcat capture in background
    $logFile = "$outputDir\log_$Name.txt"
    $logProcess = Start-Process -FilePath $adb -ArgumentList "logcat -v time" -RedirectStandardOutput $logFile -PassThru -NoNewWindow
    
    # Launch App
    Invoke-Expression "& `"$adb`" shell $Command"
    
    # Wait for launch
    Start-Sleep -Seconds 15
    
    # Take Screenshot
    $screenshotPath = "/sdcard/screen_$Name.png"
    & $adb shell screencap -p $screenshotPath
    & $adb pull $screenshotPath "$outputDir\screen_$Name.png"
    & $adb shell rm $screenshotPath
    
    # Stop Logcat
    Stop-Process -Id $logProcess.Id -Force
    
    # Force stop app to clean state
    & $adb shell am force-stop $package
    
    Write-Host "Finished: $Name"
    Write-Host "--------------------------------------------------"
}

# 1. Main Activity Launch
Test-Launch -Name "MainActivity" -Command "am start -n $package/$activity"

# 2. Deep Link: dnevnik-mes
Test-Launch -Name "DeepLink_Dnevnik" -Command "am start -a android.intent.action.VIEW -d `"dnevnik-mes://test`""

# 3. Deep Link: octodiary
Test-Launch -Name "DeepLink_OctoDiary" -Command "am start -a android.intent.action.VIEW -d `"octodiary://debug`""

# 4. Deep Link: rt.schoolboy.app
Test-Launch -Name "DeepLink_RT" -Command "am start -a android.intent.action.VIEW -d `"rt.schoolboy.app://test`""

Write-Host "All tests completed. Check $outputDir for logs and screenshots."
