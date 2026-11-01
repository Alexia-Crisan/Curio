#!/usr/bin/env bash
set -u
echo "== Placeholders owned by Developer B =="
grep -rln "TEMP placeholder" app/src/main || true
echo "== Open TODO markers =="
grep -rn "TODO(" app/src/main app/src/debug 2>/dev/null || true
echo "== Room schema version =="
grep -h '"version"' app/schemas/*/*.json 2>/dev/null || echo "no exported schema found"
echo "== INTERNET permission =="
grep -n "android.permission.INTERNET" app/src/main/AndroidManifest.xml || echo "not declared"
echo "== Unit test result files =="
find app/build/test-results -name "*.xml" 2>/dev/null | wc -l
echo "== Files still holding the old prototype names =="
grep -rln "InteractionRepository" app/src/main || true
