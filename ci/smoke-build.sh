#!/usr/bin/env bash
# Baut die Debug-APK und die Test-APK für den Test auf dem virtuellen Handy.
set -uo pipefail
cd "$(dirname "$0")/.."
source ci/publish-branch.sh

OUT=smoke-out
rm -rf "$OUT"
mkdir -p "$OUT"
chmod +x ./gradlew
./gradlew --no-daemon --stacktrace :app:assembleFossDebug :app:assembleFossDebugAndroidTest 2>&1 | tee "$OUT/build.log"
status=${PIPESTATUS[0]}
if [ "$status" -ne 0 ]; then
  {
    echo "commit: ${GITHUB_SHA:-?}"
    echo "run: ${RUN_NUMBER:-?}"
    echo "Ergebnis: BUILD FEHLGESCHLAGEN"
    echo
    grep -E "^e: |What went wrong|FAILED|Execution failed" "$OUT/build.log" | head -n 120
    echo
    echo "----- letzte 300 Zeilen -----"
    tail -n 300 "$OUT/build.log"
  } > "$OUT/summary.txt"
  rm -f "$OUT/build.log"
  publish_branch ci-smoke "$OUT" "Smoke-Build fehlgeschlagen (Lauf ${RUN_NUMBER:-?})"
  exit "$status"
fi
rm -f "$OUT/build.log"
