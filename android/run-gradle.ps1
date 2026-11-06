param(
    [string]$Task = "assembleDebug"
)

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
$env:ANDROID_HOME = "C:\Users\Shrio\tools\android-sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

$gradle = "C:\Users\Shrio\tools\gradle-8.7\bin\gradle.bat"

Write-Host "Running Gradle with JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_HOME=$env:ANDROID_HOME"
Write-Host "Task: $Task"

Set-Location "C:\Users\Shrio\Attendance-System\android"
& $gradle --no-daemon $Task --stacktrace
