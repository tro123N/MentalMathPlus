# Mental Math Plus – Hinweise für die Weiterentwicklung

## Projekt und Nutzer
- Fork der Android-App „Mental Math“ (Kotlin, Jetpack Compose, Hilt, Room, DataStore), GPLv3.
- Ziel: ein Kletterweg (Welten → Levels) zum Kopfrechnen nach den Methoden aus dem Buch *Mathe-Magie* (Benjamin/Shermer, Heyne 2017).
- Der Besitzer (tro123N) programmiert nicht selbst. Immer auf Deutsch antworten, Schritte für ihn einfach erklären.

## Wichtige Regeln
- **Kein Buchtext in die App.** Das Buch ist urheberrechtlich geschützt und das Repository öffentlich. Methoden exakt wie im Buch umsetzen (gleiche Schritte, Reihenfolge, Namen wie „Additionsmethode“), aber Erklärungen in eigenen Worten und mit eigenen Beispielen schreiben. Keine Wortlisten, Merksätze oder Übungsaufgaben aus dem Buch übernehmen. Pro Level nur auf Kapitel und Abschnitt verweisen.
- **Keine Schlüssel einchecken.** Der Signaturschlüssel liegt als Repository-Secret `SIGNING_KEY` vor (Format `<Passwort>.<PKCS12 als Base64>`, Alias `mentalmathplus`). `*.p12/*.jks/*.keystore` sind in `.gitignore`.
- Fortschritts-IDs der Levels (z. B. `w3.add2`) nie umbenennen, sonst geht gespeicherter Fortschritt verloren.

## Branches, Build und Release
- Entwicklung auf Branch `plus` (nicht `main`; `main` ist der unveränderte Import des Originals).
- Jeder Push auf `plus` startet `.github/workflows/build.yml` → `ci/build.sh`: Unit-Tests (`testFossDebugUnitTest`), `assembleFossRelease`, Release `v1.0.<Laufnummer>` mit Asset `MentalMathPlus.apk`.
- Feste Download-Adresse: https://github.com/tro123N/MentalMathPlus/releases/latest/download/MentalMathPlus.apk
- Vor jedem Push `ci/release-notes.md` (kurz, Deutsch, für den Nutzer) aktualisieren – das wird der Release-Text.
- Schlägt ein Build fehl, legt `ci/build.sh` das Log auf den Branch `ci-logs` (`logs/latest.txt`):
  `git fetch origin +refs/heads/ci-logs:refs/remotes/origin/ci-logs && git show origin/ci-logs:logs/latest.txt`
- Lauf-Status: `curl -s https://api.github.com/repos/tro123N/MentalMathPlus/actions/runs?per_page=3`

## Lokal prüfen (ohne Android SDK)
In der Cloud-Umgebung sind Google Maven und Maven Central gesperrt; Android lässt sich nicht lokal bauen. Die reine Kotlin-Logik unter `domain/climb` hat keine Android-Abhängigkeiten und lässt sich mit `kotlinc` (GitHub-Release `JetBrains/kotlin`, z. B. 2.4.20) kompilieren und mit JUnit 4 testen (`junit-4.13.2.jar` und `hamcrest-core-1.3.jar` liegen in `/opt/gradle-*/lib`).

## Architektur (Kletterweg)
- `domain/climb/model` – Aufgaben, Schritte, erwartete Antworten, Levels, Welten, Fortschritt.
- `domain/climb/math` – Formatierung (deutsches Zahlenformat), Antwortprüfung, Hilfsfunktionen.
- `domain/climb/engine` – Bewertung (Sterne), Freischalten, Auffrischen/Zurückstufen, Wiederholungsplan.
- `domain/climb/curriculum` – eine Datei pro Welt mit Generatoren, Lösungswegen und Erklärungstexten.
- `data/climb` – Speicherung des Fortschritts als JSON-Datei.
- `presentation/climb` – ViewModel und Compose-Oberfläche im Stil der App.
