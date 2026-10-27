#!/usr/bin/env bash
# Baut die signierte APK, führt die Tests aus und veröffentlicht ein Release.
# Wird von .github/workflows/build.yml aufgerufen.
set -uo pipefail
cd "$(dirname "$0")/.."

RUN="${RUN_NUMBER:-0}"
VERSION="1.0.${RUN}"
TAG="v${VERSION}"
APK_NAME="MentalMathPlus.apk"
LOG="build.log"

report_failure() {
  echo "::group::Fehlerzusammenfassung"
  grep -E "^e: |error:|FAILURE|What went wrong|> Task .* FAILED|Execution failed|Could not|Exception" "$LOG" | head -n 80 || true
  echo "::endgroup::"
  # Kotlin-Fehler als Annotation sichtbar machen (e: file:///pfad/Datei.kt:12:5 Meldung)
  grep -E "^e: file://" "$LOG" | head -n 40 | while IFS= read -r line; do
    loc="${line#e: file://}"
    file="${loc%%:*}"; rest="${loc#*:}"
    ln="${rest%%:*}"; rest="${rest#*:}"
    col="${rest%% *}"; msg="${rest#* }"
    rel="${file#"$PWD"/}"
    echo "::error file=${rel},line=${ln},col=${col}::${msg}"
  done
  # Log auf den Branch "ci-logs" legen, damit er ohne Anmeldung lesbar ist.
  if [ -n "${GITHUB_TOKEN:-${GH_TOKEN:-}}" ] && [ -n "${GITHUB_REPOSITORY:-}" ]; then
    tmp="$(mktemp -d)"
    cp "$LOG" "$tmp/build.log"
    (
      cd "$tmp" &&
      git init -q &&
      git checkout -q -b ci-logs &&
      mkdir -p logs &&
      { echo "commit: ${GITHUB_SHA:-?}"; echo "run: ${RUN}"; echo; grep -E "^e: |^w: .*error|What went wrong|FAILED|Execution failed" build.log | head -n 200; echo; echo "----- letzte 400 Zeilen -----"; tail -n 400 build.log; } > "logs/latest.txt" &&
      cp logs/latest.txt "logs/run-${RUN}.txt" &&
      git add logs &&
      git -c user.name="ci" -c user.email="ci@users.noreply.github.com" commit -q -m "Build-Log für Lauf ${RUN}" &&
      git push -q --force "https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" ci-logs
    ) || echo "Log konnte nicht hochgeladen werden."
  fi
}

if [ -z "${SIGNING_KEY:-}" ]; then
  echo "::error title=Signaturschlüssel fehlt::Bitte das Repository-Secret SIGNING_KEY anlegen (Settings → Secrets and variables → Actions)."
  echo "Secret SIGNING_KEY fehlt." > "$LOG"
  report_failure
  exit 1
fi

# SIGNING_KEY hat das Format "<Passwort>.<Keystore als Base64>" (eine Zeile).
KEY_PASSWORD="${SIGNING_KEY%%.*}"
KEY_DATA="$(printf '%s' "${SIGNING_KEY#*.}" | tr -d ' \r\n\t')"
KEY_FILE="${RUNNER_TEMP:-/tmp}/mmp-release.p12"
if ! printf '%s' "$KEY_DATA" | base64 -d > "$KEY_FILE" 2>/dev/null || [ ! -s "$KEY_FILE" ]; then
  echo "::error title=Signaturschlüssel ungültig::Das Secret SIGNING_KEY ist beschädigt. Bitte den Text noch einmal vollständig einfügen."
  echo "Secret SIGNING_KEY ungültig." > "$LOG"
  report_failure
  exit 1
fi

export MMP_STORE_FILE="$KEY_FILE"
export MMP_STORE_PASSWORD="$KEY_PASSWORD"
export MMP_KEY_ALIAS="mentalmathplus"
export MMP_KEY_PASSWORD="$KEY_PASSWORD"
export MMP_VERSION_CODE="$((1000 + RUN))"
export MMP_VERSION_NAME="$VERSION"

./gradlew --no-daemon --stacktrace :app:testFossDebugUnitTest :app:assembleFossRelease 2>&1 | tee "$LOG"
status=${PIPESTATUS[0]}
if [ "$status" -ne 0 ]; then
  report_failure
  exit "$status"
fi

APK="$(ls app/build/outputs/apk/foss/release/*.apk | head -n 1)"
if [ -z "$APK" ] || [ ! -f "$APK" ]; then
  echo "::error::Keine APK gefunden." | tee -a "$LOG"
  report_failure
  exit 1
fi
cp "$APK" "$APK_NAME"

NOTES="ci/release-notes.md"
[ -f "$NOTES" ] || { echo "Neue Version ${VERSION}" > "$NOTES"; }

gh release create "$TAG" "$APK_NAME" \
  --title "Mental Math Plus ${VERSION}" \
  --notes-file "$NOTES" \
  --target "${GITHUB_SHA:-plus}" \
  --latest || { echo "::error::Release konnte nicht erstellt werden."; exit 1; }

echo "Fertig: ${TAG}"
