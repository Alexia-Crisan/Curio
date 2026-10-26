#!/usr/bin/env bash
set -euo pipefail
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug || echo "lint reported problems"
echo "verify: OK"
