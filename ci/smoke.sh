#!/usr/bin/env bash
# Läuft auf dem virtuellen Handy (android-emulator-runner): installiert die App,
# klickt den Kletterweg mit ClimbSmokeTest durch und legt Ergebnis, Logs und
# Bildschirmfotos auf den Branch "ci-smoke".
#
# Zwei Durchläufe: erst alle Tests außer dem KI-Test, dann der KI-Test mit einem echten
# Sprachmodell (dauert im Emulator lange). Nach jedem Durchlauf wird veröffentlicht,
# damit Ergebnisse auch dann ankommen, wenn der KI-Test die Zeit überschreitet.
set -uo pipefail
cd "$(dirname "$0")/.."
source ci/publish-branch.sh

PKG="io.github.tro123n.mentalmathplus"
RUNNER="${PKG}.test/androidx.test.runner.AndroidJUnitRunner"
CLASS="com.helddertierwelt.mentalmath.ClimbSmokeTest"
OUT=smoke-out
mkdir -p "$OUT/screens"

APP="$(ls app/build/outputs/apk/foss/debug/*.apk 2>/dev/null | head -n 1)"
TEST="$(ls app/build/outputs/apk/androidTest/foss/debug/*.apk 2>/dev/null | head -n 1)"
SHOTS="$(grep -v '^#' ci/smoke-shots.txt 2>/dev/null | tr -s ' \n' ',' | sed 's/^,//; s/,$//')"
[ -n "$SHOTS" ] || SHOTS="all"

status_main=1
status_ai=1
ai_state="nicht gelaufen"

pull_results() {
  for f in $(timeout 30 adb shell run-as "$PKG" ls files/screens 2>/dev/null | tr -d '\r'); do
    timeout 30 adb exec-out run-as "$PKG" cat "files/screens/$f" > "$OUT/screens/$f" || true
  done
  timeout 60 adb logcat -d -v time '*:E' > "$OUT/logcat-errors.txt" 2>&1 || true
  timeout 60 adb logcat -d -v time -s ClimbSmokeTest:V AndroidRuntime:V TestRunner:V AiEngine:V ModelStore:V litert:V > "$OUT/logcat-test.txt" 2>&1 || true
}

write_summary() {
  {
    echo "commit: ${GITHUB_SHA:-?}"
    echo "run: ${RUN_NUMBER:-?}"
    echo "shots: ${SHOTS}"
    if [ "$status_main" -eq 0 ]; then echo "Ergebnis: OK"; else echo "Ergebnis: FEHLER"; fi
    echo "KI-Test: $ai_state"
    echo "Bilder: $(ls "$OUT/screens" | wc -l)"
    [ -f "$OUT/apk-info.txt" ] && cat "$OUT/apk-info.txt"
    [ -f "$OUT/model-info.txt" ] && cat "$OUT/model-info.txt"
    echo
    echo "----- Tests ohne KI -----"
    tail -n 60 "$OUT/instrument.txt" 2>/dev/null
    if [ -f "$OUT/instrument-ki.txt" ]; then
      echo
      echo "----- KI-Test -----"
      tail -n 60 "$OUT/instrument-ki.txt"
    fi
  } > "$OUT/summary.txt"
}

if [ -n "$APP" ] && [ -n "$TEST" ]; then
  adb wait-for-device
  # Fehlerdialoge des Emulators („Pixel Launcher reagiert nicht“) unterdrücken.
  adb shell settings put global hide_error_dialogs 1 || true
  adb shell am force-stop com.google.android.apps.nexuslauncher || true
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1 || true
  if adb install -r -g "$APP" && adb install -r -g "$TEST"; then
    adb shell cmd locale set-app-locales "$PKG" --locales de-DE || true
    # KI-Modell für den KI-Test in den App-Ordner legen (wie nach einem Download).
    MODEL="smoke-model/gemma-4-E2B-it.litertlm"
    have_model=0
    if [ -f "$MODEL" ]; then
      MDIR="/sdcard/Android/data/$PKG/files/models"
      adb shell mkdir -p "$MDIR" || true
      if timeout 600 adb push "$MODEL" "$MDIR/" > /dev/null; then
        echo "KI-Modell aufgespielt."
        have_model=1
      else
        echo "KI-Modell konnte nicht aufgespielt werden."
      fi
      adb shell ls -l "$MDIR" || true
      adb shell df -h /data | tail -n 1 || true
      rm -f "$MODEL"
    fi

    # 1) Alle Tests außer dem KI-Gespräch.
    adb logcat -c || true
    timeout -k 30 2700 adb shell am instrument -w \
      -e class "$CLASS" \
      -e notClass "$CLASS#l_kiCoach" \
      -e shots "$SHOTS" \
      "$RUNNER" 2>&1 | tee "$OUT/instrument.txt"
    grep -q "^OK (" "$OUT/instrument.txt" && status_main=0
    pull_results
    write_summary
    publish_branch ci-smoke "$OUT" "Smoke-Test Lauf ${RUN_NUMBER:-?} (ohne KI)"

    # 2) KI-Test mit dem echten Modell (falls aufgespielt).
    if [ "$have_model" -eq 1 ]; then
      ai_state="läuft"
      timeout -k 30 2700 adb shell am instrument -w \
        -e class "$CLASS#l_kiCoach" \
        -e keepScreens true \
        -e shots "$SHOTS" \
        "$RUNNER" 2>&1 | tee "$OUT/instrument-ki.txt"
      if grep -q "^OK (" "$OUT/instrument-ki.txt"; then
        status_ai=0
        ai_state="OK"
      else
        ai_state="FEHLER oder Zeit überschritten"
      fi
      timeout 30 adb shell ls -l "/sdcard/Android/data/$PKG/files/models" > "$OUT/models.txt" 2>&1 || true
      timeout 30 adb shell df -h /data >> "$OUT/models.txt" 2>&1 || true
      timeout 30 adb shell dumpsys meminfo "$PKG" > "$OUT/meminfo.txt" 2>&1 || true
      pull_results
    else
      ai_state="übersprungen (kein Modell)"
    fi
  else
    echo "Installation fehlgeschlagen." > "$OUT/instrument.txt"
  fi
else
  echo "APKs fehlen." > "$OUT/instrument.txt"
fi

write_summary
cat "$OUT/summary.txt"
publish_branch ci-smoke "$OUT" "Smoke-Test Lauf ${RUN_NUMBER:-?}"
# Der KI-Test im Emulator hängt von einem großen Modell ab; er bricht den Lauf nicht ab,
# sein Ergebnis steht in summary.txt („KI-Test: …“).
exit "$status_main"
