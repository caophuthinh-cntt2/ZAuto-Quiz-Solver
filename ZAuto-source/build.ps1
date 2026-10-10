param([string]$Sdk="C:\Android\sdk",[string]$Jdk="C:\Program Files\Java\jdk-21")
$ErrorActionPreference="Stop"
$env:JAVA_HOME=$Jdk
$env:PATH="$Jdk\bin;$env:PATH"
Set-Location $PSScriptRoot
$bt=Join-Path $Sdk "build-tools\35.0.0"
$android=Join-Path $Sdk "platforms\android-36\android.jar"
New-Item -ItemType Directory -Force build/classes,build/dex | Out-Null
function Check { if($LASTEXITCODE -ne 0){throw "Build failed: $LASTEXITCODE"} }
# javac requires source without a UTF-8 BOM.
Get-ChildItem src -Recurse -Filter *.java | ForEach-Object {
    $content=[IO.File]::ReadAllText($_.FullName)
    [IO.File]::WriteAllText($_.FullName,$content,[Text.UTF8Encoding]::new($false))
}
$sources=(Get-ChildItem src -Recurse -Filter *.java).FullName
& "$Jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -classpath $android -d build/classes $sources
Check
& "$Jdk\bin\jar.exe" cf build/classes.jar -C build/classes .
Check
& "$bt\d8.bat" --lib $android --min-api 26 --output build/dex build/classes.jar
Check
& "$bt\aapt2.exe" link -I $android --manifest AndroidManifest.xml -o build/unsigned.apk
Check
& "$Jdk\bin\jar.exe" uf build/unsigned.apk -C build/dex classes.dex
Check
& "$bt\zipalign.exe" -f 4 build/unsigned.apk build/aligned.apk
Check
if(!(Test-Path build/local.jks)) {
    & "$Jdk\bin\keytool.exe" -genkeypair -keystore build/local.jks -storepass android -keypass android -alias zauto -dname "CN=ZAuto Local" -keyalg RSA -keysize 2048 -validity 10000
    Check
}
& "$bt\apksigner.bat" sign --ks build/local.jks --ks-pass pass:android --out build/ZAuto.apk build/aligned.apk
Check
& "$bt\apksigner.bat" verify --verbose build/ZAuto.apk
Check
