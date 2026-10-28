# Mental Math Plus

[![Neueste Version](https://img.shields.io/github/v/release/tro123N/MentalMathPlus?label=Version&sort=semver)](https://github.com/tro123N/MentalMathPlus/releases/latest)

> **Hinweis: Dieses Projekt wurde mit KI erstellt.** Alle Erweiterungen dieses Forks – Code, Erklärungstexte und Dokumentation – wurden von der KI **Claude (Anthropic)** geschrieben. Es ist ein **privates Hobbyprojekt** ohne kommerzielle Absicht, ohne Gewährleistung und ohne professionelle Prüfung. Fehler sind möglich.
>
> **Inhaber und Betreuer dieses Forks:** [tro123N](https://github.com/tro123N). Er hat die Idee, die Wünsche und die Entscheidungen beigesteuert, programmiert aber nicht selbst.
>
> **Die eigentliche App „Mental Math“** stammt von **HeldDerTierwelt** und wird auf [Codeberg](https://codeberg.org/Mental-Math/MentalMath) entwickelt. Mental Math Plus baut darauf auf; Ehre und Rechte am Original gehören ihm.

Kopfrechnen Schritt für Schritt lernen: ein **Kletterweg** aus Welten und Levels, der die Rechenmethoden aus dem Buch *Mathe-Magie* (Arthur Benjamin & Michael Shermer) trainiert – vom Zahlengefühl bis zum Quadrieren fünfstelliger Zahlen. Dazu kommen ein freies Training und der klassische Modus der Original-App.

Diese App ist ein inoffizieller Fork. Sie steht in keiner Verbindung zu den Autoren oder dem Verlag des Buchs und auch nicht zum Entwickler der Original-App. Alle Erklärungen in der App sind eigene Texte; das Buch selbst ist nicht enthalten.

## Download

Die jeweils neueste Version (Android-APK):

**https://github.com/tro123N/MentalMathPlus/releases/latest/download/MentalMathPlus.apk**

Beim ersten Installieren fragt Android, ob der Browser Apps installieren darf. Updates installieren sich über die vorhandene Version, der Fortschritt bleibt erhalten.

## Funktionen

Die App hat drei getrennte Bereiche (Leiste unten):

### 1. Kletterweg – lernen nach Plan (neu in Mental Math Plus)
- **12 Welten mit 84 Levels** nach den Kapiteln 0–9 des Buchs. Jedes Level: Erklärung, geführtes Üben (Zwischenschritte eintippen), freies Üben und eine Prüfung auf Zeit.
- **Ziele und Sterne:** Ein Level gilt als geschafft, wenn Genauigkeit und Durchschnittszeit stimmen. Schneller heißt mehr Sterne.
- **Auffrischen statt Vergessen:** Gelernte Levels kommen in Abständen wieder dran. Wer schlechter wird, frischt die Grundlage auf, bevor es weitergeht.
- **Einstufungstest:** Welten überspringen, die man schon kann.
- **Handbuch:** alle Tricks zum Nachschlagen, durchsuchbar; wächst mit jedem neuen Level automatisch mit.
- **Statistik:** Verlauf als Diagramm, Stärken und Schwächen, Fortschritt je Welt und je Level.

### 2. Freies Training – üben ohne Plan (neu in Mental Math Plus)
- Neun Aufgabenarten in drei Stufen mit den Modi Auf Zeit, Endlos, Überleben und Prüfung, eigene Bestwerte. Zählt nicht für den Kletterweg.
- **Anpassende Schwierigkeit** (schaltbar): wird schwerer, wenn es gut läuft, und leichter nach Fehlern.
- **Trick-Tipp-Knopf** (schaltbar, auch im Kletterweg beim Üben): zeigt, welcher Trick zur Aufgabe passt.

### TMSnat-Training (im freien Training)
- Vorbereitung auf den TMSnat-Untertest **„Arithmetisches Problemlösen“** (ab 2027; der Untertest stammt aus dem HAM-Nat): 16 Aufgaben in 25 Minuten, Antworten A–E, ohne Taschenrechner.
- Unbegrenzt viele eigene Textaufgaben in 12 Kategorien (Dreisatz, Prozente, Beziehungsketten, Anteile, Einheiten und Dosierung, Tempo, gemeinsame Arbeit, Mischungen, Wachstum und Halbwertszeit, Gleichungen, Kombinatorik, Wahrscheinlichkeit) – mit Lösungswegen und unwichtigen Zusatzangaben wie im Test.
- **Typische Fehler erklärt:** Die falschen Antworten entstehen aus typischen Denkfehlern. Wer so eine Antwort wählt (oder eintippt), sieht, was schiefging – z. B. „Das ist die Tagesdosis – sie muss noch auf 3 Gaben verteilt werden.“ Die richtige Antwort steht gleich oft bei A, B, C, D und E.
- **Testsimulation** wie im TMSnat (16 Aufgaben, 25 Minuten), dazu Kurztest und Tempo-Test (20 Minuten); mit Gesamtzeit, „Später bearbeiten“, Auswertung nach Kategorien und **Zeitanalyse** (Zeit pro Aufgabe gegen das Ziel von 94 Sekunden, Zeitfresser, Tipps).
- **Punkt-Prognose:** Aus den eigenen Ergebnissen werden 10.000 Tests simuliert – mit erwarteter Punktzahl, Bereich, Tempo und der Kategorie, deren Üben am meisten bringt.
- **Fehlertraining:** Falsch gelöste Aufgabentypen kommen mit neuen Zahlen wieder – sofort, dann nach 1, 3 und 7 Tagen.
- **Gemischt üben** ohne Ende: nach einem Fehler erscheint sofort der Lösungsweg.
- Countdown bis zum ersten TMSnat; jede Kategorie ist auch ein Level (Welt 13) mit Erklärung, geführtem Üben und Prüfung.

### KI-Coach – eine KI direkt auf dem Handy (ab Version 2.1)
- Ein Sprachmodell von Google (**Gemma 4**) läuft komplett auf dem Handy, über [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM): **ohne Internet, kostenlos, Gespräche und Lernstand bleiben auf dem Gerät.** Das Modell wird einmal heruntergeladen.
- Er **kennt den Lernstand** (Kletterweg und TMSnat-Training), **erklärt Aufgaben und Fehler** Schritt für Schritt – bei Aufgaben der App immer anhand ihres geprüften Lösungswegs – und **stellt Aufgaben im Chat**, prüft die Antworten und gibt auf Wunsch Tipps.
- Er **greift in die App ein**, aber nur auf Knopfdruck: Er bereitet Übungen, Testsimulationen, das Fehlertraining, ähnliche Aufgaben, Levels und Einstellungen (z. B. die tägliche Erinnerung) vor; die Person startet sie selbst.
- Jede Rechnung prüft die KI mit einem eingebauten **exakten Rechner** (Bruchrechnung statt Rundungsfehler).
- **Denkmodus** („Gründlich“) für schwierige Fragen, umschaltbar auf „Schnell“.
- Modelle: **Gemma 4 E4B** (empfohlen, das klügste Modell, das zuverlässig auf Handys läuft, 3,7 GB) und **Gemma 4 E2B** (klein und schnell, 2,6 GB). Gemma 4 12B ist für Computer gedacht und wird seit 2.1.1 nicht mehr angeboten (auf Handys reicht der Speicher der Grafikeinheit nicht). Lizenz der Modelle: Apache 2.0. Braucht Android 12 oder neuer.
- Einstieg: ✨ oben auf der Startseite, im TMSnat-Training, nach einem Fehler beim Üben („KI-Coach fragen“) und in der Aufgabenliste jedes Ergebnisses.

### 3. Klassischer Modus – die Original-App (fährt neben „Freies Training“ heraus)
- Der unveränderte Spielmodus von „Mental Math“: Grundrechenarten mit eigenen Zahlenbereichen, Bestenliste, Verlauf und Spiele per Link.

### Für alle Bereiche
- **Einstellungen:** Farbthemen (hell/dunkel und fünf Farben), tägliche Erinnerung zur gewünschten Uhrzeit, Ton und Vibration, KI-Coach (Modell, Denkmodus, Speicher).
- **Berechtigungen:** Benachrichtigungen (Erinnerung), Vibration und – nur für den Download des KI-Modells – Internet.
- **Fortschritt sichern:** als Datei speichern oder teilen und auf einem anderen Handy wieder laden.

## Versionen

Die Versionsnummer zeigt, wie groß eine Änderung ist:

- **erste Zahl** (z. B. **2**.0): großer Umbau oder ein ganz neuer Bereich,
- **zweite Zahl** (z. B. 2.**1**): neue Funktionen,
- **dritte Zahl** (z. B. 2.1.**1**): nur Fehlerkorrekturen.

Bis Version 1.0.24 war die letzte Zahl nur ein Zähler der automatischen Builds – deshalb die Sprünge (1.0.15 → 1.0.18 → 1.0.20 → 1.0.24). Ab Version 2.0 gilt die Zählung oben. Was sich in jeder Version geändert hat, steht unter [Releases](https://github.com/tro123N/MentalMathPlus/releases).

## Herkunft, Urheber und Lizenz

- **Original:** [Mental Math](https://codeberg.org/Mental-Math/MentalMath) von HeldDerTierwelt (© 2025–2026), lizenziert unter der [GNU GPL v3](LICENSE.md).
- **Fork „Mental Math Plus“:** betreut von [tro123N](https://github.com/tro123N), Code und Texte erzeugt mit der KI Claude (Anthropic). Alle Änderungen stehen ebenfalls unter der GPL v3.
- **Buch:** Die Methoden folgen *Mathe-Magie* von Arthur Benjamin und Michael Shermer (Heyne). Die App verweist nur auf Kapitel und Abschnitte; Texte, Wortlisten und Übungen aus dem Buch sind nicht enthalten.
- **KI:** Die Modelle Gemma 4 (Google, Apache 2.0) werden nicht mit der App verteilt, sondern bei Bedarf von Hugging Face (Konto `litert-community`) geladen. Die Laufzeit LiteRT-LM (Google) steht ebenfalls unter Apache 2.0.

## Entwicklung

- Die Weiterentwicklung liegt auf dem Branch `plus`. Jeder Push dorthin baut über GitHub Actions eine signierte APK (`.github/workflows/build.yml`, Logik in `ci/build.sh`) und veröffentlicht sie unter *Releases*.
- Die Versionsnummer steht in `ci/version.txt` (z. B. `2.1`). Gibt es diese Version schon, zählt der Build automatisch die dritte Zahl hoch (`2.1.1`, `2.1.2` …).
- Der Branch `main` enthält den unveränderten Import der Original-App; nur diese README wird nach jedem Release dorthin übernommen, damit sie auf der Startseite des Repositorys erscheint.
- Der Signaturschlüssel liegt nicht im Repository, sondern als Repository-Secret `SIGNING_KEY` (Format: `<Passwort>.<Keystore als Base64>`).
- Lokal bauen: `./gradlew assembleFossRelease` (Android SDK und JDK 21 nötig).
