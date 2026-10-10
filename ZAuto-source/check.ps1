$ErrorActionPreference="Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$apk=Join-Path $PSScriptRoot "build/ZAuto.apk"
$zip=[IO.Compression.ZipFile]::OpenRead($apk)
try {
    foreach($name in @("AndroidManifest.xml","classes.dex")) {
        if(!$zip.GetEntry($name)){throw "Missing $name"}
    }
    if($zip.Entries.FullName -match '\.(py|jks|env)$'){throw "Unexpected private/source file in APK"}
} finally {$zip.Dispose()}
$metadata=& C:\Android\sdk\build-tools\35.0.0\aapt.exe dump badging $apk
if($LASTEXITCODE -ne 0){throw "Cannot parse APK"}
foreach($expected in @("package: name='vn.zauto'","sdkVersion:'26'","targetSdkVersion:'35'","android.permission.SYSTEM_ALERT_WINDOW","android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION","launchable-activity: name='vn.zauto.MainActivity'")) {
    if(!($metadata | Select-String -SimpleMatch $expected)){throw "Missing APK property: $expected"}
}
& C:\Android\sdk\build-tools\35.0.0\apksigner.bat verify $apk
if($LASTEXITCODE -ne 0){throw "Invalid signature"}
Write-Output "PASS: APK contents, SDK versions, entry activity, permissions, and signature."
