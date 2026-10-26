$ErrorActionPreference = "Stop"
& .\gradlew.bat ":app:assembleDebug" ":app:testDebugUnitTest" ":app:compileDebugAndroidTestKotlin"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& .\gradlew.bat ":app:lintDebug"
Write-Host "verify: OK"
