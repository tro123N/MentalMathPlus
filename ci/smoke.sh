#!/usr/bin/env bash
# Läuft auf dem virtuellen Handy (android-emulator-runner): installiert die App,
# klickt den Kletterweg mit ClimbSmokeTest durch und legt Ergebnis, Logs und
# Bildschirmfotos auf den Branch "ci-smoke".
set -uo pipefail
cd "$(dirname "$0")/.."
source ci/publish-branch.sh

PKG="io.github.tro123n.mentalmathplus"
RUNNER="${PKG}.test/androidx.test.runner.AndroidJUnitRunner"
OUT=smoke-out
mkdir -p "$OUT/screens"

APP="$(ls app/build/outputs/apk/foss/debug/*.apk 2>/dev/null | head -n 1)"
TEST="$(ls app/build/outputs/apk/androidTest/foss/debug/*.apk 2>/dev/null | head -n 1)"
SHOTS="$(grep -v '^#' ci/smoke-shots.txt 2>/dev/null | tr -s ' \n' ',' | sed 's/^,//; s/,$//')"
[ -n "$SHOTS" ] || SHOTS="all"

status=1
if [ -n "$APP" ] && [ -n "$TEST" ]; then
  adb wait-for-device
  # Fehlerdialoge des Emulators („Pixel Launcher reagiert nicht“) unterdrücken.
  adb shell settings put global hide_error_dialogs 1 || true
  adb shell am force-stop com.google.android.apps.nexuslauncher || true
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1 || true
  adb install -r -g "$APP" && adb install -r -g "$TEST" && {
    adb shell cmd locale set-app-locales "$PKG" --locales de-DE || true
    adb logcat -c || true
    timeout 2400 adb shell am instrument -w \
      -e class com.helddertierwelt.mentalmath.ClimbSmokeTest \
      -e shots "$SHOTS" \
      "$RUNNER" 2>&1 | tee "$OUT/instrument.txt"
    grep -q "^OK (" "$OUT/instrument.txt" && status=0
  }
  for f in $(adb shell run-as "$PKG" ls files/screens 2>/dev/null | tr -d '\r'); do
    adb exec-out run-as "$PKG" cat "files/screens/$f" > "$OUT/screens/$f"
  done
  adb logcat -d -v time '*:E' > "$OUT/logcat-errors.txt" 2>&1 || true
  adb logcat -d -v time -s ClimbSmokeTest:V AndroidRuntime:V TestRunner:V > "$OUT/logcat-test.txt" 2>&1 || true
else
  echo "APKs fehlen." > "$OUT/instrument.txt"
fi

{
  echo "commit: ${GITHUB_SHA:-?}"
  echo "run: ${RUN_NUMBER:-?}"
  echo "shots: ${SHOTS}"
  if [ "$status" -eq 0 ]; then echo "Ergebnis: OK"; else echo "Ergebnis: FEHLER"; fi
  echo "Bilder: $(ls "$OUT/screens" | wc -l)"
  echo
  tail -n 80 "$OUT/instrument.txt"
} > "$OUT/summary.txt"
cat "$OUT/summary.txt"
publish_branch ci-smoke "$OUT" "Smoke-Test Lauf ${RUN_NUMBER:-?}"
exit "$status"
