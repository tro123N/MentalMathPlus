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
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.random.Random

/**
 * Welt 14 (Ergänzung, nicht aus dem Buch): Einheiten umrechnen – Vorsilben, Zehnerpotenzen,
 * Länge, Masse, Volumen, Fläche, Zeit, Geschwindigkeit, Konzentration und Physik. Eigenständig
 * (immer offen), Einstieg auch im TMSnat-Training. Unbegrenzt viele Aufgaben aus [UnitTasks].
 */
object World14 {

    private val book = BookRef(null, "Ergänzung: Einheiten umrechnen")
    private const val MU = UnitTasks.MU

    private val quick = Goals(testSize = 10, minAccuracy = 0.9, star1 = 30.0, star2 = 20.0, star3 = 12.0)
    private val medium = Goals(testSize = 10, minAccuracy = 0.9, star1 = 40.0, star2 = 28.0, star3 = 18.0)

    /** IDs der Levels – für den Einstieg im TMSnat-Training. */
    val skillIds: List<String> get() = world.skills.map { it.id }

    private fun skill(
        id: String,
        title: String,
        method: String,
        summary: String,
        intro: String,
        steps: List<String>,
        formulas: List<String>,
        tip: String,
        goals: Goals,
        exampleSeed: Long,
        generator: (String, Random) -> Problem,
    ): Skill {
        val full = "w14.$id"
        return Skill(
            id = full,
            title = title,
            method = method,
            book = book,
            summary = summary,
            explanation = listOf(Block.Text(intro), Block.Steps(steps)) + formulas.map { Block.Formula(it) } +
                listOf(Block.Example(generator(full, Random(exampleSeed))), Block.Tip(tip)),
            goals = goals,
            guidedCount = 3,
            practiceCount = 10,
            generator = { r -> generator(full, r) },
        )
    }

    private val skills = listOf(
        skill(
            "prefix", "Vorsilben und Zehnerpotenzen", "Exponenten-Abstand der Vorsilben",
            "Kilo, Mega, Milli, Mikro, Nano: welche Zehnerpotenz hinter jeder Vorsilbe steckt.",
            "Vor fast jeder Einheit kann eine Vorsilbe stehen. Sie sagt, mit welcher Zehnerpotenz die Grundeinheit malgenommen wird: 1 km sind 10³ m, 1 µg sind 10⁻⁶ g. Wer die Tabelle kennt, rechnet jede Einheit gleich um – egal ob Gramm, Watt, Hertz oder Mol.",
            listOf(
                "Zu beiden Vorsilben den Exponenten aufschreiben (ohne Vorsilbe: 10⁰).",
                "Abstand der Exponenten bestimmen: von k (3) nach m (−3) sind es 6 Stellen.",
                "Von groß nach klein: mal 10 hoch Abstand – das Komma rückt nach rechts.",
                "Von klein nach groß: geteilt durch 10 hoch Abstand – das Komma rückt nach links.",
            ),
            listOf(
                "Tera T = 10¹² · Giga G = 10⁹ · Mega M = 10⁶ · Kilo k = 10³ · Hekto h = 10²",
                "Dezi d = 10⁻¹ · Zenti c = 10⁻² · Milli m = 10⁻³ · Mikro $MU = 10⁻⁶ · Nano n = 10⁻⁹ · Piko p = 10⁻¹²",
            ),
            "Ab Kilo und ab Milli geht es in Dreierschritten weiter: Kilo, Mega, Giga, Tera nach oben, Milli, Mikro, Nano, Piko nach unten. Zwischen zwei Nachbarn liegt also immer der Faktor 1.000.",
            quick, 1401,
        ) { id, r -> if (r.nextBoolean()) UnitTasks.prefixChoice(id, r) else UnitTasks.prefixConvert(id, r) },
        skill(
            "length", "Längen", "Faktor zwischen den Einheiten",
            "Kilometer bis Nanometer sicher umrechnen.",
            "Bei Längen hilft die Treppe km – m – dm – cm – mm: Zwischen m, dm, cm und mm liegt jeweils der Faktor 10, von km zu m der Faktor 1.000. Unterhalb von mm geht es in Tausenderschritten weiter: 1 mm = 1.000 µm, 1 µm = 1.000 nm.",
            listOf(
                "Faktor zwischen den beiden Einheiten bestimmen (Stufen der Treppe malnehmen).",
                "In die kleinere Einheit: mal Faktor, die Zahl wird größer.",
                "In die größere Einheit: geteilt durch den Faktor, die Zahl wird kleiner.",
                "Verschiedene Einheiten erst angleichen, dann zusammenzählen.",
            ),
            listOf("1 km = 1.000 m · 1 m = 10 dm = 100 cm = 1.000 mm · 1 mm = 1.000 $MU" + "m · 1 $MU" + "m = 1.000 nm"),
            "Prüfe am Ende kurz die Richtung: Eine Strecke in Zentimetern hat immer eine größere Zahl als dieselbe Strecke in Metern.",
            quick, 1402,
        ) { id, r -> if (r.nextInt(10) < 7) UnitTasks.convert(id, UnitTasks.LENGTH, r) else UnitTasks.sum(id, UnitTasks.LENGTH, r) },
        skill(
            "mass", "Massen", "Faktor zwischen den Einheiten",
            "Tonnen, Kilogramm, Gramm, Milligramm, Mikrogramm und Nanogramm.",
            "Bei Massen ist jeder Schritt ein Tausenderschritt: t – kg – g – mg – µg – ng. Gerade in Medikamentenaufgaben tauchen mg und µg oft nebeneinander auf – hier entstehen die meisten Faktor-1.000-Fehler.",
            listOf(
                "Abzählen, wie viele Tausenderschritte zwischen den Einheiten liegen.",
                "Je Schritt das Komma um drei Stellen verschieben.",
                "In die kleinere Einheit nach rechts (mal), in die größere nach links (geteilt).",
            ),
            listOf("1 t = 1.000 kg · 1 kg = 1.000 g · 1 g = 1.000 mg · 1 mg = 1.000 $MU" + "g · 1 $MU" + "g = 1.000 ng"),
            "Von g zu µg sind es zwei Tausenderschritte – also mal 1.000.000, nicht mal 1.000.",
            quick, 1403,
        ) { id, r -> if (r.nextInt(10) < 7) UnitTasks.convert(id, UnitTasks.MASS, r) else UnitTasks.sum(id, UnitTasks.MASS, r) },
        skill(
            "volume", "Volumen und Liter", "Längenfaktor hoch 3",
            "Liter, Milliliter und Kubikmeter – und warum 1 l genau 1 dm³ ist.",
            "Ein Liter ist ein Würfel mit 1 dm Kantenlänge, ein Milliliter ein Würfel mit 1 cm Kantenlänge. Deshalb gilt 1 l = 1 dm³ und 1 ml = 1 cm³. Bei Kubik-Einheiten kommt der Längenfaktor dreimal vor: 1 m = 10 dm, also 1 m³ = 10 × 10 × 10 dm³ = 1.000 l.",
            listOf(
                "Liter-Einheiten (hl, l, dl, cl, ml, µl) wie Längen umrechnen: meist Zehner- oder Tausenderschritte.",
                "Kubik-Einheiten: Längenfaktor dreimal malnehmen (10 × 10 × 10 = 1.000).",
                "Zwischen beiden Welten wechseln: 1 l = 1 dm³, 1 ml = 1 cm³, 1 µl = 1 mm³.",
            ),
            listOf("1 m³ = 1.000 l · 1 hl = 100 l · 1 l = 10 dl = 100 cl = 1.000 ml · 1 ml = 1 cm³ = 1.000 $MU" + "l"),
            "Wer bei m³ in cm³ nur mal 100 rechnet, liegt um den Faktor 10.000 daneben: richtig ist 100 × 100 × 100 = 1.000.000.",
            quick, 1404,
        ) { id, r -> if (r.nextInt(10) < 8) UnitTasks.convert(id, UnitTasks.VOLUME, r) else UnitTasks.sum(id, UnitTasks.VOLUME, r) },
        skill(
            "area", "Flächen umrechnen", "Längenfaktor hoch 2",
            "Quadratmeter, Hektar und Quadratzentimeter: der Längenfaktor zählt doppelt.",
            "Eine Fläche ist Länge mal Breite. Darum kommt der Längenfaktor zweimal vor: 1 m = 100 cm, also 1 m² = 100 × 100 cm² = 10.000 cm². Zwischen zwei benachbarten Flächeneinheiten (m², dm², cm², mm²) liegt deshalb der Faktor 100.",
            listOf(
                "Längenfaktor der zugehörigen Längeneinheiten bestimmen.",
                "Diesen Faktor zweimal malnehmen – das ist der Flächenfaktor.",
                "Wie gewohnt: in die kleinere Einheit mal, in die größere geteilt.",
                "Hektar und Ar merken: 1 a = 100 m², 1 ha = 10.000 m², 1 km² = 100 ha.",
            ),
            listOf("1 km² = 100 ha · 1 ha = 100 a · 1 a = 100 m² · 1 m² = 100 dm² = 10.000 cm²"),
            "Ein Hektar ist ein Quadrat mit 100 m Seitenlänge – ungefähr ein Fußballfeld und ein bisschen mehr.",
            medium, 1405,
        ) { id, r -> UnitTasks.convert(id, UnitTasks.AREA, r, maxGap = 2) },
        skill(
            "time", "Zeit und Uhrzeit", "Mal 60, nicht mal 100",
            "Dezimalstunden, Stunden und Minuten, Sekunden, Tage und Zeitspannen.",
            "Zeit ist die tückischste Größe, weil sie nicht im Zehnersystem läuft: 1 h = 60 min, 1 min = 60 s, 1 d = 24 h. Darum sind 2,5 h nicht 2 h 50 min, sondern 2 h 30 min. Erst unterhalb der Sekunde wird es wieder dezimal: 1 s = 1.000 ms.",
            listOf(
                "Dezimalstunden in Minuten: mal 60 (0,25 h = 15 min, 0,5 h = 30 min, 0,75 h = 45 min).",
                "Minuten in Stunden: durch 60.",
                "Sekunden in Stunden: zweimal durch 60, also durch 3.600.",
                "Zeitspannen: erst bis zur vollen Stunde, dann volle Stunden, dann den Rest dazu.",
            ),
            listOf("1 d = 24 h · 1 h = 60 min = 3.600 s · 1 s = 1.000 ms = 1.000.000 $MU" + "s"),
            "Merke die Brüche einer Stunde: 0,1 h = 6 min, 0,2 h = 12 min, 0,25 h = 15 min. Damit rechnest du Dezimalstunden in Sekunden um.",
            medium, 1406,
        ) { id, r -> UnitTasks.time(id, r) },
        skill(
            "speed", "Geschwindigkeit", "1 m/s = 3,6 km/h",
            "km/h und m/s, Meter pro Minute und der Weg in wenigen Minuten.",
            "Für Geschwindigkeiten reicht eine Zahl: 1 m/s = 3,6 km/h. Sie kommt daher, dass eine Stunde 3.600 Sekunden hat und ein Kilometer 1.000 Meter: 3.600 ÷ 1.000 = 3,6.",
            listOf(
                "Von m/s in km/h: mal 3,6.",
                "Von km/h in m/s: geteilt durch 3,6.",
                "Andere Kombinationen: Zähler und Nenner einzeln umrechnen (Meter in km, Minuten in Stunden).",
                "Weg in wenigen Minuten: Tempo durch 60 teilen (pro Minute), dann mal die Minuten.",
            ),
            listOf("1 m/s = 3,6 km/h · 36 km/h = 10 m/s · 72 km/h = 20 m/s · 108 km/h = 30 m/s"),
            "Zum Überschlagen: km/h durch 4 ist etwas weniger als m/s. 100 km/h sind knapp 28 m/s.",
            medium, 1407,
        ) { id, r -> UnitTasks.speed(id, r) },
        skill(
            "conc", "Konzentration und Dichte", "Zähler und Nenner getrennt umrechnen",
            "mg/ml, g/l, mmol/l, Prozentlösungen, Tropfen und Dichte.",
            "Eine Konzentration ist ein Bruch aus zwei Einheiten, zum Beispiel Milligramm pro Milliliter. Ändern sich Zähler und Nenner um denselben Faktor, bleibt die Zahl gleich: 1 mg/ml = 1 g/l. Eine Prozentangabe bei Lösungen bedeutet Gramm pro 100 ml.",
            listOf(
                "Zähler umrechnen, Nenner umrechnen – dann beide Faktoren verrechnen.",
                "Prozentlösungen: 1 % = 1 g pro 100 ml = 10 g/l = 10 mg/ml.",
                "Wirkstoffmenge: Prozent × (Milliliter ÷ 100) ergibt Gramm.",
                "Tropfen: 20 Tropfen = 1 ml. Dichte: Masse = Dichte × Volumen.",
            ),
            listOf("1 mg/ml = 1 g/l = 1.000 $MU" + "g/ml · 1 g/dl = 10 g/l · 1 mol/l = 1.000 mmol/l · 1 % = 10 mg/ml"),
            "Wasser hat die Dichte 1 g/ml: 1 l Wasser wiegt 1 kg. Damit kannst du jede Mengenangabe schnell überschlagen.",
            medium, 1408,
        ) { id, r -> UnitTasks.concentration(id, r) },
        skill(
            "physics", "Druck, Energie und Strom", "Vorsilben plus Sonderfaktoren",
            "bar und Pascal, Joule und Kilowattstunden, Watt, Ampere, Volt, Hertz, Mol und Kelvin.",
            "Die meisten physikalischen Einheiten rechnest du mit den Vorsilben um (kW, mA, MHz, mmol). Dazu kommen ein paar Sonderfälle, die man einmal lernen muss: 1 bar = 100.000 Pa, 1 kWh = 3,6 MJ, 1 kcal = 4,184 kJ und Kelvin = Grad Celsius + 273,15.",
            listOf(
                "Gibt es nur eine Vorsilbe: wie in Level 1 über den Exponenten-Abstand.",
                "Druck: 1 bar = 1.000 hPa = 100.000 Pa; 1 hPa = 1 mbar.",
                "Energie: 1 Wh = 3.600 J, also 1 kWh = 3,6 MJ. Energie = Leistung × Zeit (kW × h = kWh).",
                "Temperatur: Kelvin = °C + 273,15.",
            ),
            listOf("1 bar = 1.000 hPa = 100.000 Pa · 1 kWh = 3,6 MJ · 1 kcal = 4,184 kJ · 0 °C = 273,15 K"),
            "Der Luftdruck auf Meereshöhe liegt bei etwa 1.013 hPa – das sind gut 1 bar.",
            medium, 1409,
        ) { id, r -> UnitTasks.physics(id, r) },
        skill(
            "sci", "Wissenschaftliche Schreibweise", "Zahlen und Exponenten getrennt",
            "Sehr große und kleine Zahlen als Zahl mal Zehnerpotenz – und damit rechnen.",
            "Sehr große und sehr kleine Zahlen schreibt man als Zahl zwischen 1 und 10 mal einer Zehnerpotenz: 0,00042 = 4,2 · 10⁻⁴. Der Exponent zählt, wie viele Stellen das Komma wandert. Beim Rechnen behandelst du Zahlen und Zehnerpotenzen getrennt.",
            listOf(
                "Komma so lange verschieben, bis genau eine Ziffer (nicht 0) davor steht – die Schritte zählen.",
                "Nach links verschoben (große Zahl): positiver Exponent. Nach rechts (kleine Zahl): negativer Exponent.",
                "Malnehmen: Zahlen malnehmen, Exponenten addieren. Teilen: Zahlen teilen, Exponenten abziehen.",
                "Ist die Zahl danach 10 oder größer: eine Stelle in den Exponenten schieben (20 · 10⁸ = 2 · 10⁹).",
            ),
            listOf("10³ · 10⁻⁵ = 10⁻² · 10⁶ ÷ 10² = 10⁴ · (3 · 10⁴) · (2 · 10⁻⁶) = 6 · 10⁻²"),
            "Vorsicht mit Minuszeichen: 10⁶ ÷ 10⁻² = 10⁸, denn 6 − (−2) = 8.",
            medium, 1410,
        ) { id, r -> UnitTasks.scientific(id, r) },
        skill(
            "mixed", "Alle Einheiten gemischt", "Größe erkennen, Faktor wählen",
            "Alle Arten von Umrechnungen durcheinander – wie im Test.",
            "Hier kommen alle Größen durcheinander: Vorsilben, Länge, Masse, Volumen, Fläche, Zeit, Tempo, Konzentration, Physik und Zehnerpotenzen. Erkenne zuerst die Größe, dann den passenden Faktor.",
            listOf(
                "Welche Größe ist es? Daran hängt der Faktor (10, 100, 1.000, 60, 3,6 …).",
                "Richtung prüfen: in die kleinere Einheit wird die Zahl größer.",
                "Rechnen und kurz überschlagen, ob das Ergebnis passt.",
            ),
            emptyList(),
            "Die meisten Fehler sind Faktor-10-Fehler. Ein kurzer Blick auf die Größenordnung („sind 3,5 km wirklich 350 m?“) fängt sie ab.",
            medium, 1411,
        ) { id, r -> UnitTasks.mixed(id, r) },
    )

    val world = World(
        id = "w14",
        number = 14,
        title = "Einheiten umrechnen",
        subtitle = "Ergänzung · Vorsilben, Zeit, Konzentration und mehr",
        skills = skills,
        independent = true,
    )
}
