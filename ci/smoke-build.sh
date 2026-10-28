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

# Release-Build (wie für das Release, nur ohne Signatur): prüft R8 vorab und zeigt Größe und ABIs.
./gradlew --no-daemon --stacktrace :app:assembleFossRelease 2>&1 | tee "$OUT/release.log"
rstatus=${PIPESTATUS[0]}
REL="$(ls app/build/outputs/apk/foss/release/*.apk 2>/dev/null | head -n 1)"
{
  echo "Release-Build: $([ "$rstatus" -eq 0 ] && echo OK || echo FEHLGESCHLAGEN)"
  if [ -n "$REL" ]; then
    echo "APK: $(basename "$REL") $(du -h "$REL" | cut -f1)"
    unzip -l "$REL" | grep -E "\.so$" | awk '{printf "%8.1f MB  %s\n", $1/1048576, $4}'
  else
    grep -E "^e: |What went wrong|FAILED|Execution failed|R8|Missing class" "$OUT/release.log" | head -n 80
  fi
} > "$OUT/apk-info.txt"
cat "$OUT/apk-info.txt"
rm -f "$OUT/release.log"

# KI-Test: das kleine Gemma-4-Modell (E2B, 2,6 GB) für das virtuelle Handy laden und prüfen.
# Klappt der Download nicht, läuft der Test ohne KI-Gespräch weiter.
MODEL_DIR=smoke-model
MODEL_FILE=gemma-4-E2B-it.litertlm
MODEL_URL="https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1/${MODEL_FILE}?download=true"
MODEL_SHA=181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c
rm -rf "$MODEL_DIR"
mkdir -p "$MODEL_DIR"
df -h . | tail -n 1
if curl -L --fail --retry 3 --retry-delay 10 -sS -o "$MODEL_DIR/$MODEL_FILE" "$MODEL_URL"; then
  if echo "$MODEL_SHA  $MODEL_DIR/$MODEL_FILE" | sha256sum -c --status; then
    echo "KI-Modell geladen: $(du -h "$MODEL_DIR/$MODEL_FILE" | cut -f1)"
  else
    echo "KI-Modell: Prüfsumme falsch – ohne Modell weiter."
    rm -f "$MODEL_DIR/$MODEL_FILE"
  fi
else
  echo "KI-Modell konnte nicht geladen werden – ohne Modell weiter."
  rm -f "$MODEL_DIR/$MODEL_FILE"
fi
df -h . | tail -n 1
