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
- Entwicklung auf Branch `plus` (nicht `main`; `main` ist der unveränderte Import des Originals – nur `README.md` wird dort nach jedem Release von `ci/build.sh` aus `plus` übernommen, weil GitHub `main` als Startseite zeigt; den Standard-Branch kann man über den Proxy nicht ändern).
- Jeder Push auf `plus` startet `.github/workflows/build.yml` → `ci/build.sh`: Unit-Tests (`testFossDebugUnitTest`), `assembleFossRelease`, Release `v<Version>` mit Asset `MentalMathPlus.apk`.
- **Versionsnummer** (seit 2.0, vorher `1.0.<Laufnummer>`): steht in `ci/version.txt` als `Major.Minor`. Neue Funktionen → zweite Zahl erhöhen (2.0 → 2.1), großer Umbau → erste Zahl. Existiert die Version schon als Release, hängt `ci/build.sh` automatisch `.1`, `.2` … an (reine Korrekturen). `versionCode` bleibt `1000 + Laufnummer` (steigt immer).
- Feste Download-Adresse: https://github.com/tro123N/MentalMathPlus/releases/latest/download/MentalMathPlus.apk
- Vor jedem Push auf `plus` `ci/release-notes.md` (kurz, Deutsch, für den Nutzer) aktualisieren – das wird der Release-Text – und bei neuen Funktionen `ci/version.txt` erhöhen.
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
- Drei Bereiche mit Leiste unten (`PlusBottomBar`, `PlusTab`): Kletterweg („climb“), Freies Training („free“), Klassischer Modus der Original-App („settings“). „Klassisch“ fährt nur neben „Freies Training“ heraus (gehört zum freien Üben). Wechsel über `NavController.openTab`, „Zurück“ führt immer zum Kletterweg.
- Trainingshilfen (an/aus in `PlusSettings.trickTips`/`adaptive`, Schalter im freien Training und in den Einstellungen): `engine/TrickAdvisor.kt` (Trick-Tipp: Level zur Aufgabe, bei freien Aufgaben per Mustererkennung) und `engine/Adaptive.kt` + `SessionLogic.adapt` (Stufe im freien Training, `FreeRules.level = 0` = automatisch).
- `domain/climb/engine/Stats.kt` (Sicherheit je Level, Stärken/Schwächen, Tagesreihen), `Placement.kt` (Einstufungstest je Welt), `Reminder.kt` (Text der Erinnerung).
- `domain/climb/model/PlusSettings.kt` (Farbthema `Palette`, Erinnerung) und `Backup.kt` (Sicherungsdatei, `BackupCodec`); gespeichert von `data/climb/PlusSettingsRepository` (plus_settings.json).
- Erinnerung: `data/climb/ReminderScheduler` + `ReminderWorker` (WorkManager, täglich zur gewählten Uhrzeit). Die alte Erinnerung der Original-App wird beim ersten Start übernommen und ist im Menü des klassischen Modus ausgeblendet.
- Farbthemen: `presentation/theme/Theme.kt` (`MentalMathTheme(themeMode, palette)`, Akzentfarbe `accent` statt `blue` im Kletterweg).
- TMSnat-Training (Untertest „Arithmetisches Problemlösen“ des TMSnat ab 2027, aus dem HAM-Nat übernommen – für den Nutzer ein und derselbe Untertest, deshalb keine eigene HAM-Nat-Variante anbieten: 16 Aufgaben in 25 min statt früher 30, Antworten A–E, kein Taschenrechner; letzter HAM-Nat am 5.9.2026, erster TMSnat geplant 8./9.5.2027): `curriculum/ApTasks.kt` (12 Kategorien `ApCategory`, je 3 Stufen, Lösungswege, typische Fehler als Distraktoren, Ballast-Sätze, `choice()` für Antworten A–E – nach Größe sortiert, richtige Antwort an jeder Stelle gleich oft, Test `apAnswerPositionsAreEven` –, `simulation()`, `simulationName()`: TMSnat-Simulation 1500 s, Kurztest 8 Aufgaben/750 s, Tempo-Test 1200 s), `curriculum/World13.kt` (Welt 13 mit `independent = true`: immer offen, nicht Teil von „Weiterklettern“), Route „aptraining“ (`ApTrainingScreen`, Einstieg im freien Training). Testsimulation = `FreeMode.SIMULATION` (Gesamtzeit, keine Rückmeldung, `SessionLogic.skip`, offene Aufgaben zählen falsch), Ergebnisse in `ClimbProgress.apRuns`; `FreeRules.track = "ap"` zählt die Antworten auch für die Levels von Welt 13. Gemischtes Üben (`FreeMode.ENDLOS` mit track „ap“, `Session.showsSolution`) zeigt nach Fehlern den Lösungsweg (Phase REVEAL); im freien Training wird eine falsch gelöste Aufgabe nicht wiederholt.
- Seit 2.0 im TMSnat-Training: **Fehler-Erklärungen** (`ApTask.wrong: List<Wrong>` mit `why`; `near()` = nahe Zahl ohne Erklärung; `Problem.mistakes`/`mistakeFor()`, `SessionLogic.MISTAKE_PREFIX`, `ProblemResult.note`; jede neue falsche Antwort braucht eine zutreffende Erklärung mit eigenen Zahlen), **Zeitanalyse** (`engine/ApTime.kt`; `Planned.number/spentMs`, `ProblemResult.number/extraMs/totalMs`, `finishNow(s, elapsedMs)`), **Punkt-Prognose** (`engine/ApForecast.kt`, Monte-Carlo mit 10.000 Tests aus `SkillProgress.recent` der Welt-13-Levels) und **Fehlertraining** (`engine/ApReview.kt`, `ClimbProgress.apMistakes`, Aufgabentyp = `Kategorie/Schlüssel-Präfix` über `ApTasks.typeOf/typeName/taskOfType`; Stufen sofort → 1 → 3 → 7 Tage, wird in `SessionFinisher.finish` für alle Einheiten außer geführtem Üben fortgeschrieben).
- **KI-Coach (seit 2.1)** – Gemma 4 offline über LiteRT-LM 0.18.0 (`com.google.ai.edge.litertlm:litertlm-android`, braucht Android 12+):
  - `domain/climb/ai` (reines Kotlin, Tests in `AiTest`): `AiModels.kt` (Katalog E4B/12B/E2B, auf HF-Commit gepinnte URL, Größe, sha256, Kontext, Denkbudget), `Calculator.kt` (exakter Bruch-Rechner, deutsches Zahlenformat ohne Tausenderpunkte), `AiTools.kt` (`AiToolbox`: 13 Werkzeuge mit JSON-Schema und Ausführung gegen `AiContext`; `AiQuiz` für Chat-Aufgaben aus Seed), `AiPrompt.kt` (Systemanweisung, `taskBlock` mit Lösungsweg der App, Vorschläge), `AiAgent.kt` (Schleife Modell ↔ Werkzeuge, Schnittstelle `LlmSession`), `AiText.kt` (LaTeX/Steuerzeichen entfernen, Markdown-Leser), `AiChat.kt` (`AiMessage`, `AiAction`, `AiHistory`), `AiLookup.kt` (Kategorien/Levels finden, Datum).
  - Die KI führt nie selbst etwas aus: Übungen, Levels und Einstellungen kommen als `AiAction` (Knopf unter der Antwort), ausgeführt in `AiChatScreen.runAction`. Neue Werkzeuge: Spec in `AiToolbox.specs`, Ausführung, Test in `AiTest`.
  - `data/climb/ai`: `AiEngine` (GPU → CPU, MTP wenn möglich, manuelles Tool-Calling, Absturzvermerk `ai_loading.txt`/`ai_bad.txt`, Freigabe nach 3 min; Sampler-Startwert pro Gespräch zufällig – LiteRT-LM nimmt ihn direkt, mit festem Wert käme nach „Nochmal“ dieselbe Antwort –, in Tests fest 42; `AiViewModel.run` versucht es nach einem Fehler ohne sichtbaren Text einmal still neu), `ModelStore` (Android-DownloadManager nach `Android/data/<pkg>/files/models`, Prüfsumme, `.ok`-Vermerk; nicht in der Android-Sicherung), `AiChatRepository` (`ai_chat.json`). Einstellungen in `PlusSettings.aiModel/aiThinking/aiFast/aiWifiOnly`.
  - Oberfläche: `AiViewModel` (Activity-weit), `AiChatScreen` (Route „ai“), `AiSettingsScreen` („aisettings“), `component/AiComponents.kt`. Einstiege: ✨ auf der Startseite, TMSnat-Training, „KI-Coach fragen“ nach Fehlern (RevealCard) und im Ergebnis.
  - R8: `app/proguard-rules.pro` hält LiteRT-LM (JNI ruft Kotlin-Methoden per Namen auf) und braucht `-dontwarn` für `androidx.window.extensions/sidecar` (sonst bricht der Release-Build ab). Manifest: INTERNET (nur Download), `uses-native-library` für OpenCL.
  - Emulator-Test `l_kiCoach`: `ci/smoke-build.sh` lädt Gemma 4 E2B (2,6 GB), `ci/smoke.sh` spielt es auf; Antworten der KI stehen in `screens/ki_antworten.txt` auf ci-smoke, das Ergebnis des Downloads in `summary.txt` (aus `model-info.txt`); der Smoke-Job prüft vorab auch Unit-Tests und Release-Build (R8) und räumt am Anfang Speicher frei. Das Laden auf der GPU lässt sich nur auf einem echten Handy prüfen.
- Handbuch (`HandbookScreen`, Route „handbook“) wird aus `Curriculum.worlds` erzeugt – neue Levels erscheinen automatisch; Erklärungen rendert `component/ExplanationBlocks.kt` (auch im Lernen-Bildschirm).

## Stand
- 12 Welten / 84 Levels decken Kapitel 0–9 des Buches ab (Welt 1 Grundlagen, Welt 5 Kommazahlen als Ergänzung); dazu Welt 13 (13 Levels) als eigenständiges TMSnat-Training.
- Freies Training mit 9 Aufgabenarten, 3 Stufen, Modi Zeit/Endlos/Überleben/Prüfung und Statistik; der klassische Modus der Original-App ist über „Klassischer Modus“ erreichbar.
- Lokale Tests: `CurriculumTest` rechnet jeden Lösungsweg nach und prüft, dass jede Antwort eintippbar ist; `World12Test` prüft die Wochentagsmethode gegen den echten Kalender.
- Statistik mit Diagrammen (Canvas, `component/Charts.kt`), Einstufungstest, tägliche Erinnerung, Sicherung (speichern/teilen/laden) und Farbthemen sind eingebaut.
- KI-Coach (2.1): Gemma 4 E4B/12B/E2B auf dem Handy, Chat mit Werkzeugen (Lernstand, Rechner, Methoden, Aufgaben, Vorschläge). Auf dem Handy des Nutzers (OnePlus 15, 16 GB) noch nicht selbst erprobt – Rückmeldungen ernst nehmen.
- Ideen für später (vom Nutzer noch nicht gewählt): Tagesaufgabe mit Serie, Erfolge, Audio-Modus.
