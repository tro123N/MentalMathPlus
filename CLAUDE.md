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
- Lauf-Status: `curl -s https://api.github.com/repos/tro123N/MentalMathPlus/actions/runs?per_page=3` (Jobs: `/actions/runs/<id>/jobs`)
- **Test auf virtuellem Handy (Job `smoke`):** läuft bei jedem Push auf `plus` und `dev`. `ci/smoke-build.sh` baut Debug- und Test-APK, `ci/smoke.sh` startet `ClimbSmokeTest` (androidTest) im Emulator. Ergebnis, Logs und Bildschirmfotos liegen danach auf dem Branch `ci-smoke`:
  `git fetch -q origin +refs/heads/ci-smoke:refs/remotes/origin/ci-smoke && git show origin/ci-smoke:summary.txt`
  Bilder: `git archive origin/ci-smoke screens | tar -x -C <ordner>`. Welche Levels ausführlich fotografiert werden, steht in `ci/smoke-shots.txt`.
- **Zwischenstände auf `dev` pushen** (nur Emulator-Test, kein Release). Erst wenn alles passt, `dev` in `plus` übernehmen – das erzeugt das Release für den Nutzer.

## Lokal prüfen (ohne Android SDK)
In der Cloud-Umgebung sind Google Maven und Maven Central gesperrt; Android lässt sich nicht lokal bauen. Die reine Kotlin-Logik unter `domain/climb` hat keine Android-Abhängigkeiten und lässt sich mit `kotlinc` (GitHub-Release `JetBrains/kotlin`, z. B. 2.4.20) kompilieren und mit JUnit 4 testen (`junit-4.13.2.jar` und `hamcrest-core-1.3.jar` liegen in `/opt/gradle-*/lib`).

## Architektur (Kletterweg)
- `domain/climb/model` – Aufgaben, Schritte, erwartete Antworten, Levels, Welten, Fortschritt.
- `domain/climb/math` – Formatierung (deutsches Zahlenformat), Antwortprüfung, Hilfsfunktionen.
- `domain/climb/engine` – Bewertung (Sterne), Freischalten, Auffrischen/Zurückstufen, Wiederholungsplan.
- `domain/climb/curriculum` – eine Datei pro Welt mit Generatoren, Lösungswegen und Erklärungstexten.
- `data/climb` – Speicherung des Fortschritts als JSON-Datei.
- `domain/climb/free` – Aufgabenarten des freien Trainings (FreeTopics); Modi und Regeln in `engine/Session.kt` (FreeMode, FreeRules).
- `domain/climb/math/Phonetic.kt` – phonetischer Code mit eigenen Merkwörtern 0–99 (nicht die Liste aus dem Buch!).
- `presentation/climb` – ViewModel und Compose-Oberfläche im Stil der App (Startseite „climb“, Level, Lernen, Spielen, Ergebnis, „free“, „handbook“, „stats“, „placement“, „plussettings“).
- Drei Bereiche mit Leiste unten (`PlusBottomBar`, `PlusTab`): Kletterweg („climb“), Freies Training („free“), Klassischer Modus der Original-App („settings“). Wechsel über `NavController.openTab`, „Zurück“ führt immer zum Kletterweg.
- `domain/climb/engine/Stats.kt` (Sicherheit je Level, Stärken/Schwächen, Tagesreihen), `Placement.kt` (Einstufungstest je Welt), `Reminder.kt` (Text der Erinnerung).
- `domain/climb/model/PlusSettings.kt` (Farbthema `Palette`, Erinnerung) und `Backup.kt` (Sicherungsdatei, `BackupCodec`); gespeichert von `data/climb/PlusSettingsRepository` (plus_settings.json).
- Erinnerung: `data/climb/ReminderScheduler` + `ReminderWorker` (WorkManager, täglich zur gewählten Uhrzeit). Die alte Erinnerung der Original-App wird beim ersten Start übernommen und ist im Menü des klassischen Modus ausgeblendet.
- Farbthemen: `presentation/theme/Theme.kt` (`MentalMathTheme(themeMode, palette)`, Akzentfarbe `accent` statt `blue` im Kletterweg).
- Handbuch (`HandbookScreen`, Route „handbook“) wird aus `Curriculum.worlds` erzeugt – neue Levels erscheinen automatisch; Erklärungen rendert `component/ExplanationBlocks.kt` (auch im Lernen-Bildschirm).

## Stand
- 12 Welten / 84 Levels decken Kapitel 0–9 des Buches ab (Welt 1 Grundlagen, Welt 5 Kommazahlen als Ergänzung).
- Freies Training mit 9 Aufgabenarten, 3 Stufen, Modi Zeit/Endlos/Überleben/Prüfung und Statistik; der klassische Modus der Original-App ist über „Klassischer Modus“ erreichbar.
- Lokale Tests: `CurriculumTest` rechnet jeden Lösungsweg nach und prüft, dass jede Antwort eintippbar ist; `World12Test` prüft die Wochentagsmethode gegen den echten Kalender.
- Statistik mit Diagrammen (Canvas, `component/Charts.kt`), Einstufungstest, tägliche Erinnerung, Sicherung (speichern/teilen/laden) und Farbthemen sind eingebaut.
- Ideen für später (vom Nutzer noch nicht gewählt): Tagesaufgabe mit Serie, Trick-Tipp-Knopf, anpassende Schwierigkeit, Erfolge, Audio-Modus.
