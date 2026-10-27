# Mental Math Plus

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

### Medizinertest-Training (im freien Training)
- Vorbereitung auf den Untertest **„Arithmetisches Problemlösen“** im HAM-Nat und ab 2027 im **TMSnat** (16 Aufgaben, 25 bzw. 30 Minuten, ohne Taschenrechner).
- Unbegrenzt viele eigene Textaufgaben in 12 Kategorien (Dreisatz, Prozente, Beziehungsketten, Anteile, Einheiten und Dosierung, Tempo, gemeinsame Arbeit, Mischungen, Wachstum und Halbwertszeit, Gleichungen, Kombinatorik, Wahrscheinlichkeit) – mit Lösungswegen, typischen Fehlern als falschen Antworten und unwichtigen Zusatzangaben wie im Test.
- **Testsimulationen** TMSnat (A–E) und HAM-Nat (A–D) mit Gesamtzeit, „Später bearbeiten“ und Auswertung nach Kategorien; Verlauf der Ergebnisse.
- Jede Kategorie ist auch ein Level (Welt 13) mit Erklärung, geführtem Üben und Prüfung.

### 3. Klassischer Modus – die Original-App (fährt neben „Freies Training“ heraus)
- Der unveränderte Spielmodus von „Mental Math“: Grundrechenarten mit eigenen Zahlenbereichen, Bestenliste, Verlauf und Spiele per Link.

### Für alle Bereiche
- **Einstellungen:** Farbthemen (hell/dunkel und fünf Farben), tägliche Erinnerung zur gewünschten Uhrzeit, Ton und Vibration.
- **Fortschritt sichern:** als Datei speichern oder teilen und auf einem anderen Handy wieder laden.

## Herkunft, Urheber und Lizenz

- **Original:** [Mental Math](https://codeberg.org/Mental-Math/MentalMath) von HeldDerTierwelt (© 2025–2026), lizenziert unter der [GNU GPL v3](LICENSE.md).
- **Fork „Mental Math Plus“:** betreut von [tro123N](https://github.com/tro123N), Code und Texte erzeugt mit der KI Claude (Anthropic). Alle Änderungen stehen ebenfalls unter der GPL v3.
- **Buch:** Die Methoden folgen *Mathe-Magie* von Arthur Benjamin und Michael Shermer (Heyne). Die App verweist nur auf Kapitel und Abschnitte; Texte, Wortlisten und Übungen aus dem Buch sind nicht enthalten.

## Entwicklung

- Die Weiterentwicklung liegt auf dem Branch `plus`. Jeder Push dorthin baut über GitHub Actions eine signierte APK (`.github/workflows/build.yml`, Logik in `ci/build.sh`) und veröffentlicht sie unter *Releases*.
- Der Signaturschlüssel liegt nicht im Repository, sondern als Repository-Secret `SIGNING_KEY` (Format: `<Passwort>.<Keystore als Base64>`).
- Lokal bauen: `./gradlew assembleFossRelease` (Android SDK und JDK 21 nötig).
