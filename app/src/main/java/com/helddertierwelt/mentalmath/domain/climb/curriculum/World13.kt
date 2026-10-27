/*
Mental Math Plus - Kletterweg zum Kopfrechnen
Copyright (C) 2026 tro123N
Based on Mental Math, Copyright (C) 2025-2026 HeldDerTierwelt

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.md.
*/

package com.helddertierwelt.mentalmath.domain.climb.curriculum

import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.domain.climb.model.BookRef
import com.helddertierwelt.mentalmath.domain.climb.model.Goals
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.random.Random

/**
 * Welt 13 (Ergänzung, nicht aus dem Buch): Vorbereitung auf den Untertest „Arithmetisches
 * Problemlösen“ des HAM-Nat und des TMSnat (ab 2027). Eigene Textaufgaben in zwölf Kategorien
 * und ein gemischtes Level mit Antwortmöglichkeiten wie im Test.
 */
object World13 {

    private val book = BookRef(null, "Ergänzung: Medizinertest (TMSnat, HAM-Nat)")

    private fun example(category: ApCategory, seed: Long) =
        Block.Example(ApTasks.typed(ApTasks.task(category, 2, Random(seed))))

    private val easyGoals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 100.0, star2 = 75.0, star3 = 50.0)
    private val goals = Goals(testSize = 8, minAccuracy = 0.875, star1 = 120.0, star2 = 90.0, star3 = 60.0)

    private class Text(val summary: String, val intro: String, val steps: List<String>, val tip: String)

    private val texts = mapOf(
        ApCategory.RATIO to Text(
            "Von einer bekannten Menge auf eine andere schließen – mit und gegen den Strich.",
            "Beim Dreisatz schließt du von einer bekannten Menge auf eine andere. Entscheidend ist zuerst die Frage: Wird das Ergebnis größer oder kleiner? Mehr Patienten brauchen mehr Tabletten (proportional). Mehr Helfer brauchen aber weniger Zeit (antiproportional).",
            listOf(
                "Prüfen: „je mehr, desto mehr“ (proportional) oder „je mehr, desto weniger“ (antiproportional)?",
                "Proportional: erst auf 1 herunterteilen, dann mit der gesuchten Zahl malnehmen.",
                "Antiproportional: erst den Gesamtaufwand (Anzahl × Zeit) ausrechnen, dann durch die neue Anzahl teilen.",
                "Ändern sich zwei Größen (Maschinen und Stunden), alles auf „eine Maschine, eine Stunde“ zurückrechnen.",
            ),
            "Überschlage vor dem Rechnen die Richtung: Werden es mehr Helfer, muss die Zeit kleiner werden. Antworten in der falschen Richtung kannst du sofort streichen.",
        ),
        ApCategory.PERCENT to Text(
            "Prozentwert, Grundwert und Prozentsatz sicher auseinanderhalten.",
            "Prozentaufgaben haben immer drei Größen: den Grundwert (das Ganze, 100 %), den Prozentsatz und den Prozentwert (den Anteil). Die meisten Fehler passieren beim Grundwert: Nach einer Erhöhung um 25 % ist der neue Preis 125 % des alten – nicht 100 %.",
            listOf(
                "Klären: Was ist 100 %? Meist der Wert vor der Änderung.",
                "Prozentwert = Grundwert × Prozentsatz ÷ 100.",
                "Erhöhen oder senken in einem Schritt: mal (100 + p) bzw. (100 − p), dann durch 100.",
                "Zurückrechnen: neuer Wert ÷ (100 ± p) × 100.",
                "Zwei Änderungen nacheinander: mit 100 als Startwert durchrechnen.",
            ),
            "Erst plus 20 % und dann minus 20 % ergibt nicht den alten Wert: 100 → 120 → 96. Am Ende fehlen 4 %.",
        ),
        ApCategory.RELATIONS to Text(
            "Ketten aus „doppelt so viele“, „6 weniger als“ und „25 % mehr“ auflösen.",
            "Viele Aufgaben bestehen aus einer Kette von Beziehungen: „A hat doppelt so viele wie B, B hat 6 weniger als C, C hat 24.“ Suche die Größe, die als Zahl gegeben ist, und rechne von dort Schritt für Schritt bis zur gefragten Größe.",
            listOf(
                "Die Größe mit bekannter Zahl suchen – dort beginnt die Rechnung.",
                "Jede Beziehung als kleine Gleichung lesen: „A hat 6 weniger als B“ heißt A = B − 6.",
                "Schritt für Schritt bis zur gefragten Größe rechnen und Zwischenwerte notieren.",
                "Zum Schluss prüfen: Ist nach einer Größe oder nach der Summe gefragt?",
            ),
            "„Doppelt so viele wie“ und „halb so viele wie“ werden unter Zeitdruck leicht verwechselt. Prüfe jede Beziehung kurz mit deinen Zahlen: Hat A wirklich mehr als B?",
        ),
        ApCategory.FRACTIONS to Text(
            "Aus dem bekannten Rest auf das Ganze schließen.",
            "Bei Anteilsaufgaben ist oft nur der Rest als Zahl bekannt. Bestimme zuerst, welchen Bruchteil dieser Rest ausmacht – dann ist das Hochrechnen auf das Ganze einfach.",
            listOf(
                "Alle Anteile in gleiche Teile umrechnen (gemeinsamer Nenner, also das kgV der Nenner).",
                "Rest in Teilen: alle Teile minus die bekannten Teile.",
                "Ein Teil = Restmenge ÷ Anzahl der Restteile.",
                "Das Ganze = ein Teil × Anzahl aller Teile.",
                "„Ein Drittel des Rests“: von hinten nach vorne zurückrechnen.",
            ),
            "Achte darauf, worauf sich ein Anteil bezieht: „ein Viertel aller Spritzen“ ist etwas anderes als „ein Viertel der übrigen Spritzen“.",
        ),
        ApCategory.UNITS to Text(
            "Dosierungen, Infusionen und Einheiten ohne Faktor-10-Fehler.",
            "Dosierungsaufgaben sind fast immer ein Dreisatz mit einer Einheiten-Falle. Bringe alle Angaben in dieselbe Einheit, bevor du rechnest.",
            listOf(
                "Einheiten angleichen: 1 g = 1.000 mg, 1 mg = 1.000 µg, 1 l = 1.000 ml, 1 h = 60 min.",
                "Dosis pro kg: mg pro kg × Körpergewicht.",
                "Auf Einzelgaben verteilen: Tagesdosis ÷ Anzahl der Gaben.",
                "Infusion: Menge ÷ Zeit. Tropfen pro Minute = ml pro Stunde × Tropfen pro ml ÷ 60.",
            ),
            "Falsche Antworten unterscheiden sich im Test oft genau um den Faktor 10 oder 1.000. Prüfe am Ende die Größenordnung: Niemand nimmt 60 Tabletten am Tag.",
        ),
        ApCategory.SPEED to Text(
            "Weg = Tempo × Zeit – mit Durchschnitt, Begegnung und Einholen.",
            "Weg, Zeit und Geschwindigkeit hängen über eine einzige Formel zusammen: Weg = Geschwindigkeit × Zeit. Die Fallen sind die Einheiten und die Durchschnittsgeschwindigkeit.",
            listOf(
                "Einheiten prüfen: Minuten in Stunden umrechnen („wie oft passen 12 Minuten in eine Stunde?“), 1 m/s = 3,6 km/h.",
                "Durchschnitt = Gesamtweg ÷ Gesamtzeit – nie einfach den Mittelwert der Geschwindigkeiten.",
                "Aufeinander zu: Geschwindigkeiten addieren. Hinterherfahren: Geschwindigkeiten abziehen.",
                "Einholen: erst den Vorsprung ausrechnen, dann Vorsprung ÷ Unterschied der Geschwindigkeiten.",
            ),
            "Wer hin mit 60 km/h und zurück mit 30 km/h fährt, ist im Schnitt mit 40 km/h unterwegs, nicht mit 45. Die langsame Strecke dauert länger und zählt deshalb stärker.",
        ),
        ApCategory.WORK to Text(
            "Wenn mehrere zusammen arbeiten: Leistungen addieren, nicht Zeiten.",
            "Arbeiten zwei zusammen, addierst du nicht die Zeiten, sondern die Leistungen. Am einfachsten denkst du dir die Arbeit in so viele gleiche Teile zerlegt, dass beide pro Stunde eine ganze Zahl davon schaffen.",
            listOf(
                "Arbeit in Teile zerlegen: das kgV der beiden Zeiten.",
                "Für jeden ausrechnen, wie viele Teile er pro Stunde schafft.",
                "Zusammen pro Stunde: addieren – ein Abfluss wird abgezogen.",
                "Zeit = alle Teile ÷ Teile pro Stunde.",
                "Kommen später Leute dazu: Gesamtaufwand minus Erledigtes, den Rest durch die neue Leistung teilen.",
            ),
            "Zusammen geht es immer schneller als der Schnellere allein. Ist eine Antwort länger als die kürzere Einzelzeit, ist sie sicher falsch.",
        ),
        ApCategory.MIX to Text(
            "Konzentrationen beim Verdünnen und Mischen berechnen.",
            "Beim Verdünnen und Mischen ändert sich die Menge des Wirkstoffs nicht – nur die Menge der Lösung. Rechne deshalb immer zuerst aus, wie viel Wirkstoff vorhanden ist.",
            listOf(
                "Wirkstoff = Menge × Prozent ÷ 100.",
                "Neue Konzentration = Wirkstoff ÷ neue Gesamtmenge × 100.",
                "Wasser zugeben: neue Gesamtmenge ausrechnen und die alte Menge abziehen.",
                "Zwei Lösungen mischen: Wirkstoffe addieren und durch die gesamte Menge teilen.",
                "Mischungskreuz: die Abstände beider Lösungen zum Ziel bilden – sie geben das Mischungsverhältnis über Kreuz an.",
            ),
            "Den Mittelwert der beiden Prozente darfst du nur nehmen, wenn von beiden Lösungen gleich viel gemischt wird.",
        ),
        ApCategory.GROWTH to Text(
            "Verdopplungen und Halbwertszeiten zählen statt rechnen.",
            "Bei Verdopplung und Halbwertszeit zählst du zuerst, wie oft sich der Wert verdoppelt oder halbiert. Erst dann wird gerechnet.",
            listOf(
                "Anzahl = vergangene Zeit ÷ Verdopplungszeit bzw. Halbwertszeit.",
                "Faktor = 2 hoch Anzahl (2, 4, 8, 16, 32, 64 …).",
                "Wachstum: Startwert × Faktor. Abbau: Startwert ÷ Faktor.",
                "Rückwärts oder nach der Zeit gefragt: Faktor aus dem Verhältnis der Werte, dann die Halbierungen abzählen.",
            ),
            "Die Zweierpotenzen bis 2¹⁰ = 1.024 solltest du auswendig kennen – dann sind diese Aufgaben in Sekunden gelöst.",
        ),
        ApCategory.EQUATIONS to Text(
            "Summe und Unterschied, zwei Sorten, Alter: mit Gleichung oder ohne.",
            "Manche Aufgaben lassen sich am schnellsten mit einer kleinen Gleichung lösen. Oft geht es aber auch ohne x – mit der Frage „Was wäre, wenn alle gleich wären?“.",
            listOf(
                "Unbekannte benennen: Was genau ist gesucht?",
                "Summe und Unterschied: (Summe − Unterschied) ÷ 2 ergibt die kleinere Zahl.",
                "Zwei Sorten (Ampullen, Münzen): annehmen, alle wären von der kleineren Sorte, dann den fehlenden Rest verteilen.",
                "Alter: Gleichung für heute und für später aufstellen und nach x auflösen.",
                "Zwei Bestellungen: voneinander abziehen, damit eine Sorte wegfällt.",
            ),
            "Prüfe das Ergebnis kurz am Text: Stimmen Summe und Unterschied wieder? Das kostet fünf Sekunden und rettet Punkte.",
        ),
        ApCategory.COMBI to Text(
            "Reihenfolgen, Ämter, Teams und Händeschütteln abzählen.",
            "Beim Zählen von Möglichkeiten besetzt du Plätze nacheinander. Die wichtigste Frage lautet: Spielt die Reihenfolge eine Rolle?",
            listOf(
                "Alle anordnen: n × (n − 1) × … × 1.",
                "Verschiedene Ämter besetzen: n × (n − 1) × (n − 2) … – die Reihenfolge zählt.",
                "Gleichwertige Gruppe: wie oben rechnen, dann durch die Zahl der Reihenfolgen teilen (bei drei Personen durch 6).",
                "Jeder mit jedem (Händeschütteln, Anstoßen): n × (n − 1) ÷ 2.",
                "Unabhängige Auswahl („Vorspeise und Hauptgang“): Möglichkeiten multiplizieren.",
            ),
            "Lies genau, wer alles mitzählt – Gastgeber, Ärztin, man selbst. Eine vergessene Person verändert das Ergebnis deutlich.",
        ),
        ApCategory.PROB to Text(
            "Günstige durch mögliche Fälle, „und“ heißt malnehmen.",
            "Eine Wahrscheinlichkeit ist der Anteil der günstigen an allen möglichen Fällen. Für mehrere Schritte nacheinander gilt: „und“ heißt multiplizieren.",
            listOf(
                "Ein Schritt: günstige Fälle ÷ mögliche Fälle, dann kürzen.",
                "Nacheinander ohne Zurücklegen: im zweiten Schritt ist eine Möglichkeit weniger da.",
                "Unabhängige Ereignisse: die Wahrscheinlichkeiten multiplizieren.",
                "„Mindestens einmal“: 1 minus „keinmal“ (Gegenwahrscheinlichkeit).",
            ),
            "Bei zwei Würfeln gibt es 36 gleich wahrscheinliche Paare. Die Summe 7 kommt am häufigsten vor: 6 von 36.",
        ),
    )

    private val easy = setOf(ApCategory.RATIO, ApCategory.PERCENT, ApCategory.RELATIONS, ApCategory.COMBI)

    private fun skillFor(category: ApCategory, seed: Long): Skill {
        val t = texts.getValue(category)
        return Skill(
            id = category.skillId,
            title = category.title,
            method = category.method,
            book = book,
            summary = t.summary,
            explanation = listOf(
                Block.Text(t.intro),
                Block.Steps(t.steps),
                example(category, seed),
                Block.Tip(t.tip),
            ),
            goals = if (category in easy) easyGoals else goals,
            guidedCount = 3,
            practiceCount = 8,
            generator = { r -> ApTasks.typed(ApTasks.task(category, ApTasks.levelDifficulty(r), r)) },
        )
    }

    private val mixed = Skill(
        id = "w13.mixed",
        title = "Gemischt wie im Test",
        method = "Aufgabenart erkennen, Ballast streichen",
        book = book,
        summary = "Alle Aufgabenarten durcheinander, mit Antwortmöglichkeiten A bis E.",
        explanation = listOf(
            Block.Text("Im Test kommen alle Aufgabenarten durcheinander – mit fünf Antwortmöglichkeiten im TMSnat und vier im HAM-Nat. Hier übst du genau das: erst die Art der Aufgabe erkennen, dann den passenden Weg wählen."),
            Block.Steps(
                listOf(
                    "Lesen und die Frage festhalten: Was ist gesucht, in welcher Einheit?",
                    "Ballast streichen: Angaben, die für die Frage keine Rolle spielen.",
                    "Den passenden Weg wählen: Dreisatz, Prozent, Mischung, Kombinatorik …",
                    "Rechnen oder überschlagen – die Antworten stehen nach Größe sortiert.",
                    "Ergebnis mit den Antworten vergleichen. Ohne Idee: raten, markieren, weiter.",
                ),
            ),
            Block.Example(ApTasks.mixed(5, 2, Random(1313)).copy(skillId = "w13.mixed")),
            Block.Tip("Für 16 Aufgaben hast du im TMSnat 25 Minuten – gut 90 Sekunden pro Aufgabe. Bleib nie länger als zwei Minuten an einer Aufgabe hängen und lass keine Antwort leer."),
        ),
        goals = Goals(testSize = 8, minAccuracy = 0.75, star1 = 120.0, star2 = 94.0, star3 = 70.0),
        guidedCount = 3,
        practiceCount = 8,
        generator = { r -> ApTasks.mixed(5, ApTasks.levelDifficulty(r), r).copy(skillId = "w13.mixed") },
    )

    val world = World(
        id = "w13",
        number = 13,
        title = "Medizinertest: Arithmetisches Problemlösen",
        subtitle = "Ergänzung · TMSnat und HAM-Nat",
        skills = ApCategory.entries.mapIndexed { i, c -> skillFor(c, 1300L + i) } + mixed,
        independent = true,
    )
}
