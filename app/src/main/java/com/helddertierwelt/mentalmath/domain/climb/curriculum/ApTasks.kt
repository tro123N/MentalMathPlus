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

import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.math.Num
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Mistake
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.random.Random

/**
 * Kategorien des TMSnat-Untertests „Arithmetisches Problemlösen“ (aus dem HAM-Nat übernommen).
 * Jede Kategorie ist ein Level in Welt 13; die Aufgaben sind eigene Textaufgaben.
 */
enum class ApCategory(val skillId: String, val title: String, val method: String) {
    RATIO("w13.ratio", "Dreisatz und Antiproportionalität", "Erst auf eins, dann auf das Gesuchte"),
    PERCENT("w13.percent", "Prozente in Textaufgaben", "Grundwert, Prozentwert, Prozentsatz"),
    RELATIONS("w13.relations", "Beziehungsketten", "Von der bekannten Größe aus rückwärts"),
    FRACTIONS("w13.fractions", "Anteile und Restmengen", "Rest als Anteil, dann hochrechnen"),
    UNITS("w13.units", "Einheiten und Dosierung", "Erst Einheiten angleichen, dann rechnen"),
    SPEED("w13.speed", "Weg, Zeit und Tempo", "Weg = Tempo × Zeit"),
    WORK("w13.work", "Gemeinsam arbeiten", "Leistungen pro Stunde addieren"),
    MIX("w13.mix", "Mischen und Verdünnen", "Die Menge Wirkstoff bleibt gleich"),
    GROWTH("w13.growth", "Wachstum und Halbwertszeit", "Verdopplungen zählen"),
    EQUATIONS("w13.equations", "Gleichungen aus dem Text", "Unbekannte benennen, Gleichung aufstellen"),
    COMBI("w13.combi", "Zählen und Anordnen", "Plätze nacheinander besetzen"),
    PROB("w13.prob", "Wahrscheinlichkeiten", "Günstige durch mögliche Fälle"),
    ;

    companion object {
        fun of(skillId: String): ApCategory? = entries.firstOrNull { it.skillId == skillId }

        /** Kategorie einer Aufgabe – auch im gemischten Level, dessen Aufgaben die Level-ID tragen. */
        fun of(problem: Problem): ApCategory? = of(problem.skillId) ?: entries.firstOrNull { it.method == problem.method }
    }
}

/** Ergebnis eines typischen Fehlers; [why] erklärt ihn (null: nur eine nahe, plausible Zahl). */
data class Wrong(val value: BigDecimal, val why: String?)

/** Wie [Wrong], für Brüche. */
data class WrongFrac(val value: Pair<Long, Long>, val why: String?)

/** Eine Textaufgabe mit Lösung, Lösungsweg und den Ergebnissen typischer Fehler. */
data class ApTask(
    val category: ApCategory,
    val key: String,
    val text: String,
    /** Einheit der Antwort, z. B. „ml“ oder „%“; null bei reinen Anzahlen ohne Einheit. */
    val unit: String?,
    /** Zahlenergebnis; null, wenn das Ergebnis ein Bruch ist. */
    val value: BigDecimal?,
    /** Ergebnis als gekürzter Bruch (Wahrscheinlichkeiten). */
    val frac: Pair<Long, Long>? = null,
    val steps: List<Step>,
    /** Ergebnisse, die bei typischen Fehlern herauskommen – die besten falschen Antworten. */
    val wrong: List<Wrong> = emptyList(),
    val wrongFrac: List<WrongFrac> = emptyList(),
)

/** Erzeugt beliebig viele Aufgaben zum arithmetischen Problemlösen. */
object ApTasks {

    // --- Kleine Helfer ------------------------------------------------------------------------

    private fun bd(x: Long): BigDecimal = BigDecimal.valueOf(x)

    private fun isInt(x: BigDecimal): Boolean = x.stripTrailingZeros().scale() <= 0

    private fun reduce(n: Long, d: Long): Pair<Long, Long> {
        val g = Num.gcd(n, d)
        return (n / g) to (d / g)
    }

    private fun lcm(a: Long, b: Long): Long = a / Num.gcd(a, b) * b

    private fun fracStep(label: String, n: Long, d: Long, hint: String? = null): Step {
        val (rn, rd) = reduce(n, d)
        return Step(label, Expected.Frac(rn, rd), InputKind.FRACTION, hint)
    }

    /** Wert als Schritt: ganze Zahl oder Kommazahl. */
    private fun valueStep(label: String, value: BigDecimal, hint: String? = null): Step =
        if (isInt(value)) step(label, value.toLong(), hint) else decStep(label, value, null, hint)

    private fun dec(x: BigDecimal): String = Fmt.dec(x)

    /** Typischer Fehler mit Erklärung. */
    private infix fun BigDecimal.because(why: String): Wrong = Wrong(this, why)

    /** Nur eine nahe, plausible falsche Zahl ohne bestimmten Denkfehler. */
    private fun near(x: BigDecimal): Wrong = Wrong(x, null)

    private infix fun Pair<Long, Long>.because(why: String): WrongFrac = WrongFrac(this, why)

    private fun nearFrac(x: Pair<Long, Long>): WrongFrac = WrongFrac(x, null)

    private fun sameFrac(a: Pair<Long, Long>, b: Pair<Long, Long>): Boolean = a.first * b.second == b.first * a.second

    /** Unwichtige Angaben, die im Test gern eingestreut werden. Sie passen zu keiner Rechnung. */
    private val ballast: List<(Random) -> String> = listOf(
        { r -> "Das Gebäude hat ${r.between(3, 9)} Stockwerke." },
        { r -> "Auf dem Parkplatz ist Platz für ${r.between(4, 30) * 10} Autos." },
        { r -> "Die Cafeteria öffnet um ${r.between(6, 9)}:30 Uhr." },
        { r -> "Das Labor wurde vor ${r.between(3, 25)} Jahren renoviert." },
        { r -> "Im Wartezimmer stehen ${r.between(8, 24)} Stühle." },
    )

    /** Text aus Angaben und Frage; ab Stufe 2 oft mit einer unwichtigen Zusatzangabe. */
    private fun compose(facts: List<String>, question: String, d: Int, r: Random): String {
        val list = facts.toMutableList()
        // Nie an den Anfang: Die Aufgabe soll mit ihrer eigentlichen Situation beginnen.
        if (d >= 2 && r.nextInt(100) < 55) list.add(r.nextInt(1, list.size + 1), r.pick(ballast)(r))
        return list.joinToString(" ") + " " + question
    }

    // --- Aufgaben erzeugen --------------------------------------------------------------------

    /** Eine Aufgabe der Kategorie in Stufe [difficulty] (1 leicht bis 3 schwer). */
    fun task(category: ApCategory, difficulty: Int, r: Random): ApTask {
        val d = difficulty.coerceIn(1, 3)
        return when (category) {
            ApCategory.RATIO -> ratio(d, r)
            ApCategory.PERCENT -> percent(d, r)
            ApCategory.RELATIONS -> relations(d, r)
            ApCategory.FRACTIONS -> fractions(d, r)
            ApCategory.UNITS -> units(d, r)
            ApCategory.SPEED -> speed(d, r)
            ApCategory.WORK -> work(d, r)
            ApCategory.MIX -> mix(d, r)
            ApCategory.GROWTH -> growth(d, r)
            ApCategory.EQUATIONS -> equations(d, r)
            ApCategory.COMBI -> combi(d, r)
            ApCategory.PROB -> prob(d, r)
        }
    }

    /** Stufe für Level-Aufgaben: meist mittel, manchmal leicht oder schwer. */
    fun levelDifficulty(r: Random): Int = when (r.nextInt(10)) {
        0, 1, 2 -> 1
        3, 4, 5, 6 -> 2
        else -> 3
    }

    private fun questionMark(unit: String?): String = if (unit == null) "?" else "? $unit"

    /** Aufgabe zum Eintippen. */
    fun typed(t: ApTask): Problem {
        val frac = t.frac
        val value = t.value
        val (expected, input) = when {
            frac != null -> Expected.Frac(frac.first, frac.second) to InputKind.FRACTION
            value != null && isInt(value) -> Expected.Exact(value) to InputKind.INTEGER
            else -> Expected.Exact(value!!) to InputKind.DECIMAL
        }
        // Typische Fehler auch beim Eintippen erkennen (Levels von Welt 13).
        val mistakes = if (frac != null) {
            t.wrongFrac.mapNotNull { w -> w.why?.takeIf { !sameFrac(w.value, frac) }?.let { Mistake(Expected.Frac(w.value.first, w.value.second), it) } }
        } else {
            t.wrong.mapNotNull { w -> w.why?.takeIf { w.value.compareTo(value!!) != 0 }?.let { Mistake(Expected.Exact(w.value), it) } }
        }
        return Problem(
            skillId = t.category.skillId,
            key = t.key,
            prompt = Prompt(text = questionMark(t.unit), caption = t.text),
            expected = expected,
            input = input,
            steps = t.steps,
            method = t.category.method,
            mistakes = mistakes,
        )
    }

    private val singular = mapOf(
        "Tage" to "Tag", "Stunden" to "Stunde", "Minuten" to "Minute", "Tabletten" to "Tablette",
        "Patienten" to "Patient", "Betten" to "Bett", "Proben" to "Probe", "Menüs" to "Menü",
        "Spritzen" to "Spritze", "Ampullen" to "Ampulle", "Flaschen" to "Flasche", "Teams" to "Team",
        "Bücher" to "Buch", "Jahre" to "Jahr",
    )

    /** Text einer Antwortmöglichkeit, z. B. „360 ml“ oder „3/10“. */
    fun optionText(value: BigDecimal?, frac: Pair<Long, Long>?, unit: String?): String = when {
        frac != null -> "${frac.first}/${frac.second}"
        unit == null -> dec(value!!)
        value!!.compareTo(BigDecimal.ONE) == 0 -> "1 ${singular[unit] ?: unit}"
        else -> "${dec(value)} $unit"
    }

    /** Aufgabe mit [options] Antwortmöglichkeiten (im TMSnat A–E), nach Größe sortiert. */
    fun choice(t: ApTask, options: Int, r: Random): Problem {
        val labels = "ABCDE"
        val n = options.coerceIn(3, 5)
        val texts: List<String>
        val correctIndex: Int
        val mistakes: List<Mistake>
        val frac = t.frac
        if (frac != null) {
            val all = fracOptions(frac, t.wrongFrac, n, r)
            correctIndex = all.indexOfFirst { sameFrac(it, frac) }
            texts = all.map { optionText(null, it, null) }
            mistakes = all.mapIndexedNotNull { i, f ->
                if (i == correctIndex) null else t.wrongFrac.firstOrNull { w -> w.why != null && sameFrac(w.value, f) }?.why?.let { Mistake(Expected.Choice(i), it) }
            }
        } else {
            val answer = t.value!!
            val all = numericOptions(answer, t.wrong, n, r)
            correctIndex = all.indexOfFirst { it.compareTo(answer) == 0 }
            texts = all.map { optionText(it, null, t.unit) }
            mistakes = all.mapIndexedNotNull { i, v ->
                if (i == correctIndex) null else t.wrong.firstOrNull { w -> w.why != null && w.value.compareTo(v) == 0 }?.why?.let { Mistake(Expected.Choice(i), it) }
            }
        }
        val choices = texts.mapIndexed { i, s -> "(${labels[i]}) $s" }
        val answerStep = Step(
            label = "Welche Antwort?",
            expected = Expected.Choice(correctIndex),
            input = InputKind.CHOICE,
            hint = "Das Ergebnis ist Antwort ${labels[correctIndex]}.",
        )
        return Problem(
            skillId = t.category.skillId,
            key = t.key,
            prompt = Prompt(text = questionMark(t.unit), caption = t.text),
            expected = Expected.Choice(correctIndex),
            input = InputKind.CHOICE,
            steps = t.steps + answerStep,
            method = t.category.method,
            choices = choices,
            mistakes = mistakes,
        )
    }

    private fun norm(x: BigDecimal): BigDecimal = x.stripTrailingZeros()

    /**
     * Antworten zu einer Zahl: zuerst typische Fehler, dann nahe Werte. Die richtige Antwort soll
     * an jeder Stelle (A bis E) etwa gleich oft stehen – sonst lernt man beim Üben ein Muster,
     * das es im Test nicht gibt. [below] falsche Antworten kommen deshalb unter die richtige.
     */
    private fun numericOptions(answer: BigDecimal, wrong: List<Wrong>, count: Int, r: Random): List<BigDecimal> {
        val intAnswer = isInt(answer)
        val picked = mutableListOf<BigDecimal>()
        val typicalValues = wrong.map { norm(it.value) }
        fun ok(x: BigDecimal): Boolean {
            // Null nur, wenn sie ausdrücklich ein typischer Fehler ist (z. B. „0 %“).
            if (x.signum() < 0 || (x.signum() == 0 && typicalValues.none { it.signum() == 0 })) return false
            if (intAnswer && !isInt(x)) return false
            if (!intAnswer && x.stripTrailingZeros().scale() > 2) return false
            if (x.compareTo(answer) == 0) return false
            return picked.none { it.compareTo(x) == 0 }
        }
        val below = r.nextInt(count)
        fun fill(candidates: List<BigDecimal>, under: Boolean, limit: Int) {
            for (c in candidates) {
                if (picked.count { (it < answer) == under } >= limit) return
                if ((c < answer) == under && ok(c)) picked += c
            }
        }
        val typical = typicalValues.shuffled(r)
        val step = niceStep(answer)
        val two = BigDecimal(2)
        fun steps(k: Int) = norm(answer + step * BigDecimal(k))
        fun times(f: String) = norm(answer * BigDecimal(f))
        val lower = listOf(steps(-1), steps(-2)).shuffled(r) +
            listOf(times("0.75"), norm(answer.divide(two, MathContext.DECIMAL64))).shuffled(r) +
            listOf(steps(-3), steps(-4))
        val upper = listOf(steps(1), steps(2)).shuffled(r) +
            listOf(times("1.5"), times("2")).shuffled(r) +
            listOf(steps(3), steps(4))
        fill(typical, true, below)
        fill(typical, false, count - 1 - below)
        fill(lower, true, below)
        fill(upper, false, count - 1 - below)
        // Reicht es auf einer Seite nicht (z. B. gibt es keine kleinere ganze Zahl), die andere Seite auffüllen.
        for (c in typical + upper + lower) {
            if (picked.size >= count - 1) break
            if (ok(c)) picked += c
        }
        var k = 5
        while (picked.size < count - 1) {
            val c = steps(k)
            if (ok(c)) picked += c
            k++
        }
        return (picked + norm(answer)).sortedWith { a, b -> a.compareTo(b) }
    }

    /** Abstand für „nahe“ falsche Antworten: etwa ein Zehntel der Größenordnung. */
    private fun niceStep(answer: BigDecimal): BigDecimal {
        if (!isInt(answer)) return if (answer < BigDecimal.TEN) BigDecimal("0.5") else BigDecimal.ONE
        val v = answer.toLong()
        return when {
            v < 10 -> BigDecimal.ONE
            v < 30 -> BigDecimal(2)
            v < 100 -> BigDecimal(5)
            v < 300 -> BigDecimal(10)
            v < 1_000 -> BigDecimal(50)
            v < 3_000 -> BigDecimal(100)
            v < 10_000 -> BigDecimal(500)
            v < 30_000 -> BigDecimal(1_000)
            else -> bd(Num.pow10(v.toString().length - 2) * 5)
        }
    }

    /** Antworten zu einem Bruch, nach demselben Muster wie bei Zahlen. */
    private fun fracOptions(answer: Pair<Long, Long>, wrong: List<WrongFrac>, count: Int, r: Random): List<Pair<Long, Long>> {
        val picked = mutableListOf<Pair<Long, Long>>()
        fun ok(f: Pair<Long, Long>): Boolean =
            f.first > 0 && f.second > 0 && f.first <= f.second && f.first * answer.second != answer.first * f.second &&
                picked.none { it.first * f.second == f.first * it.second }
        fun value(f: Pair<Long, Long>): Double = f.first.toDouble() / f.second
        val target = value(answer)
        val below = r.nextInt(count)
        fun fill(candidates: List<Pair<Long, Long>>, under: Boolean, limit: Int) {
            for (c in candidates) {
                if (picked.count { (value(it) < target) == under } >= limit) return
                if ((value(c) < target) == under && ok(c)) picked += c
            }
        }
        fun fracs(vararg list: Pair<Long, Long>) =
            list.filter { it.first > 0 && it.second > 0 }.map { reduce(it.first, it.second) }
        val typical = wrong.map { reduce(it.value.first, it.value.second) }.shuffled(r)
        val (n, d) = answer
        val lower = fracs(n - 1 to d, n to d + 1).shuffled(r) + fracs(n to d + 2, n to d * 2, 1L to d).shuffled(r) + fracs(n to d * 3)
        val upper = fracs(n + 1 to d, n to d - 1).shuffled(r) + fracs(n + 2 to d, n * 2 to d, n + 1 to d + 1).shuffled(r) + fracs(n + 3 to d)
        fill(typical, true, below)
        fill(typical, false, count - 1 - below)
        fill(lower, true, below)
        fill(upper, false, count - 1 - below)
        for (c in typical + upper + lower) {
            if (picked.size >= count - 1) break
            if (ok(c)) picked += c
        }
        var k = 2L
        while (picked.size < count - 1) {
            val c = reduce(1, d * k)
            if (ok(c)) picked += c
            k++
        }
        return (picked + answer).sortedBy { value(it) }
    }

    // --- 1. Dreisatz --------------------------------------------------------------------------

    private class PropCtx(val fact: (Long, Long) -> String, val question: (Long) -> String, val unit: String?)

    private val propContexts = listOf(
        PropCtx({ a, b -> "Für ${n(a)} Patienten werden pro Tag ${n(b)} Tabletten benötigt." }, { c -> "Wie viele Tabletten braucht man pro Tag für ${n(c)} Patienten?" }, "Tabletten"),
        PropCtx({ a, b -> "${n(a)} Packungen Verbandsmaterial kosten ${n(b)} €." }, { c -> "Wie viel kosten ${n(c)} Packungen?" }, "€"),
        PropCtx({ a, b -> "Eine Infusionspumpe fördert in ${n(a)} Minuten ${n(b)} ml." }, { c -> "Wie viele ml fördert sie in ${n(c)} Minuten?" }, "ml"),
        PropCtx({ a, b -> "Ein Labor wertet in ${n(a)} Stunden ${n(b)} Blutproben aus." }, { c -> "Wie viele Blutproben schafft es im gleichen Tempo in ${n(c)} Stunden?" }, "Proben"),
        PropCtx({ a, b -> "Für ${n(a)} Liter Desinfektionslösung braucht man ${n(b)} ml Konzentrat." }, { c -> "Wie viele ml Konzentrat braucht man für ${n(c)} Liter?" }, "ml"),
    )

    private class InvCtx(val fact: (Long, Long) -> String, val question: (Long) -> String, val unit: String)

    private val invContexts = listOf(
        InvCtx({ a, t -> "${n(a)} Pflegekräfte brauchen für eine Inventur ${n(t)} Stunden." }, { c -> "Wie viele Stunden brauchen ${n(c)} Pflegekräfte bei gleichem Tempo?" }, "Stunden"),
        InvCtx({ a, t -> "${n(a)} gleich starke Pumpen leeren ein Becken in ${n(t)} Minuten." }, { c -> "Wie viele Minuten brauchen ${n(c)} solche Pumpen?" }, "Minuten"),
        InvCtx({ a, t -> "Ein Vorrat an Verbandsmaterial reicht für ${n(a)} Patienten ${n(t)} Tage." }, { c -> "Wie viele Tage reicht er bei gleichem Verbrauch für ${n(c)} Patienten?" }, "Tage"),
        InvCtx({ a, t -> "Mit ${n(a)} Helfern dauert der Aufbau eines Impfzentrums ${n(t)} Stunden." }, { c -> "Wie viele Stunden dauert er mit ${n(c)} Helfern?" }, "Stunden"),
    )

    private fun ratio(d: Int, r: Random): ApTask = when (if (d == 3) r.nextInt(3) else r.nextInt(2)) {
        0 -> proportional(d, r)
        1 -> inverse(d, r)
        else -> twoFactor(r)
    }

    private fun proportional(d: Int, r: Random): ApTask {
        val ctx = r.pick(propContexts)
        val a = if (d == 1) r.betweenL(2, 6) else r.betweenL(3, 12)
        val k = if (d == 1) r.betweenL(2, 12) else r.betweenL(6, 25)
        var c = if (d == 1) r.betweenL(2, 12) else r.betweenL(5, 30)
        if (c == a) c += 1
        val b = a * k
        val answer = k * c
        val wrong = mutableListOf(
            bd(b * c) because "${n(b)} $X ${n(c)} – die Angabe gilt für ${n(a)}, nicht für 1. Erst durch ${n(a)} teilen.",
            near(bd(answer + k)),
            near(bd(k * (c - 1))),
            bd(b + c) because "${n(b)} + ${n(c)} – addiert statt mit dem Dreisatz gerechnet: erst auf 1, dann auf ${n(c)}.",
        )
        if ((b * a) % c == 0L) wrong += bd(b * a / c) because "${n(b)} $X ${n(a)} $DIV ${n(c)} – antiproportional gerechnet. Hier gilt aber „je mehr, desto mehr“."
        return ApTask(
            category = ApCategory.RATIO,
            key = "prop:$a,$b,$c",
            text = compose(listOf(ctx.fact(a, b)), ctx.question(c), d, r),
            unit = ctx.unit,
            value = bd(answer),
            steps = listOf(
                step("Für 1: ${n(b)} $DIV ${n(a)}", k, "Doppelt so viel → doppelt so viel: Erst auf eins herunterrechnen."),
                step("Für ${n(c)}: ${n(k)} $X ${n(c)}", answer, "Dann mit der gesuchten Anzahl malnehmen."),
            ),
            wrong = wrong,
        )
    }

    private fun inverse(d: Int, r: Random): ApTask {
        repeat(200) {
            val ctx = r.pick(invContexts)
            val a = if (d == 1) r.betweenL(2, 6) else r.betweenL(3, 12)
            val t = if (d == 1) r.betweenL(2, 12) else r.betweenL(4, 30)
            val total = a * t
            val divisors = (2L..maxOf(3L, a * 3)).filter { it != a && total % it == 0L && total / it >= 1 }
            if (divisors.isEmpty()) return@repeat
            val c = r.pick(divisors)
            val answer = total / c
            val wrong = mutableListOf(
                bd(total) because "${n(a)} $X ${n(t)} ist der Gesamtaufwand – er muss noch auf ${n(c)} verteilt werden.",
                near(bd(answer * 2)),
                near(bd(answer + 1)),
            )
            if ((t * c) % a == 0L) wrong += bd(t * c / a) because "${n(t)} $X ${n(c)} $DIV ${n(a)} – proportional gerechnet. Hier gilt aber „je mehr, desto weniger“."
            if (t + a > c) {
                wrong += bd(t + a - c) because if (c > a) {
                    "Den Unterschied ${n(c - a)} einfach abgezogen. Bei „je mehr, desto weniger“ wird der Gesamtaufwand geteilt."
                } else {
                    "Den Unterschied ${n(a - c)} einfach dazugezählt. Bei „je mehr, desto weniger“ wird der Gesamtaufwand geteilt."
                }
            }
            return ApTask(
                category = ApCategory.RATIO,
                key = "inv:$a,$t,$c",
                text = compose(listOf(ctx.fact(a, t)), ctx.question(c), d, r),
                unit = ctx.unit,
                value = bd(answer),
                steps = listOf(
                    step("Gesamtaufwand: ${n(a)} $X ${n(t)}", total, "Mehr Leute → weniger Zeit (antiproportional): Erst den Gesamtaufwand bestimmen."),
                    step("Verteilt auf ${n(c)}: ${n(total)} $DIV ${n(c)}", answer, "Den Aufwand gleichmäßig verteilen."),
                ),
                wrong = wrong,
            )
        }
        return proportional(d, r)
    }

    private fun twoFactor(r: Random): ApTask {
        val q = r.betweenL(2, 15)
        val m = r.betweenL(2, 6)
        val h = r.betweenL(2, 6)
        var m2 = r.betweenL(2, 9)
        if (m2 == m) m2 += 1
        var h2 = r.betweenL(2, 9)
        if (h2 == h) h2 += 1
        val p = q * m * h
        val answer = q * m2 * h2
        val ctx = r.nextInt(2)
        val text = if (ctx == 0) {
            compose(
                listOf("${n(m)} Abfüllmaschinen füllen in ${n(h)} Stunden ${n(p)} Flaschen Kochsalzlösung ab."),
                "Wie viele Flaschen füllen ${n(m2)} Maschinen in ${n(h2)} Stunden ab?",
                3,
                r,
            )
        } else {
            compose(
                listOf("${n(m)} Laborkräfte werten in ${n(h)} Stunden ${n(p)} Proben aus."),
                "Wie viele Proben werten ${n(m2)} Laborkräfte im gleichen Tempo in ${n(h2)} Stunden aus?",
                3,
                r,
            )
        }
        val wrong = mutableListOf(near(bd(answer + q)), near(bd(answer - q)))
        if ((p * m2) % m == 0L) wrong += bd(p * m2 / m) because "Nur die neue Anzahl eingerechnet (${n(p)} $X ${n(m2)} $DIV ${n(m)}) – die neue Stundenzahl fehlt."
        if ((p * h2) % h == 0L) wrong += bd(p * h2 / h) because "Nur die neue Stundenzahl eingerechnet (${n(p)} $X ${n(h2)} $DIV ${n(h)}) – die neue Anzahl fehlt."
        return ApTask(
            category = ApCategory.RATIO,
            key = "two:$q,$m,$h,$m2,$h2,$ctx",
            text = text,
            unit = if (ctx == 0) "Flaschen" else "Proben",
            value = bd(answer),
            steps = listOf(
                step("Einsatz insgesamt: ${n(m)} $X ${n(h)}", m * h, "Zwei Größen ändern sich: Erst alles auf „eine Maschine, eine Stunde“ bringen."),
                step("Pro Einheit und Stunde: ${n(p)} $DIV ${n(m * h)}", q),
                step("Gesucht: ${n(q)} $X ${n(m2)} $X ${n(h2)}", answer, "Dann mit beiden neuen Größen malnehmen."),
            ),
            wrong = wrong,
        )
    }

    // --- 2. Prozente --------------------------------------------------------------------------

    private val easyPercents = listOf(10L, 20L, 25L, 50L)
    private val percents = listOf(5L, 10L, 15L, 20L, 25L, 30L, 40L, 60L, 75L)

    /** Kleinster Grundwert, bei dem p % eine ganze Zahl ergibt. */
    private fun baseUnit(p: Long): Long = 100 / Num.gcd(p, 100)

    private fun percent(d: Int, r: Random): ApTask = when (d) {
        1 -> if (r.nextBoolean()) percentPart(d, r) else percentChange(d, r)
        2 -> when (r.nextInt(3)) {
            0 -> percentBase(d, r)
            1 -> percentRate(d, r)
            else -> percentChange(d, r)
        }
        else -> if (r.nextBoolean()) percentRelation(r) else percentTwice(r)
    }

    private fun percentPart(d: Int, r: Random): ApTask {
        val p = r.pick(if (d == 1) easyPercents else percents)
        val unit = baseUnit(p)
        val base = unit * r.betweenL(maxOf(1, 40 / unit), maxOf(2, 1_200 / unit))
        val answer = base * p / 100
        val wrong = mutableListOf(
            bd(base - answer) because "Das sind die übrigen ${n(100 - p)} % – gefragt sind die ${n(p)} %.",
            near(bd(answer * 2)),
            if ((base * p) % 10 == 0L) bd(base * p / 10) because "Durch 10 statt durch 100 geteilt – das Komma ist um eine Stelle verrutscht." else near(bd(answer + 1)),
        )
        return ApTask(
            category = ApCategory.PERCENT,
            key = "part:$base,$p",
            text = compose(
                listOf("In einer Klinik wurden im letzten Monat ${n(base)} Patienten behandelt. ${n(p)} % von ihnen waren privat versichert."),
                "Wie viele Patienten waren privat versichert?",
                d,
                r,
            ),
            unit = "Patienten",
            value = bd(answer),
            steps = listOf(step("${n(base)} $X ${n(p)} $DIV 100", answer, "Prozentwert = Grundwert × Prozentsatz ÷ 100.")),
            wrong = wrong,
        )
    }

    private fun percentChange(d: Int, r: Random): ApTask {
        val up = r.nextBoolean()
        val p = r.pick(if (d == 1) easyPercents else percents)
        val unit = baseUnit(p)
        val old = unit * r.betweenL(maxOf(1, 20 / unit), maxOf(2, 600 / unit))
        val change = old * p / 100
        val answer = if (up) old + change else old - change
        if (answer <= 0) return percentPart(d, r)
        val factor = if (up) 100 + p else 100 - p
        val wrong = mutableListOf(
            bd(change) because "Das ist nur die Änderung (${n(p)} % von ${n(old)} €) – sie muss noch ${if (up) "zum alten Preis addiert" else "vom alten Preis abgezogen"} werden.",
            bd(if (up) old - change else old + change) because "In die falsche Richtung gerechnet: ${if (up) "gesenkt statt erhöht" else "erhöht statt gesenkt"}.",
            when {
                up -> bd(old + p) because "${n(p)} € statt ${n(p)} % dazugezählt."
                old - p >= 1 -> bd(old - p) because "${n(p)} € statt ${n(p)} % abgezogen."
                else -> near(bd(answer + 1))
            },
        )
        return ApTask(
            category = ApCategory.PERCENT,
            key = "chg:$old,$p,$up",
            text = compose(
                listOf("Ein Blutdruckmessgerät kostet ${n(old)} €. Der Preis wird um ${n(p)} % ${if (up) "erhöht" else "gesenkt"}."),
                "Wie viel kostet das Gerät danach?",
                d,
                r,
            ),
            unit = "€",
            value = bd(answer),
            steps = listOf(
                step("Neuer Preis in Prozent: 100 ${if (up) "+" else MINUS} ${n(p)}", factor, "Der alte Preis ist 100 %."),
                step("${n(old)} $X ${n(factor)} $DIV 100", answer, "Gleich mit dem neuen Prozentsatz rechnen – das spart einen Schritt."),
            ),
            wrong = wrong,
        )
    }

    private fun percentBase(d: Int, r: Random): ApTask {
        val up = r.nextBoolean()
        val p = r.pick(percents)
        val factor = if (up) 100 + p else 100 - p
        val unit = 100 / Num.gcd(factor, 100)
        val old = unit * r.betweenL(maxOf(1, 20 / unit), maxOf(2, 800 / unit))
        val now = old * factor / 100
        val wrong = mutableListOf(
            (bd(now) * BigDecimal(if (up) 100 - p else 100 + p)).divide(BigDecimal(100), MathContext.DECIMAL64) because
                "Vom neuen Preis aus ${n(p)} % ${if (up) "abgezogen" else "aufgeschlagen"}. Die ${n(p)} % beziehen sich aber auf den alten Preis.",
            bd(if (up) now - p else now + p) because "${n(p)} € statt ${n(p)} % ${if (up) "abgezogen" else "dazugezählt"}.",
            bd(now) because "Das ist der Preis nachher – gefragt ist der Preis vorher.",
        )
        return ApTask(
            category = ApCategory.PERCENT,
            key = "base:$old,$p,$up",
            text = compose(
                listOf(
                    if (up) "Nach einer Preiserhöhung um ${n(p)} % kostet eine Packung Teststreifen ${n(now)} €."
                    else "Nach ${n(p)} % Rabatt kostet eine Packung Teststreifen ${n(now)} €.",
                ),
                "Wie viel hat sie vorher gekostet?",
                d,
                r,
            ),
            unit = "€",
            value = bd(old),
            steps = listOf(
                step("Neuer Preis in Prozent: 100 ${if (up) "+" else MINUS} ${n(p)}", factor, "Vorsicht Falle: Der neue Preis ist nicht 100 %, sondern ${n(factor)} %."),
                step("${n(now)} $X 100 $DIV ${n(factor)}", old, "${n(now)} € sind ${n(factor)} % – also mal 100 und durch ${n(factor)}."),
            ),
            wrong = wrong,
        )
    }

    private fun percentRate(d: Int, r: Random): ApTask {
        val down = r.nextBoolean()
        val p = r.pick(percents)
        val unit = baseUnit(p)
        val a = unit * r.betweenL(maxOf(1, 20 / unit), maxOf(2, 400 / unit))
        val diff = a * p / 100
        val b = if (down) a - diff else a + diff
        val wrong = mutableListOf(
            bd(diff) because "Das ist der Unterschied in mg – gefragt ist der Unterschied in Prozent.",
            if (down) {
                bd(100 - p) because "Das ist der neue Wert in Prozent des alten (${n(b)} mg sind ${n(100 - p)} % von ${n(a)} mg) – gefragt ist, um wie viel gesenkt wurde."
            } else {
                bd(100 + p) because "Das ist der neue Wert in Prozent des alten (${n(b)} mg sind ${n(100 + p)} % von ${n(a)} mg) – gefragt ist nur die Erhöhung."
            },
        )
        val other = BigDecimal(diff * 100).divide(bd(b), MathContext.DECIMAL64)
        if (other.stripTrailingZeros().scale() <= 1) wrong += other because "Auf den neuen Wert ${n(b)} mg bezogen statt auf den Ausgangswert ${n(a)} mg."
        return ApTask(
            category = ApCategory.PERCENT,
            key = "rate:$a,$p,$down",
            text = compose(
                listOf("Die Tagesdosis eines Medikaments wird von ${n(a)} mg auf ${n(b)} mg ${if (down) "gesenkt" else "erhöht"}."),
                "Um wie viel Prozent wurde die Dosis ${if (down) "gesenkt" else "erhöht"}?",
                d,
                r,
            ),
            unit = "%",
            value = bd(p),
            steps = listOf(
                step("Unterschied: ${n(maxOf(a, b))} $MINUS ${n(minOf(a, b))}", diff),
                step("${n(diff)} $X 100 $DIV ${n(a)}", p, "Bezogen auf den Ausgangswert ${n(a)} mg – nicht auf den neuen Wert."),
            ),
            wrong = wrong,
        )
    }

    private fun timesWord(m: Long): String = when (m) {
        2L -> "doppelt"
        3L -> "dreimal"
        4L -> "viermal"
        5L -> "fünfmal"
        else -> "${n(m)}-mal"
    }

    private fun percentRelation(r: Random): ApTask {
        repeat(200) {
            val p = r.pick(listOf(10L, 20L, 25L, 40L, 50L))
            val m = r.betweenL(2, 5)
            val pb = r.betweenL(4, 60)
            val aNow = m * pb
            if ((aNow * 100) % (100 - p) != 0L) return@repeat
            val aOld = aNow * 100 / (100 - p)
            val wrong = mutableListOf(
                bd(aNow) because "Das ist der Preis nach der Senkung – gefragt ist der Preis vorher.",
                (bd(aNow) * BigDecimal(100 + p)).divide(BigDecimal(100), MathContext.DECIMAL64) because
                    "${n(p)} % auf den heutigen Preis aufgeschlagen. Die ${n(p)} % beziehen sich aber auf den alten Preis.",
                bd(aNow + p) because "${n(p)} € statt ${n(p)} % dazugezählt.",
                if ((pb * 100) % (100 - p) == 0L) {
                    bd(pb * 100 / (100 - p)) because "Nur mit dem Preis des Pflaster-Sets gerechnet – das Desinfektionsmittel kostet aber ${timesWord(m)} so viel."
                } else {
                    near(bd(aOld + pb))
                },
            )
            return ApTask(
                category = ApCategory.PERCENT,
                key = "rel:$p,$m,$pb",
                text = compose(
                    listOf(
                        "Nach einer Preissenkung um ${n(p)} % kostet ein Desinfektionsmittel ${timesWord(m)} so viel wie ein Pflaster-Set.",
                        "Das Pflaster-Set kostet ${n(pb)} €.",
                    ),
                    "Wie viel kostete das Desinfektionsmittel vor der Preissenkung?",
                    3,
                    r,
                ),
                unit = "€",
                value = bd(aOld),
                steps = listOf(
                    step("Preis jetzt: ${n(m)} $X ${n(pb)}", aNow, "Erst den heutigen Preis aus der Beziehung ausrechnen."),
                    step("Prozent jetzt: 100 $MINUS ${n(p)}", 100 - p),
                    step("Vorher: ${n(aNow)} $X 100 $DIV ${n(100 - p)}", aOld, "Der heutige Preis sind ${n(100 - p)} % des alten."),
                ),
                wrong = wrong,
            )
        }
        return percentBase(3, r)
    }

    private fun percentTwice(r: Random): ApTask {
        val p = r.pick(listOf(10L, 20L, 30L, 40L, 50L))
        val upFirst = r.nextBoolean()
        val after = (100 + p) * (100 - p) / 100
        val loss = 100 - after
        return ApTask(
            category = ApCategory.PERCENT,
            key = "twice:$p,$upFirst",
            text = compose(
                listOf(
                    if (upFirst) "Der Preis eines Ultraschallgels steigt zuerst um ${n(p)} % und sinkt danach um ${n(p)} %."
                    else "Der Preis eines Ultraschallgels sinkt zuerst um ${n(p)} % und steigt danach um ${n(p)} %.",
                ),
                "Um wie viel Prozent liegt der Preis jetzt unter dem Ausgangspreis?",
                3,
                r,
            ),
            unit = "%",
            value = bd(loss),
            steps = listOf(
                step("Nach der ersten Änderung (Ausgang 100): 100 ${if (upFirst) "+" else MINUS} ${n(p)}", if (upFirst) 100 + p else 100 - p, "Mit 100 als Ausgangspreis rechnen."),
                step("Nach der zweiten: ${n(if (upFirst) 100 + p else 100 - p)} $X ${n(if (upFirst) 100 - p else 100 + p)} $DIV 100", after, "Die zweite Änderung bezieht sich auf den neuen Preis."),
                step("Unter dem Ausgangspreis: 100 $MINUS ${n(after)}", loss, "Plus und minus gleich viel Prozent heben sich nicht auf."),
            ),
            wrong = listOf(
                BigDecimal.ZERO because "Plus und minus ${n(p)} % heben sich nicht auf: Die zweite Änderung bezieht sich auf den schon veränderten Preis.",
                near(bd(p)),
                near(bd(2 * p)),
                near(bd(p / 2)),
                near(bd(loss * 2)),
            ),
        )
    }

    // --- 3. Beziehungsketten ------------------------------------------------------------------

    private class Names(val names: List<String>, val noun: String)

    private val nameSets = listOf(
        Names(listOf("Station A", "Station B", "Station C", "Station D"), "Betten"),
        Names(listOf("Labor 1", "Labor 2", "Labor 3", "Labor 4"), "Proben"),
        Names(listOf("Praxis Nord", "Praxis Süd", "Praxis West", "Praxis Ost"), "Patienten"),
        Names(listOf("Lea", "Tom", "Mia", "Jan"), "Bücher"),
    )

    /** Beziehung „Subjekt = f(Objekt)“. */
    private class Rel(val kind: Int, val k: Long) {
        fun apply(x: Long): Long? = when (kind) {
            0 -> x * k
            1 -> if (x % k == 0L) x / k else null
            2 -> x + k
            3 -> if (x - k > 0) x - k else null
            else -> if ((x * (100 + k)) % 100 == 0L) x * (100 + k) / 100 else null
        }

        /** Häufiger Lesefehler: Beziehung umgedreht. */
        fun flipped(x: Long): Long? = when (kind) {
            0 -> if (x % k == 0L) x / k else null
            1 -> x * k
            2 -> if (x - k > 0) x - k else null
            3 -> x + k
            else -> if ((x * (100 - k)) % 100 == 0L) x * (100 - k) / 100 else null
        }

        fun text(s: String, o: String, noun: String): String = when (kind) {
            0 -> when (k) {
                2L -> "$s hat doppelt so viele $noun wie $o."
                3L -> "$s hat dreimal so viele $noun wie $o."
                4L -> "$s hat viermal so viele $noun wie $o."
                else -> "$s hat ${n(k)}-mal so viele $noun wie $o."
            }

            1 -> when (k) {
                2L -> "$s hat halb so viele $noun wie $o."
                3L -> "$s hat ein Drittel so viele $noun wie $o."
                else -> "$s hat ein Viertel so viele $noun wie $o."
            }

            2 -> "$s hat ${n(k)} $noun mehr als $o."
            3 -> "$s hat ${n(k)} $noun weniger als $o."
            else -> "$s hat ${n(k)} % mehr $noun als $o."
        }

        fun label(x: Long): String = when (kind) {
            0 -> "${n(x)} $X ${n(k)}"
            1 -> "${n(x)} $DIV ${n(k)}"
            2 -> "${n(x)} + ${n(k)}"
            3 -> "${n(x)} $MINUS ${n(k)}"
            else -> "${n(x)} $X ${n(100 + k)} $DIV 100"
        }
    }

    private fun randomRel(d: Int, r: Random): Rel = when (r.nextInt(if (d >= 2) 5 else 4)) {
        0 -> Rel(0, r.betweenL(2, if (d == 1) 3 else 5))
        1 -> Rel(1, r.betweenL(2, if (d == 1) 2 else 4))
        2 -> Rel(2, r.betweenL(2, 15))
        3 -> Rel(3, r.betweenL(2, 12))
        else -> Rel(4, r.pick(listOf(10L, 20L, 25L, 50L)))
    }

    private fun relations(d: Int, r: Random): ApTask {
        repeat(500) {
            val set = r.pick(nameSets)
            val count = if (d == 1) 3 else 4
            val names = set.names.take(count)
            val values = LongArray(count)
            values[count - 1] = r.betweenL(6, if (d == 1) 30 else 60)
            val rels = arrayOfNulls<Rel>(count - 1)
            var ok = true
            for (i in count - 2 downTo 0) {
                var found: Rel? = null
                repeat(30) {
                    if (found == null) {
                        val rel = randomRel(d, r)
                        val v = rel.apply(values[i + 1])
                        if (v != null && v in 2..400) found = rel
                    }
                }
                val rel = found
                if (rel == null) {
                    ok = false
                    break
                }
                rels[i] = rel
                values[i] = rel.apply(values[i + 1])!!
            }
            if (!ok) return@repeat
            val statements = (0 until count - 1).map { i -> rels[i]!!.text(names[i], names[i + 1], set.noun) } +
                "${names[count - 1]} hat ${n(values[count - 1])} ${set.noun}."
            val shown = if (d == 1) statements else statements.shuffled(r)
            val askSum = d == 3 && r.nextBoolean()
            val steps = mutableListOf<Step>()
            for (i in count - 2 downTo 0) {
                steps += step("${names[i]}: ${rels[i]!!.label(values[i + 1])}", values[i], if (i == count - 2) "Bei der Größe anfangen, die als Zahl gegeben ist." else null)
            }
            val answer: Long
            val wrong = mutableListOf<Wrong>()
            if (askSum) {
                answer = values.sum()
                steps += step("Zusammen: " + values.joinToString(" + ") { n(it) }, answer)
                wrong += bd(answer - values[count - 1]) because "${names[count - 1]} beim Zusammenzählen vergessen."
                wrong += bd(values[0] * count) because "Die Anzahl von ${names[0]} mal $count genommen – die anderen haben aber verschieden viele."
            } else {
                answer = values[0]
            }
            // Falsche Antworten: eine Beziehung falsch herum gelesen.
            for (flip in 0 until count - 1) {
                var v = values[count - 1]
                var valid = true
                val chain = LongArray(count)
                chain[count - 1] = v
                for (i in count - 2 downTo 0) {
                    val next = if (i == flip) rels[i]!!.flipped(v) else rels[i]!!.apply(v)
                    if (next == null || next <= 0) {
                        valid = false
                        break
                    }
                    v = next
                    chain[i] = v
                }
                if (valid) {
                    val sentence = rels[flip]!!.text(names[flip], names[flip + 1], set.noun).removeSuffix(".")
                    wrong += bd(if (askSum) chain.sum() else chain[0]) because "Die Beziehung „$sentence“ falsch herum gelesen."
                }
            }
            val question = if (askSum) "Wie viele ${set.noun} haben alle zusammen?" else "Wie viele ${set.noun} hat ${names[0]}?"
            return ApTask(
                category = ApCategory.RELATIONS,
                key = "rel:" + names.first() + values.joinToString(",") + rels.joinToString(",") { "${it!!.kind}${it.k}" } + askSum,
                text = compose(shown, question, d, r),
                unit = set.noun,
                value = bd(answer),
                steps = steps,
                wrong = wrong,
            )
        }
        error("Beziehungskette nicht erzeugbar")
    }

    // --- 4. Anteile ---------------------------------------------------------------------------

    private fun fracText(num: Long, den: Long): String = when {
        num == 1L && den == 2L -> "die Hälfte"
        num == 1L && den == 3L -> "ein Drittel"
        num == 1L && den == 4L -> "ein Viertel"
        num == 1L && den == 5L -> "ein Fünftel"
        num == 1L && den == 6L -> "ein Sechstel"
        num == 1L && den == 8L -> "ein Achtel"
        num == 1L && den == 10L -> "ein Zehntel"
        num == 2L && den == 3L -> "zwei Drittel"
        num == 3L && den == 4L -> "drei Viertel"
        num == 2L && den == 5L -> "zwei Fünftel"
        num == 3L && den == 5L -> "drei Fünftel"
        num == 3L && den == 8L -> "drei Achtel"
        num == 3L && den == 10L -> "drei Zehntel"
        else -> "$num/$den"
    }

    private fun fractions(d: Int, r: Random): ApTask = when (d) {
        1 -> fractionOfTotal(r)
        2 -> if (r.nextBoolean()) restParts(2, r) else restOfRest(r)
        else -> if (r.nextBoolean()) restParts(3, r) else restOfRest(r)
    }

    private fun fractionOfTotal(r: Random): ApTask {
        val (num, den) = r.pick(listOf(1L to 2L, 1L to 3L, 1L to 4L, 2L to 3L, 3L to 4L, 1L to 5L, 2L to 5L, 3L to 5L, 3L to 8L))
        val total = den * r.betweenL(3, 16)
        val partUnit = total / den
        val occupied = partUnit * num
        val free = total - occupied
        return ApTask(
            category = ApCategory.FRACTIONS,
            key = "tot:$total,$num,$den",
            text = compose(
                listOf("Ein Krankenhaus hat ${n(total)} Intensivbetten. ${fracText(num, den).replaceFirstChar { it.uppercase() }} davon sind belegt."),
                "Wie viele Intensivbetten sind frei?",
                1,
                r,
            ),
            unit = "Betten",
            value = bd(free),
            steps = listOf(
                step("Ein Teil (1/$den): ${n(total)} $DIV ${n(den)}", partUnit, "Erst einen Teil ausrechnen."),
                step("Belegt: ${n(partUnit)} $X ${n(num)}", occupied),
                step("Frei: ${n(total)} $MINUS ${n(occupied)}", free, "Gefragt sind die freien Betten – nicht die belegten."),
            ),
            wrong = listOf(
                bd(occupied) because "Das sind die belegten Betten – gefragt sind die freien.",
                bd(partUnit) because "Das ist erst ein Teil (${n(total)} $DIV ${n(den)}).",
                bd(total - partUnit) because "Nur 1/${n(den)} abgezogen statt ${fracText(num, den)}.",
            ),
        )
    }

    private fun restParts(d: Int, r: Random): ApTask {
        val fracsPool = listOf(1L to 2L, 1L to 3L, 1L to 4L, 1L to 5L, 1L to 6L, 1L to 8L, 2L to 5L, 1L to 10L, 3L to 10L)
        repeat(300) {
            val parts = if (d == 3) 3 else 2
            val chosen = fracsPool.shuffled(r).take(parts)
            if (chosen.map { it.second }.toSet().size < parts) return@repeat
            val l = chosen.fold(1L) { acc, f -> lcm(acc, f.second) }
            val partCounts = chosen.map { l / it.second * it.first }
            val rest = l - partCounts.sum()
            if (rest <= 0) return@repeat
            val k = r.betweenL(2, if (d == 3) 6 else 10)
            val total = l * k
            val restCount = rest * k
            if (total > 2_000) return@repeat
            val groups = listOf("Kinder", "Jugendliche", "Senioren").take(parts)
            val factText = chosen.mapIndexed { i, f -> "${fracText(f.first, f.second)} ${groups[i]}" }
            val sentence = "Von allen Patienten einer Ambulanz sind " +
                (if (parts == 2) "${factText[0]} und ${factText[1]}." else "${factText[0]}, ${factText[1]} und ${factText[2]}.")
            val wrong = mutableListOf(near(bd(restCount * 2)), near(bd(total + k)), near(bd(restCount * parts)))
            val used = partCounts.sum()
            if ((restCount * l) % used == 0L) {
                wrong += bd(restCount * l / used) because
                    "Die ${n(restCount)} Erwachsenen so gerechnet, als wären sie die genannten Anteile (${n(used)} von ${n(l)} Teilen). Sie sind aber der Rest: ${n(rest)} von ${n(l)} Teilen."
            }
            return ApTask(
                category = ApCategory.FRACTIONS,
                key = "rest:" + chosen.joinToString(",") + ":$k",
                text = compose(listOf(sentence, "Die übrigen ${n(restCount)} Patienten sind Erwachsene."), "Wie viele Patienten hat die Ambulanz insgesamt?", d, r),
                unit = "Patienten",
                value = bd(total),
                steps = listOf(
                    step("Gemeinsamer Nenner: kgV von ${chosen.joinToString(" und ") { n(it.second) }}", l, "Alle Anteile in die gleichen Teile umrechnen."),
                    step("Rest in Teilen: ${n(l)} " + partCounts.joinToString(" ") { "$MINUS ${n(it)}" }, rest, "So viele der ${n(l)} Teile bleiben für die Erwachsenen."),
                    step("Ein Teil: ${n(restCount)} $DIV ${n(rest)}", k),
                    step("Gesamt: ${n(k)} $X ${n(l)}", total),
                ),
                wrong = wrong,
            )
        }
        return fractionOfTotal(r)
    }

    private fun restOfRest(r: Random): ApTask {
        val a = r.pick(listOf(2L, 3L, 4L, 5L))
        val b = r.pick(listOf(2L, 3L, 4L, 5L))
        val k = r.betweenL(2, 12)
        val start = a * b * k
        val afterFirst = start / a * (a - 1)
        val left = afterFirst / b * (b - 1)
        val wrong = mutableListOf(
            bd(left * a * b) because "Einfach mit ${n(a)} und ${n(b)} malgenommen. Übrig bleiben aber ${n(a - 1)}/${n(a)} und dann ${n(b - 1)}/${n(b)} – nicht 1/${n(a)} und 1/${n(b)}.",
            near(bd(start + k * (a - 1))),
            bd(afterFirst) because "Das ist der Vorrat nach Montag – gefragt ist der Vorrat am Anfang.",
        )
        val sameBase = a * b - b - a
        if (sameBase > 0 && (left * a * b) % sameBase == 0L) {
            wrong += bd(left * a * b / sameBase) because "Beide Anteile vom ursprünglichen Vorrat abgezogen. Am Dienstag wurde aber ${fracText(1, b)} der übrigen Spritzen verbraucht."
        }
        return ApTask(
            category = ApCategory.FRACTIONS,
            key = "ror:$a,$b,$k",
            text = compose(
                listOf(
                    "Aus einem Vorrat an Einmalspritzen wird am Montag ${fracText(1, a)} verbraucht.",
                    "Am Dienstag wird ${fracText(1, b)} der übrigen Spritzen verbraucht. Danach sind noch ${n(left)} Spritzen da.",
                ),
                "Wie viele Spritzen waren es am Anfang?",
                2,
                r,
            ),
            unit = "Spritzen",
            value = bd(start),
            steps = listOf(
                step("Vor Dienstag: ${n(left)} $X ${n(b)} $DIV ${n(b - 1)}", afterFirst, "Rückwärts rechnen: Nach Dienstag sind noch ${n(b - 1)}/${n(b)} des Montagsrests da."),
                step("Am Anfang: ${n(afterFirst)} $X ${n(a)} $DIV ${n(a - 1)}", start, "Nach Montag waren noch ${n(a - 1)}/${n(a)} da."),
            ),
            wrong = wrong,
        )
    }

    // --- 5. Einheiten und Dosierung -----------------------------------------------------------

    private fun units(d: Int, r: Random): ApTask = when (d) {
        1 -> if (r.nextBoolean()) tablets(d, r) else infusionRate(r)
        2 -> when (r.nextInt(3)) {
            0 -> perKilo(r)
            1 -> injection(r)
            else -> tablets(d, r)
        }
        else -> if (r.nextBoolean()) drops(r) else microgram(r)
    }

    private fun tablets(d: Int, r: Random): ApTask {
        val tab = r.pick(listOf(100L, 125L, 200L, 250L, 500L))
        repeat(50) {
            val count = r.betweenL(2, if (d == 1) 6 else 9)
            val mg = count * tab
            if (mg % 100 != 0L) return@repeat
            val grams = BigDecimal.valueOf(mg, 3)
            return ApTask(
                category = ApCategory.UNITS,
                key = "tab:$tab,$count",
                text = compose(
                    listOf("Eine Patientin soll täglich ${dec(grams)} g eines Wirkstoffs erhalten. Eine Tablette enthält ${n(tab)} mg."),
                    "Wie viele Tabletten muss sie täglich einnehmen?",
                    d,
                    r,
                ),
                unit = "Tabletten",
                value = bd(count),
                steps = listOf(
                    step("Dosis in mg: ${dec(grams)} $X 1.000", mg, "1 g = 1.000 mg – erst die Einheiten angleichen."),
                    step("Tabletten: ${n(mg)} $DIV ${n(tab)}", count),
                ),
                wrong = listOf(
                    bd(count * 10) because "Beim Umrechnen ist das Komma verrutscht: ${dec(grams)} g sind ${n(mg)} mg, nicht ${n(mg * 10)} mg.",
                    near(bd(count + 1)),
                    near(bd(count * 2)),
                ),
            )
        }
        return infusionRate(r)
    }

    private fun infusionRate(r: Random): ApTask {
        val rate = r.pick(listOf(50L, 60L, 75L, 80L, 100L, 120L, 125L, 150L, 200L, 250L))
        val hours = r.betweenL(2, 12)
        val vol = rate * hours
        val volText = if (vol % 1_000 == 0L) "${n(vol / 1_000)} l" else if (vol % 100 == 0L && vol > 1_000) "${dec(BigDecimal.valueOf(vol, 3))} l" else "${n(vol)} ml"
        val inLitres = volText.endsWith(" l")
        val steps = mutableListOf<Step>()
        if (inLitres) steps += step("In ml: ${volText.removeSuffix(" l")} $X 1.000", vol, "1 l = 1.000 ml.")
        steps += step("Pro Stunde: ${n(vol)} $DIV ${n(hours)}", rate)
        return ApTask(
            category = ApCategory.UNITS,
            key = "rate:$rate,$hours",
            text = compose(listOf("Eine Infusion mit $volText soll gleichmäßig über ${n(hours)} Stunden einlaufen."), "Wie viele ml müssen pro Stunde einlaufen?", 1, r),
            unit = "ml",
            value = bd(rate),
            steps = steps,
            wrong = listOf(
                if (inLitres) bd(rate * 10) because "Beim Umrechnen ist das Komma verrutscht: $volText sind ${n(vol)} ml." else near(bd(rate * 10)),
                if (vol % 60 == 0L) bd(vol / 60) because "Durch 60 geteilt statt durch die ${n(hours)} Stunden." else near(bd(rate + 10)),
                near(bd(rate * 2)),
            ),
        )
    }

    private fun perKilo(r: Random): ApTask {
        repeat(100) {
            val w = r.betweenL(8, 40)
            val dpk = r.pick(listOf(5L, 10L, 15L, 20L, 25L, 30L))
            val g = r.pick(listOf(2L, 3L, 4L))
            val tot = w * dpk
            if (tot % g != 0L) return@repeat
            val ans = tot / g
            return ApTask(
                category = ApCategory.UNITS,
                key = "kg:$w,$dpk,$g",
                text = compose(
                    listOf("Ein Kind wiegt ${n(w)} kg. Es soll ${n(dpk)} mg eines Antibiotikums pro kg Körpergewicht und Tag erhalten, aufgeteilt auf ${n(g)} gleich große Gaben."),
                    "Wie viel mg erhält es pro Gabe?",
                    2,
                    r,
                ),
                unit = "mg",
                value = bd(ans),
                steps = listOf(
                    step("Tagesdosis: ${n(w)} $X ${n(dpk)}", tot, "mg pro kg mal Körpergewicht."),
                    step("Pro Gabe: ${n(tot)} $DIV ${n(g)}", ans, "Gefragt ist eine einzelne Gabe."),
                ),
                wrong = listOf(
                    bd(tot) because "Das ist die Tagesdosis (${n(w)} $X ${n(dpk)}) – sie muss noch auf ${n(g)} Gaben verteilt werden.",
                    bd(tot * g) because "Mit der Zahl der Gaben malgenommen statt geteilt (${n(tot)} $X ${n(g)}).",
                    bd(dpk * g) because "Das Körpergewicht wurde nicht eingerechnet.",
                    near(bd(ans * 2)),
                ),
            )
        }
        return tablets(2, r)
    }

    private fun injection(r: Random): ApTask {
        val c = r.pick(listOf(2L, 4L, 5L, 10L, 20L, 25L, 50L))
        val ml = r.betweenL(2, 20)
        val mg = c * ml
        val useGram = mg % 100 == 0L && mg >= 100
        val doseText = if (useGram) "${dec(BigDecimal.valueOf(mg, 3))} g" else "${n(mg)} mg"
        val steps = mutableListOf<Step>()
        if (useGram) steps += step("Dosis in mg: ${dec(BigDecimal.valueOf(mg, 3))} $X 1.000", mg, "1 g = 1.000 mg.")
        steps += step("ml: ${n(mg)} $DIV ${n(c)}", ml, "Dosis geteilt durch die Menge pro ml.")
        return ApTask(
            category = ApCategory.UNITS,
            key = "inj:$c,$ml",
            text = compose(listOf("Eine Injektionslösung enthält ${n(c)} mg Wirkstoff pro ml. Ein Patient soll $doseText erhalten."), "Wie viele ml müssen aufgezogen werden?", 2, r),
            unit = "ml",
            value = bd(ml),
            steps = steps,
            wrong = listOf(
                if (useGram) bd(ml * 10) because "Beim Umrechnen ist das Komma verrutscht: $doseText sind ${n(mg)} mg." else near(bd(ml * 10)),
                bd(mg * c) because "Mit der Konzentration malgenommen statt geteilt (${n(mg)} $X ${n(c)}).",
                near(bd(ml + c)),
                if (useGram) BigDecimal.valueOf(ml, 1) because "Beim Umrechnen ist das Komma verrutscht: $doseText sind ${n(mg)} mg." else near(BigDecimal.valueOf(ml, 1)),
            ),
        )
    }

    private fun drops(r: Random): ApTask {
        val rate = r.pick(listOf(60L, 90L, 120L, 150L, 180L, 240L, 300L))
        val hours = r.betweenL(2, 10)
        val vol = rate * hours
        val perMin = rate * 20 / 60
        return ApTask(
            category = ApCategory.UNITS,
            key = "drop:$rate,$hours",
            text = compose(
                listOf("Eine Infusion von ${n(vol)} ml soll in ${n(hours)} Stunden einlaufen. 20 Tropfen entsprechen 1 ml."),
                "Wie viele Tropfen pro Minute müssen eingestellt werden?",
                3,
                r,
            ),
            unit = "Tropfen",
            value = bd(perMin),
            steps = listOf(
                step("ml pro Stunde: ${n(vol)} $DIV ${n(hours)}", rate),
                step("Tropfen pro Stunde: ${n(rate)} $X 20", rate * 20),
                step("Tropfen pro Minute: ${n(rate * 20)} $DIV 60", perMin, "Gefragt ist pro Minute – durch 60 teilen."),
            ),
            wrong = listOf(
                bd(rate * 20) because "Das sind die Tropfen pro Stunde – gefragt ist pro Minute: noch durch 60 teilen.",
                bd(rate) because "Das sind ml pro Stunde – noch in Tropfen pro Minute umrechnen.",
                near(bd(rate / 3 * 2)),
                if (rate % 60 == 0L) bd(rate / 60) because "Das sind ml pro Minute – noch mal 20 nehmen (20 Tropfen pro ml)." else near(bd(perMin + 10)),
            ),
        )
    }

    private fun microgram(r: Random): ApTask {
        repeat(100) {
            val u = r.pick(listOf(5L, 10L, 20L, 25L, 50L))
            val h = r.pick(listOf(2L, 4L, 5L, 6L, 8L, 10L, 12L))
            val total = u * 60 * h
            if (total % 100 != 0L) return@repeat
            val mg = BigDecimal.valueOf(total, 3)
            return ApTask(
                category = ApCategory.UNITS,
                key = "ug:$u,$h",
                text = compose(listOf("Über eine Spritzenpumpe erhält ein Patient ${n(u)} µg eines Medikaments pro Minute."), "Wie viele mg erhält er in ${n(h)} Stunden?", 3, r),
                unit = "mg",
                value = mg,
                steps = listOf(
                    step("Minuten: ${n(h)} $X 60", h * 60),
                    step("µg insgesamt: ${n(u)} $X ${n(h * 60)}", total),
                    valueStep("In mg: ${n(total)} $DIV 1.000", mg, "1 mg = 1.000 µg."),
                ),
                wrong = listOf(
                    bd(total) because "Das sind µg – noch durch 1.000 teilen (1 mg = 1.000 µg).",
                    mg * BigDecimal.TEN because "Beim Umrechnen ist das Komma verrutscht: 1 mg = 1.000 µg.",
                    mg.divide(BigDecimal.TEN) because "Beim Umrechnen ist das Komma verrutscht: 1 mg = 1.000 µg.",
                    mg.divide(BigDecimal(60), MathContext.DECIMAL64) because "Mit ${n(h)} Stunden statt ${n(h * 60)} Minuten gerechnet – die Dosis gilt pro Minute.",
                ),
            )
        }
        return drops(r)
    }

    // --- 6. Weg, Zeit, Tempo ------------------------------------------------------------------

    private fun speed(d: Int, r: Random): ApTask = when (d) {
        1 -> if (r.nextBoolean()) speedFromMinutes(r) else steps(r)
        2 -> if (r.nextBoolean()) averageSpeed(r) else meeting(r)
        else -> when (r.nextInt(3)) {
            0 -> catchUp(r)
            1 -> averageSpeed(r)
            else -> metresPerSecond(r)
        }
    }

    private fun speedFromMinutes(r: Random): ApTask {
        repeat(100) {
            val min = r.pick(listOf(5L, 6L, 10L, 12L, 15L, 20L, 30L))
            val v = r.pick(listOf(30L, 36L, 40L, 45L, 48L, 60L, 72L, 80L, 90L, 96L, 100L, 120L))
            if ((v * min) % 60 != 0L) return@repeat
            val km = v * min / 60
            val f = 60 / min
            return ApTask(
                category = ApCategory.SPEED,
                key = "kmh:$min,$v",
                text = compose(listOf("Ein Rettungswagen legt ${n(km)} km in ${n(min)} Minuten zurück."), "Wie hoch ist seine Durchschnittsgeschwindigkeit in km/h?", 1, r),
                unit = "km/h",
                value = bd(v),
                steps = listOf(
                    step("Wie oft passen ${n(min)} Minuten in eine Stunde: 60 $DIV ${n(min)}", f),
                    step("km pro Stunde: ${n(km)} $X ${n(f)}", v),
                ),
                wrong = listOf(
                    bd(km * 60) because "Mit 60 malgenommen, aber nicht durch die ${n(min)} Minuten geteilt.",
                    bd(km * min) because "Weg mal Minuten gerechnet. Richtig: Wie oft passen ${n(min)} Minuten in eine Stunde? So oft den Weg nehmen.",
                    if (v % 2 == 0L) near(bd(v / 2)) else near(bd(v + 5)),
                    near(bd(v + 10)),
                ),
            )
        }
        error("unreachable")
    }

    private fun steps(r: Random): ApTask {
        val sps = r.pick(listOf(2L, 3L))
        val len = r.pick(listOf(BigDecimal("0.5"), BigDecimal("0.6"), BigDecimal("0.75"), BigDecimal("0.8")))
        val mins = r.betweenL(2, 10)
        val sec = mins * 60
        val mps = len * bd(sps)
        val dist = mps * bd(sec)
        return ApTask(
            category = ApCategory.SPEED,
            key = "steps:$sps,$len,$mins",
            text = compose(listOf("Eine Läuferin macht ${n(sps)} Schritte pro Sekunde. Jeder Schritt ist ${dec(len)} m lang."), "Wie viele Meter legt sie in ${n(mins)} Minuten zurück?", 1, r),
            unit = "m",
            value = dist.stripTrailingZeros(),
            steps = listOf(
                step("Sekunden: ${n(mins)} $X 60", sec, "Erst die Zeit in Sekunden umrechnen."),
                valueStep("Meter pro Sekunde: ${n(sps)} $X ${dec(len)}", mps),
                valueStep("Meter: ${dec(mps)} $X ${n(sec)}", dist),
            ),
            wrong = listOf(
                mps * bd(mins) because "Mit ${n(mins)} Minuten statt ${n(sec)} Sekunden gerechnet – das Tempo gilt pro Sekunde.",
                near(dist * BigDecimal(2)),
                len * bd(sec) because "Die ${n(sps)} Schritte pro Sekunde vergessen.",
                near(dist.divide(BigDecimal(2))),
            ),
        )
    }

    private val averagePairs = listOf(
        Triple(60L, 30L, 60L), Triple(60L, 40L, 120L), Triple(90L, 60L, 180L), Triple(120L, 80L, 240L),
        Triple(30L, 20L, 60L), Triple(45L, 90L, 90L), Triple(50L, 75L, 150L), Triple(40L, 60L, 120L),
    )

    private fun averageSpeed(r: Random): ApTask {
        val (v1, v2, base) = r.pick(averagePairs)
        val dist = base * r.betweenL(1, 2)
        val t1 = dist / v1
        val t2 = dist / v2
        val total = t1 + t2
        val avg = 2 * dist / total
        return ApTask(
            category = ApCategory.SPEED,
            key = "avg:$v1,$v2,$dist",
            text = compose(
                listOf("Ein Lieferwagen fährt ${n(dist)} km mit durchschnittlich ${n(v1)} km/h zu einer Klinik und dieselbe Strecke mit ${n(v2)} km/h zurück."),
                "Wie hoch ist die Durchschnittsgeschwindigkeit für Hin- und Rückfahrt?",
                2,
                r,
            ),
            unit = "km/h",
            value = bd(avg),
            steps = listOf(
                step("Zeit hin: ${n(dist)} $DIV ${n(v1)}", t1),
                step("Zeit zurück: ${n(dist)} $DIV ${n(v2)}", t2),
                step("Zeit gesamt: ${n(t1)} + ${n(t2)}", total),
                step("Durchschnitt: ${n(2 * dist)} $DIV ${n(total)}", avg, "Gesamtstrecke durch Gesamtzeit – nicht den Mittelwert der Geschwindigkeiten nehmen!"),
            ),
            wrong = listOf(
                BigDecimal.valueOf(v1 + v2).divide(BigDecimal(2)) because "Den Mittelwert der Geschwindigkeiten genommen. Die langsame Fahrt dauert aber länger und zählt deshalb stärker.",
                bd(v1 + v2) because "Die Geschwindigkeiten addiert.",
                if (dist % total == 0L) bd(dist / total) because "Nur eine Strecke durch die Gesamtzeit geteilt – hin und zurück sind ${n(2 * dist)} km." else near(bd(avg + 5)),
            ),
        )
    }

    private fun meeting(r: Random): ApTask {
        repeat(200) {
            val v1 = r.pick(listOf(40L, 50L, 60L, 70L, 80L, 90L))
            val v2 = r.pick(listOf(30L, 40L, 50L, 60L, 70L))
            val s = v1 + v2
            val minutes = r.pick(listOf(20L, 30L, 40L, 45L, 60L, 90L, 120L))
            if ((s * minutes) % 60 != 0L) return@repeat
            val dist = s * minutes / 60
            val wrong = mutableListOf(
                near(bd(minutes * 2)),
                near(bd(minutes / 2)),
                if (dist % s == 0L) bd(dist / s) because "Das Ergebnis in Stunden – gefragt sind Minuten." else near(bd(minutes + 15)),
            )
            if (v1 != v2 && (dist * 60) % kotlin.math.abs(v1 - v2) == 0L) {
                wrong += bd(dist * 60 / kotlin.math.abs(v1 - v2)) because "Die Geschwindigkeiten abgezogen. Beim Aufeinanderzufahren addieren sie sich."
            }
            return ApTask(
                category = ApCategory.SPEED,
                key = "meet:$v1,$v2,$minutes",
                text = compose(
                    listOf("Zwei Krankenwagen starten gleichzeitig in zwei ${n(dist)} km voneinander entfernten Orten und fahren auf derselben Straße aufeinander zu – einer mit ${n(v1)} km/h, der andere mit ${n(v2)} km/h."),
                    "Nach wie vielen Minuten begegnen sie sich?",
                    2,
                    r,
                ),
                unit = "Minuten",
                value = bd(minutes),
                steps = listOf(
                    step("Annäherung pro Stunde: ${n(v1)} + ${n(v2)}", s, "Beim Aufeinanderzufahren addieren sich die Geschwindigkeiten."),
                    step("Minuten: ${n(dist)} $X 60 $DIV ${n(s)}", minutes),
                ),
                wrong = wrong,
            )
        }
        return averageSpeed(r)
    }

    private fun catchUp(r: Random): ApTask {
        repeat(300) {
            val v1 = r.pick(listOf(12L, 15L, 18L, 20L, 24L))
            val late = r.pick(listOf(20L, 30L, 40L, 60L))
            val v2 = v1 + r.pick(listOf(6L, 10L, 12L, 15L, 20L, 30L))
            if ((v1 * late) % 60 != 0L) return@repeat
            val head = v1 * late / 60
            val closing = v2 - v1
            if ((head * 60) % closing != 0L) return@repeat
            val minutes = head * 60 / closing
            if (minutes !in 5..240) return@repeat
            return ApTask(
                category = ApCategory.SPEED,
                key = "catch:$v1,$late,$v2",
                text = compose(
                    listOf(
                        "Ein Fahrradkurier fährt mit ${n(v1)} km/h los.",
                        "${n(late)} Minuten später folgt ihm ein Motorroller auf derselben Strecke mit ${n(v2)} km/h.",
                    ),
                    "Nach wie vielen Minuten (ab dem Start des Rollers) holt der Roller den Kurier ein?",
                    3,
                    r,
                ),
                unit = "Minuten",
                value = bd(minutes),
                steps = listOf(
                    step("Vorsprung in km: ${n(v1)} $X ${n(late)} $DIV 60", head),
                    step("Aufholen pro Stunde: ${n(v2)} $MINUS ${n(v1)}", closing, "Beim Hinterherfahren zählt der Unterschied der Geschwindigkeiten."),
                    step("Minuten: ${n(head)} $X 60 $DIV ${n(closing)}", minutes),
                ),
                wrong = listOf(
                    bd(minutes + late) because "Die ${n(late)} Minuten Vorsprung mitgezählt – gefragt ist die Zeit ab dem Start des Rollers.",
                    if ((head * 60) % v2 == 0L) {
                        bd(head * 60 / v2) because "Den Vorsprung durch die Geschwindigkeit des Rollers geteilt. Aufgeholt wird aber nur mit dem Unterschied: ${n(v2)} $MINUS ${n(v1)} km/h."
                    } else {
                        near(bd(minutes * 2))
                    },
                    bd(late) because "${n(late)} Minuten ist der Vorsprung des Kuriers, nicht die Zeit bis zum Einholen.",
                ),
            )
        }
        return meeting(r)
    }

    private fun metresPerSecond(r: Random): ApTask {
        val ms = r.pick(listOf(5L, 10L, 15L, 20L, 25L, 30L))
        val kmh = ms * 36 / 10
        return ApTask(
            category = ApCategory.SPEED,
            key = "ms:$ms",
            text = compose(listOf("Ein Gepard sprintet mit ${n(ms)} m/s."), "Wie viele km/h sind das?", 3, r),
            unit = "km/h",
            value = bd(kmh),
            steps = listOf(
                step("Meter pro Stunde: ${n(ms)} $X 3.600", ms * 3_600, "Eine Stunde hat 3.600 Sekunden."),
                step("km pro Stunde: ${n(ms * 3_600)} $DIV 1.000", kmh, "Kurz: m/s mal 3,6 ergibt km/h."),
            ),
            wrong = listOf(
                bd(ms * 60) because "Mit 60 statt mit 3,6 malgenommen (eine Stunde hat 3.600 Sekunden, ein km 1.000 m).",
                bd(ms * 36) because "Mit 36 statt mit 3,6 malgenommen – das Komma vergessen.",
                BigDecimal.valueOf(ms * 10).divide(BigDecimal(36), MathContext.DECIMAL64).setScale(1, RoundingMode.HALF_UP) because "Durch 3,6 geteilt statt malgenommen.",
                near(bd(kmh + 10)),
            ),
        )
    }

    // --- 7. Gemeinsam arbeiten ----------------------------------------------------------------

    private fun work(d: Int, r: Random): ApTask = when (d) {
        1 -> together(d, r)
        2 -> if (r.nextBoolean()) together(d, r) else fillAndDrain(r)
        else -> if (r.nextBoolean()) joinLater(r) else fillAndDrain(r)
    }

    private fun together(d: Int, r: Random): ApTask {
        val friendly = listOf(2L, 3L, 4L, 5L, 6L, 8L, 9L, 10L, 12L, 15L, 18L, 20L, 24L, 30L)
        repeat(300) {
            val a = r.pick(friendly.filter { it <= 12 })
            val b = r.pick(friendly)
            if (a == b) return@repeat
            val l = lcm(a, b)
            if (l > 60) return@repeat
            val sum = l / a + l / b
            val hoursExact = l % sum == 0L
            if (d == 1 && !hoursExact) return@repeat
            if ((l * 60) % sum != 0L) return@repeat
            val minutes = l * 60 / sum
            val pumps = r.nextBoolean()
            val facts = if (pumps) {
                listOf("Pumpe A füllt einen Wassertank allein in ${n(a)} Stunden, Pumpe B allein in ${n(b)} Stunden.")
            } else {
                listOf("Eine Pflegekraft braucht allein ${n(a)} Stunden, um alle Wochenpläne zu erstellen. Eine zweite Pflegekraft bräuchte allein ${n(b)} Stunden.")
            }
            val question = if (hoursExact) {
                if (pumps) "Wie viele Stunden brauchen beide Pumpen zusammen?" else "Wie viele Stunden brauchen beide zusammen?"
            } else {
                if (pumps) "Wie viele Minuten brauchen beide Pumpen zusammen?" else "Wie viele Minuten brauchen beide zusammen?"
            }
            val answer = if (hoursExact) l / sum else minutes
            val steps = mutableListOf(
                step("Die Arbeit in Teile teilen: kgV von ${n(a)} und ${n(b)}", l, "Denke dir die Arbeit als ${n(l)} gleiche Teile – dann ist alles ganzzahlig."),
                step("Erste pro Stunde: ${n(l)} $DIV ${n(a)}", l / a),
                step("Zweite pro Stunde: ${n(l)} $DIV ${n(b)}", l / b),
                step("Zusammen pro Stunde: ${n(l / a)} + ${n(l / b)}", sum, "Leistungen addieren – nicht die Zeiten!"),
            )
            steps += if (hoursExact) step("Stunden: ${n(l)} $DIV ${n(sum)}", answer) else step("Minuten: ${n(l)} $X 60 $DIV ${n(sum)}", answer)
            val unitFactor = if (hoursExact) 1L else 60L
            val wrong = mutableListOf(
                bd((a + b) * unitFactor) because "Die Zeiten addiert. Zusammen geht es aber schneller als allein.",
                bd(kotlin.math.abs(a - b) * unitFactor) because "Die Zeiten voneinander abgezogen. Richtig: die Leistungen pro Stunde addieren.",
            )
            if (((a + b) * unitFactor) % 2 == 0L) {
                wrong += bd((a + b) * unitFactor / 2) because "Den Mittelwert der Zeiten genommen. Zusammen geht es schneller als jeder allein."
            }
            wrong += bd(minOf(a, b) * unitFactor) because "Das ist die Zeit ${if (pumps) "der schnelleren Pumpe" else "der Schnelleren"} allein – zusammen geht es schneller."
            return ApTask(
                category = ApCategory.WORK,
                key = "tog:$a,$b,$pumps",
                text = compose(facts, question, d, r),
                unit = if (hoursExact) "Stunden" else "Minuten",
                value = bd(answer),
                steps = steps,
                wrong = wrong,
            )
        }
        error("unreachable")
    }

    private fun fillAndDrain(r: Random): ApTask {
        repeat(300) {
            val a = r.betweenL(2, 10)
            val b = r.betweenL(a + 1, 20)
            val l = lcm(a, b)
            val net = l / a - l / b
            if (net <= 0 || l % net != 0L) return@repeat
            val hours = l / net
            if (hours > 30) return@repeat
            val wrong = mutableListOf(
                bd(b - a) because "Die Zeiten voneinander abgezogen. Richtig: die Leistungen pro Stunde abziehen.",
                bd(a + b) because "Die Zeiten addiert.",
            )
            val sum = l / a + l / b
            if (l % sum == 0L) wrong += bd(l / sum) because "Zulauf und Abfluss addiert, als würden beide füllen. Der Abfluss arbeitet aber dagegen."
            return ApTask(
                category = ApCategory.WORK,
                key = "fd:$a,$b",
                text = compose(
                    listOf(
                        "Ein Zulauf füllt ein leeres Bewegungsbad in ${n(a)} Stunden.",
                        "Der Abfluss würde das volle Becken in ${n(b)} Stunden leeren. Versehentlich sind beide gleichzeitig offen.",
                    ),
                    "Nach wie vielen Stunden ist das leere Becken trotzdem voll?",
                    3,
                    r,
                ),
                unit = "Stunden",
                value = bd(hours),
                steps = listOf(
                    step("Becken in Teile teilen: kgV von ${n(a)} und ${n(b)}", l),
                    step("Zulauf pro Stunde: ${n(l)} $DIV ${n(a)}", l / a),
                    step("Abfluss pro Stunde: ${n(l)} $DIV ${n(b)}", l / b),
                    step("Netto pro Stunde: ${n(l / a)} $MINUS ${n(l / b)}", net, "Der Abfluss arbeitet gegen den Zulauf – abziehen."),
                    step("Stunden: ${n(l)} $DIV ${n(net)}", hours),
                ),
                wrong = wrong,
            )
        }
        return together(2, r)
    }

    private fun joinLater(r: Random): ApTask {
        repeat(300) {
            val h = r.betweenL(3, 10)
            val t = r.betweenL(4, 15)
            val t1 = r.betweenL(1, t - 2)
            val e = r.betweenL(1, 6)
            val double = r.nextBoolean()
            val capacity = h + if (double) 2 * e else e
            val total = h * t
            val done = h * t1
            val rest = total - done
            if (rest % capacity != 0L) return@repeat
            val restTime = rest / capacity
            val sum = t1 + restTime
            val joiners = if (double) {
                "${n(e)} Ärztinnen dazu, die jeweils doppelt so schnell arbeiten wie eine Hilfskraft"
            } else {
                "${n(e)} weitere Hilfskräfte mit gleichem Tempo dazu"
            }
            return ApTask(
                category = ApCategory.WORK,
                key = "join:$h,$t,$t1,$e,$double",
                text = compose(
                    listOf(
                        "${n(h)} Hilfskräfte würden gemeinsam ${n(t)} Stunden brauchen, um die Daten einer Studie auszuwerten.",
                        "Nach ${n(t1)} Stunden kommen $joiners.",
                    ),
                    "Wie viele Stunden dauert die Auswertung insgesamt?",
                    3,
                    r,
                ),
                unit = "Stunden",
                value = bd(sum),
                steps = listOf(
                    step("Gesamtaufwand in Hilfskraft-Stunden: ${n(h)} $X ${n(t)}", total),
                    step("Schon erledigt: ${n(h)} $X ${n(t1)}", done),
                    step("Rest: ${n(total)} $MINUS ${n(done)}", rest),
                    step(if (double) "Neue Leistung pro Stunde: ${n(h)} + 2 $X ${n(e)}" else "Neue Leistung pro Stunde: ${n(h)} + ${n(e)}", capacity, if (double) "Doppelt so schnell zählt wie zwei Hilfskräfte." else null),
                    step("Restzeit: ${n(rest)} $DIV ${n(capacity)}", restTime),
                    step("Insgesamt: ${n(t1)} + ${n(restTime)}", sum, "Die ersten ${n(t1)} Stunden nicht vergessen."),
                ),
                wrong = listOf(
                    bd(restTime) because "Das ist nur die Restzeit – die ersten ${n(t1)} Stunden kommen noch dazu.",
                    bd(t) because "Das ist die geplante Zeit ohne Verstärkung.",
                    near(bd(sum + 1)),
                    if (total % capacity == 0L) {
                        bd(total / capacity) because "So gerechnet, als hätten alle von Anfang an mitgearbeitet. Die Verstärkung kam aber erst nach ${n(t1)} Stunden."
                    } else {
                        near(bd(sum - 1))
                    },
                ),
            )
        }
        return fillAndDrain(r)
    }

    // --- 8. Mischen und Verdünnen -------------------------------------------------------------

    private fun mix(d: Int, r: Random): ApTask = when (d) {
        1 -> dilute(r)
        2 -> if (r.nextBoolean()) addWater(r) else combine(r)
        else -> if (r.nextBoolean()) mixCross(r) else addWater(r)
    }

    private fun dilute(r: Random): ApTask {
        repeat(100) {
            val c = r.pick(listOf(10L, 12L, 15L, 20L, 24L, 30L, 40L, 50L, 60L, 80L))
            val f = r.pick(listOf(2L, 3L, 4L, 5L, 6L, 8L, 10L))
            if (c % f != 0L) return@repeat
            val v = r.pick(listOf(50L, 100L, 150L, 200L, 250L, 300L, 400L, 500L))
            val total = v * f
            val active = BigDecimal.valueOf(v * c).divide(BigDecimal(100))
            val newC = c / f
            val wrong = mutableListOf(
                bd(c - f) because "Den Faktor ${n(f)} abgezogen statt durch ${n(f)} geteilt – die Lösung ist auf das ${n(f)}-Fache verdünnt.",
                if (c * f < 100) bd(c * f) because "Mit ${n(f)} malgenommen statt geteilt – beim Verdünnen sinkt die Konzentration." else near(bd(newC + 1)),
            )
            if (f > 1 && c % (f - 1) == 0L) wrong += bd(c / (f - 1)) because "Durch die Wassermenge geteilt statt durch die ganze neue Menge (${n(total)} ml)."
            return ApTask(
                category = ApCategory.MIX,
                key = "dil:$c,$f,$v",
                text = compose(listOf("${n(v)} ml einer ${n(c)}-prozentigen Glukoselösung werden mit Wasser auf ${n(total)} ml aufgefüllt."), "Welche Konzentration hat die neue Lösung?", 1, r),
                unit = "%",
                value = bd(newC),
                steps = listOf(
                    valueStep("Glukose in ml: ${n(v)} $X ${n(c)} $DIV 100", active, "Die Menge Glukose ändert sich beim Verdünnen nicht."),
                    step("Konzentration: ${dec(active)} $X 100 $DIV ${n(total)}", newC),
                ),
                wrong = wrong,
            )
        }
        error("unreachable")
    }

    private fun addWater(r: Random): ApTask {
        repeat(300) {
            val c = r.pick(listOf(10L, 12L, 15L, 20L, 24L, 30L, 40L))
            val c2 = r.pick(listOf(2L, 3L, 4L, 5L, 6L, 8L, 10L, 12L, 15L, 20L))
            if (c2 >= c) return@repeat
            val v = r.pick(listOf(100L, 150L, 200L, 250L, 300L, 400L, 500L, 600L))
            if ((v * c) % c2 != 0L) return@repeat
            val total = v * c / c2
            val water = total - v
            if (total > 6_000) return@repeat
            val active = BigDecimal.valueOf(v * c).divide(BigDecimal(100))
            return ApTask(
                category = ApCategory.MIX,
                key = "wat:$c,$c2,$v",
                text = compose(
                    listOf("Eine Apotheke hat ${n(v)} ml einer ${n(c)}-prozentigen Lösung."),
                    "Wie viele ml Wasser muss sie dazugeben, damit eine ${n(c2)}-prozentige Lösung entsteht?",
                    2,
                    r,
                ),
                unit = "ml",
                value = bd(water),
                steps = listOf(
                    valueStep("Wirkstoff in ml: ${n(v)} $X ${n(c)} $DIV 100", active),
                    step("Gesamtmenge danach: ${dec(active)} $X 100 $DIV ${n(c2)}", total, "Der Wirkstoff soll danach ${n(c2)} % der Lösung sein."),
                    step("Wasser: ${n(total)} $MINUS ${n(v)}", water, "Gefragt ist nur das zugegebene Wasser."),
                ),
                wrong = listOf(
                    bd(total) because "Das ist die ganze Menge danach – gefragt ist nur das Wasser: noch ${n(v)} ml abziehen.",
                    if ((v * c2) % c == 0L) bd(v * c2 / c) because "Das Verhältnis falsch herum genommen (${n(v)} $X ${n(c2)} $DIV ${n(c)})." else near(bd(water + v / 2)),
                    near(bd(water * 2)),
                ),
            )
        }
        return dilute(r)
    }

    private fun combine(r: Random): ApTask {
        repeat(300) {
            val v1 = r.pick(listOf(100L, 200L, 300L, 400L, 500L))
            val v2 = r.pick(listOf(100L, 200L, 300L, 400L, 500L))
            val c1 = r.pick(listOf(5L, 10L, 15L, 20L, 25L, 30L))
            val c2 = r.pick(listOf(10L, 20L, 30L, 40L, 50L))
            if (c1 == c2) return@repeat
            val w = v1 * c1 + v2 * c2
            val total = v1 + v2
            if (w % total != 0L) return@repeat
            val c = w / total
            val w1 = v1 * c1 / 100
            val w2 = v2 * c2 / 100
            val wrong = mutableListOf(
                bd(c1 + c2) because "Die Prozente addiert. Die Konzentration der Mischung liegt zwischen den beiden.",
                near(bd(c + 5)),
            )
            if ((c1 + c2) % 2 == 0L) wrong += bd((c1 + c2) / 2) because "Den Mittelwert der Prozente genommen – das stimmt nur bei gleichen Mengen."
            return ApTask(
                category = ApCategory.MIX,
                key = "comb:$v1,$c1,$v2,$c2",
                text = compose(listOf("Man mischt ${n(v1)} ml einer ${n(c1)}-prozentigen und ${n(v2)} ml einer ${n(c2)}-prozentigen Kochsalzlösung."), "Welche Konzentration hat die Mischung?", 2, r),
                unit = "%",
                value = bd(c),
                steps = listOf(
                    step("Salz in der ersten Lösung: ${n(v1)} $X ${n(c1)} $DIV 100", w1),
                    step("Salz in der zweiten Lösung: ${n(v2)} $X ${n(c2)} $DIV 100", w2),
                    step("Salz zusammen: ${n(w1)} + ${n(w2)}", w1 + w2),
                    step("Menge zusammen: ${n(v1)} + ${n(v2)}", total),
                    step("Konzentration: ${n(w1 + w2)} $X 100 $DIV ${n(total)}", c, "Nur bei gleichen Mengen wäre es der Mittelwert der beiden Prozente."),
                ),
                wrong = wrong,
            )
        }
        return dilute(r)
    }

    private fun mixCross(r: Random): ApTask {
        repeat(300) {
            val low = r.pick(listOf(5L, 10L, 15L, 20L))
            val high = r.pick(listOf(30L, 40L, 50L, 60L))
            val target = r.betweenL(low + 1, high - 1)
            val vLow = r.pick(listOf(100L, 200L, 300L, 400L, 600L))
            val a = target - low
            val b = high - target
            if ((vLow * a) % b != 0L) return@repeat
            val x = vLow * a / b
            if (x > 3_000) return@repeat
            val wrong = mutableListOf(
                bd(vLow) because "Gleich viel von beiden genommen – das ergibt genau die Mitte zwischen ${n(low)} % und ${n(high)} %.",
                near(bd(x * 2)),
            )
            if ((vLow * b) % a == 0L) wrong += bd(vLow * b / a) because "Das Verhältnis nicht über Kreuz genommen (${n(vLow)} $X ${n(b)} $DIV ${n(a)})."
            return ApTask(
                category = ApCategory.MIX,
                key = "cross:$low,$high,$target,$vLow",
                text = compose(
                    listOf("Zu ${n(vLow)} ml einer ${n(low)}-prozentigen Lösung soll eine ${n(high)}-prozentige Lösung gegeben werden."),
                    "Wie viele ml der starken Lösung braucht man, damit eine ${n(target)}-prozentige Mischung entsteht?",
                    3,
                    r,
                ),
                unit = "ml",
                value = bd(x),
                steps = listOf(
                    step("Abstand der schwachen Lösung zum Ziel: ${n(target)} $MINUS ${n(low)}", a, "Mischungskreuz: Die Abstände zum Ziel bestimmen das Mischungsverhältnis – über Kreuz."),
                    step("Abstand der starken Lösung zum Ziel: ${n(high)} $MINUS ${n(target)}", b),
                    step("Starke Lösung: ${n(vLow)} $X ${n(a)} $DIV ${n(b)}", x, "Verhältnis stark : schwach = ${n(a)} : ${n(b)}."),
                ),
                wrong = wrong,
            )
        }
        return combine(r)
    }

    // --- 9. Wachstum und Halbwertszeit --------------------------------------------------------

    private fun powLabel(k: Long): String = List(k.toInt()) { "2" }.joinToString(" $X ")

    private fun growth(d: Int, r: Random): ApTask = when (d) {
        1 -> doubling(d, r)
        2 -> if (r.nextBoolean()) halfLife(r, backwards = false) else doubling(d, r)
        else -> when (r.nextInt(3)) {
            0 -> halfLife(r, backwards = true)
            1 -> halfLifeTime(r)
            else -> halfLife(r, backwards = false)
        }
    }

    private fun doubling(d: Int, r: Random): ApTask {
        val n0 = r.pick(listOf(50L, 100L, 200L, 250L, 300L, 500L, 1_000L, 1_500L))
        val period = r.pick(listOf(2L, 3L, 4L, 5L, 6L, 20L, 30L))
        val k = r.betweenL(2, if (d == 1) 4 else 6)
        val t = period * k
        val minutes = period >= 20
        val factor = Num.pow2(k.toInt())
        val answer = n0 * factor
        return ApTask(
            category = ApCategory.GROWTH,
            key = "dbl:$n0,$period,$k",
            text = compose(
                listOf("In einer Bakterienkultur befinden sich ${n(n0)} Bakterien. Ihre Zahl verdoppelt sich alle ${n(period)} ${if (minutes) "Minuten" else "Stunden"}."),
                "Wie viele Bakterien sind es nach ${n(t)} ${if (minutes) "Minuten" else "Stunden"}?",
                d,
                r,
            ),
            unit = null,
            value = bd(answer),
            steps = listOf(
                step("Verdopplungen: ${n(t)} $DIV ${n(period)}", k, "Erst zählen, wie oft sich die Zahl verdoppelt."),
                step("Faktor: ${powLabel(k)}", factor),
                step("Bakterien: ${n(n0)} $X ${n(factor)}", answer),
            ),
            wrong = listOf(
                bd(n0 * 2 * k) because "Mit 2 $X ${n(k)} = ${n(2 * k)} statt mit ${powLabel(k)} = ${n(factor)} malgenommen.",
                bd(n0 * factor / 2) because "Eine Verdopplung zu wenig gezählt.",
                bd(n0 * factor * 2) because "Eine Verdopplung zu viel gezählt.",
                bd(n0 + n0 * k) because "Pro Zeitabschnitt nur den Startwert dazugezählt – beim Verdoppeln wächst aber jedes Mal der neue Bestand.",
            ),
        )
    }

    private fun halfLife(r: Random, backwards: Boolean): ApTask {
        val t12 = r.pick(listOf(2L, 3L, 4L, 5L, 6L, 8L, 12L))
        val k = r.betweenL(2, 5)
        val res = r.pick(listOf(5L, 10L, 15L, 20L, 25L, 30L, 40L, 50L, 75L, 100L))
        val factor = Num.pow2(k.toInt())
        val m0 = res * factor
        val t = t12 * k
        return if (!backwards) {
            ApTask(
                category = ApCategory.GROWTH,
                key = "hl:$t12,$k,$res",
                text = compose(
                    listOf("Ein Medikament hat im Blut eine Halbwertszeit von ${n(t12)} Stunden. Zu Beginn sind ${n(m0)} mg im Blut."),
                    "Wie viel mg sind nach ${n(t)} Stunden noch vorhanden?",
                    2,
                    r,
                ),
                unit = "mg",
                value = bd(res),
                steps = listOf(
                    step("Halbwertszeiten: ${n(t)} $DIV ${n(t12)}", k),
                    step("Faktor: ${powLabel(k)}", factor, "${n(k)}-mal halbieren heißt durch ${n(factor)} teilen."),
                    step("Rest: ${n(m0)} $DIV ${n(factor)}", res),
                ),
                wrong = listOf(
                    if (m0 % (2 * k) == 0L) bd(m0 / (2 * k)) because "Durch 2 $X ${n(k)} = ${n(2 * k)} geteilt statt durch ${powLabel(k)} = ${n(factor)}." else near(bd(res + 5)),
                    bd(res * 2) because "Einmal zu wenig halbiert – es sind ${n(k)} Halbwertszeiten.",
                    if (res % 2 == 0L) bd(res / 2) because "Einmal zu viel halbiert – es sind ${n(k)} Halbwertszeiten." else near(bd(res + 10)),
                    bd(m0 - m0 / 2) because "Nur einmal halbiert – in ${n(t)} Stunden sind es ${n(k)} Halbwertszeiten.",
                ),
            )
        } else {
            ApTask(
                category = ApCategory.GROWTH,
                key = "hlb:$t12,$k,$res",
                text = compose(
                    listOf("Bei einer Patientin sind ${n(t)} Stunden nach der Einnahme noch ${n(res)} mg eines Wirkstoffs im Blut. Die Halbwertszeit beträgt ${n(t12)} Stunden."),
                    "Wie viel mg waren zu Beginn im Blut?",
                    3,
                    r,
                ),
                unit = "mg",
                value = bd(m0),
                steps = listOf(
                    step("Halbwertszeiten: ${n(t)} $DIV ${n(t12)}", k),
                    step("Faktor: ${powLabel(k)}", factor),
                    step("Zu Beginn: ${n(res)} $X ${n(factor)}", m0, "Rückwärts: Jede Halbwertszeit verdoppelt den früheren Wert."),
                ),
                wrong = listOf(
                    bd(res * 2 * k) because "Mit 2 $X ${n(k)} = ${n(2 * k)} statt mit ${powLabel(k)} = ${n(factor)} malgenommen.",
                    bd(m0 / 2) because "Einmal zu wenig verdoppelt – es sind ${n(k)} Halbwertszeiten.",
                    bd(m0 * 2) because "Einmal zu viel verdoppelt – es sind ${n(k)} Halbwertszeiten.",
                    bd(res * t) because "Mit den ${n(t)} Stunden malgenommen, statt die Halbwertszeiten zu zählen.",
                ),
            )
        }
    }

    private fun halfLifeTime(r: Random): ApTask {
        val t12 = r.pick(listOf(2L, 3L, 5L, 6L, 8L, 10L))
        val k = r.betweenL(2, 5)
        val res = r.pick(listOf(10L, 20L, 25L, 40L, 50L))
        val factor = Num.pow2(k.toInt())
        val m0 = res * factor
        return ApTask(
            category = ApCategory.GROWTH,
            key = "hlt:$t12,$k,$res",
            text = compose(
                listOf("Ein radioaktiver Marker hat eine Halbwertszeit von ${n(t12)} Tagen. Eine Probe enthält ${n(m0)} mg davon."),
                "Nach wie vielen Tagen sind nur noch ${n(res)} mg vorhanden?",
                3,
                r,
            ),
            unit = "Tage",
            value = bd(k * t12),
            steps = listOf(
                step("Verhältnis: ${n(m0)} $DIV ${n(res)}", factor),
                step("So oft halbieren, bis ${n(factor)} erreicht ist", k, "${powLabel(k)} = ${n(factor)}."),
                step("Tage: ${n(k)} $X ${n(t12)}", k * t12),
            ),
            wrong = listOf(
                near(bd(factor * t12 / 2)),
                bd((k + 1) * t12) because "Eine Halbierung zu viel gezählt: Von ${n(m0)} mg bis ${n(res)} mg wird ${n(k)}-mal halbiert.",
                bd((k - 1) * t12) because "Eine Halbierung zu wenig gezählt: Von ${n(m0)} mg bis ${n(res)} mg wird ${n(k)}-mal halbiert.",
                bd(factor * t12) because "Mit dem Verhältnis ${n(factor)} malgenommen statt die Halbierungen zu zählen (${powLabel(k)} = ${n(factor)}, also ${n(k)} Halbierungen).",
            ),
        )
    }

    // --- 10. Gleichungen aus dem Text ---------------------------------------------------------

    private fun equations(d: Int, r: Random): ApTask = when (d) {
        1 -> sumDifference(r)
        2 -> if (r.nextBoolean()) ampoules(r) else sumDifference(r)
        else -> if (r.nextBoolean()) ages(r) else twoPrices(r)
    }

    private fun sumDifference(r: Random): ApTask {
        val b = r.betweenL(8, 60)
        val diff = r.betweenL(2, 30)
        val a = b + diff
        val sum = a + b
        return ApTask(
            category = ApCategory.EQUATIONS,
            key = "sd:$b,$diff",
            text = compose(listOf("Zwei Stationen haben zusammen ${n(sum)} Betten. Station A hat ${n(diff)} Betten mehr als Station B."), "Wie viele Betten hat Station A?", 1, r),
            unit = "Betten",
            value = bd(a),
            steps = listOf(
                step("Ohne den Unterschied: ${n(sum)} $MINUS ${n(diff)}", 2 * b, "Nimmt man A die ${n(diff)} Extra-Betten weg, sind beide gleich groß."),
                step("Station B: ${n(2 * b)} $DIV 2", b),
                step("Station A: ${n(b)} + ${n(diff)}", a),
            ),
            wrong = listOf(
                bd(b) because "Das ist Station B – gefragt ist Station A.",
                if (sum % 2 == 0L) bd(sum / 2) because "Einfach halbiert – den Unterschied von ${n(diff)} Betten nicht berücksichtigt." else near(bd(a + 1)),
                if (sum % 2 == 0L) {
                    bd(sum / 2 + diff) because "Erst halbiert und dann den ganzen Unterschied dazugezählt. Richtig: erst den Unterschied abziehen, dann halbieren."
                } else {
                    near(bd(a + diff))
                },
                bd(sum - diff) because "Den Unterschied abgezogen, aber nicht halbiert – das sind beide Stationen zusammen ohne die ${n(diff)} Extra-Betten.",
            ),
        )
    }

    private fun ampoules(r: Random): ApTask {
        val (small, large) = r.pick(listOf(2L to 5L, 5L to 10L, 2L to 10L, 1L to 5L, 5L to 20L))
        val total = r.betweenL(8, 30)
        val x = r.betweenL(2, total - 2)
        val volume = x * large + (total - x) * small
        val base = total * small
        val excess = volume - base
        return ApTask(
            category = ApCategory.EQUATIONS,
            key = "amp:$small,$large,$total,$x",
            text = compose(
                listOf("In einem Medikamentenschrank liegen ${n(total)} Ampullen. Manche enthalten ${n(small)} ml, die übrigen ${n(large)} ml. Zusammen sind es ${n(volume)} ml."),
                "Wie viele ${n(large)}-ml-Ampullen sind im Schrank?",
                2,
                r,
            ),
            unit = "Ampullen",
            value = bd(x),
            steps = listOf(
                step("Wenn alle ${n(small)} ml hätten: ${n(total)} $X ${n(small)}", base, "Erst annehmen, alle wären die kleinen."),
                step("Fehlende Menge: ${n(volume)} $MINUS ${n(base)}", excess),
                step("Jede große bringt ${n(large - small)} ml mehr: ${n(excess)} $DIV ${n(large - small)}", x),
            ),
            wrong = listOf(
                bd(total - x) because "Das sind die ${n(small)}-ml-Ampullen – gefragt sind die ${n(large)}-ml-Ampullen.",
                if (volume % large == 0L) bd(volume / large) because "So gerechnet, als wären alle Ampullen ${n(large)}-ml-Ampullen." else near(bd(x + 2)),
                if (excess % large == 0L && excess / large > 0) {
                    bd(excess / large) because "Die fehlende Menge durch ${n(large)} geteilt statt durch den Unterschied ${n(large - small)} ml."
                } else {
                    near(bd(x + 1))
                },
            ),
        )
    }

    private fun ages(r: Random): ApTask {
        repeat(300) {
            val a = r.betweenL(3, 6)
            val b = r.betweenL(2, a - 1)
            val k = r.betweenL(2, 20)
            val num = (b - 1) * k
            if (num % (a - b) != 0L) return@repeat
            val x = num / (a - b)
            if (x !in 2..16 || a * x > 60 || a * x < 20) return@repeat
            return ApTask(
                category = ApCategory.EQUATIONS,
                key = "age:$a,$b,$k",
                text = compose(
                    listOf(
                        "Eine Ärztin ist heute ${if (a == 3L) "dreimal" else if (a == 4L) "viermal" else "${n(a)}-mal"} so alt wie ihr Sohn.",
                        "In ${n(k)} Jahren ist sie nur noch ${if (b == 2L) "doppelt" else if (b == 3L) "dreimal" else "${n(b)}-mal"} so alt wie er.",
                    ),
                    "Wie alt ist der Sohn heute?",
                    3,
                    r,
                ),
                unit = "Jahre",
                value = bd(x),
                steps = listOf(
                    step("Gleichung ${n(a)}x + ${n(k)} = ${n(b)}(x + ${n(k)}), Zahl ohne x: ${n(b)} $X ${n(k)} $MINUS ${n(k)}", num, "Sohn heute = x. Beide Seiten ausrechnen und die Zahlen auf eine Seite bringen."),
                    step("Faktor vor x: ${n(a)} $MINUS ${n(b)}", a - b),
                    step("Sohn: ${n(num)} $DIV ${n(a - b)}", x),
                ),
                wrong = listOf(
                    bd(a * x) because "Das ist das Alter der Ärztin – gefragt ist der Sohn.",
                    bd(x + k) because "Das ist das Alter des Sohnes in ${n(k)} Jahren.",
                    bd(num) because "Das ist erst die Zahl ohne x – noch durch ${n(a - b)} teilen.",
                    near(bd(b * x)),
                ),
            )
        }
        return twoPrices(r)
    }

    private fun twoPrices(r: Random): ApTask {
        val coffee = r.betweenL(2, 5)
        val bun = r.betweenL(1, 4)
        val a1 = r.betweenL(3, 5)
        val a2 = r.betweenL(1, a1 - 1)
        val b1 = r.betweenL(1, 4)
        val p1 = a1 * coffee + b1 * bun
        val p2 = a2 * coffee + b1 * bun
        val diff = p1 - p2
        return ApTask(
            category = ApCategory.EQUATIONS,
            key = "pr:$coffee,$bun,$a1,$a2,$b1",
            text = compose(
                listOf(
                    "In der Klinik-Cafeteria kosten ${n(a1)} Kaffee und ${n(b1)} Brötchen zusammen ${n(p1)} €.",
                    "${n(a2)} Kaffee und ${n(b1)} Brötchen kosten ${n(p2)} €.",
                ),
                "Wie viel kostet ein Kaffee?",
                3,
                r,
            ),
            unit = "€",
            value = bd(coffee),
            steps = listOf(
                step("Preisunterschied: ${n(p1)} $MINUS ${n(p2)}", diff, "Die Brötchen sind in beiden Bestellungen gleich – der Unterschied kommt nur vom Kaffee."),
                step("Kaffee-Unterschied: ${n(a1)} $MINUS ${n(a2)}", a1 - a2),
                step("Ein Kaffee: ${n(diff)} $DIV ${n(a1 - a2)}", coffee),
            ),
            wrong = listOf(
                bd(bun) because "Das ist der Preis eines Brötchens.",
                bd(diff) because "Das ist der Preis für ${n(a1 - a2)} Kaffee – noch durch ${n(a1 - a2)} teilen.",
                near(bd(coffee + 1)),
                if (p2 % a2 == 0L && p2 / a2 != coffee) {
                    bd(p2 / a2) because "Die Brötchen in der zweiten Bestellung übersehen (${n(p2)} $DIV ${n(a2)})."
                } else {
                    near(bd(coffee + 2))
                },
            ),
        )
    }

    // --- 11. Zählen und Anordnen --------------------------------------------------------------

    private fun factorial(n: Long): Long = (1..n).fold(1L) { a, b -> a * b }

    private fun combi(d: Int, r: Random): ApTask = when (d) {
        1 -> if (r.nextBoolean()) arrangements(r) else productRule(r)
        2 -> if (r.nextBoolean()) orderedChoice(r) else handshakes(r)
        else -> if (r.nextBoolean()) teams(r) else handshakes(r)
    }

    private fun arrangements(r: Random): ApTask {
        val n = r.betweenL(3, 6)
        val result = factorial(n)
        return ApTask(
            category = ApCategory.COMBI,
            key = "perm:$n",
            text = compose(listOf("Eine Ärztin muss bei ihrer Visite ${n(n)} Patienten besuchen. Die Reihenfolge kann sie frei wählen."), "Wie viele verschiedene Reihenfolgen gibt es?", 1, r),
            unit = null,
            value = bd(result),
            steps = listOf(step("Für den ersten Besuch ${n(n)} Möglichkeiten, dann eine weniger: " + (n downTo 1).joinToString(" $X ") { n(it) }, result)),
            wrong = listOf(
                bd(n * n) because "${n(n)} $X ${n(n)} – nach jedem Besuch bleibt aber eine Möglichkeit weniger.",
                bd(n * (n - 1)) because "Nur die ersten beiden Besuche gezählt – es geht weiter bis 1: ${(n downTo 1).joinToString(" $X ") { n(it) }}.",
                bd(factorial(n - 1)) because "Einen Faktor vergessen: Für den ersten Besuch gibt es ${n(n)} Möglichkeiten.",
                near(bd(2 * n)),
            ),
        )
    }

    private fun productRule(r: Random): ApTask {
        val a = r.betweenL(2, 5)
        val b = r.betweenL(3, 6)
        val c = r.betweenL(2, 4)
        val result = a * b * c
        return ApTask(
            category = ApCategory.COMBI,
            key = "prod:$a,$b,$c",
            text = compose(
                listOf("Das Mittagsmenü der Klinik besteht aus Vorspeise, Hauptgericht und Nachtisch. Zur Wahl stehen ${n(a)} Vorspeisen, ${n(b)} Hauptgerichte und ${n(c)} Nachtische."),
                "Wie viele verschiedene Menüs kann man zusammenstellen?",
                1,
                r,
            ),
            unit = "Menüs",
            value = bd(result),
            steps = listOf(step("Jede Wahl mit jeder kombinieren: ${n(a)} $X ${n(b)} $X ${n(c)}", result, "Bei „und“ werden die Möglichkeiten multipliziert.")),
            wrong = listOf(
                bd(a + b + c) because "Addiert statt malgenommen – bei „und“ werden die Möglichkeiten multipliziert.",
                bd(a * b) because "Den Nachtisch vergessen.",
                near(bd(result * 2)),
                bd(a * b + c) because "Den Nachtisch addiert statt malgenommen.",
            ),
        )
    }

    private fun orderedChoice(r: Random): ApTask {
        val n = r.betweenL(5, 10)
        val result = n * (n - 1) * (n - 2)
        return ApTask(
            category = ApCategory.COMBI,
            key = "ord:$n",
            text = compose(
                listOf("Aus ${n(n)} Mitgliedern einer Fachschaft werden eine Leitung, eine Stellvertretung und eine Kassenwartin gewählt. Niemand darf zwei Ämter haben."),
                "Wie viele verschiedene Besetzungen der drei Ämter gibt es?",
                2,
                r,
            ),
            unit = null,
            value = bd(result),
            steps = listOf(step("Ämter nacheinander besetzen: ${n(n)} $X ${n(n - 1)} $X ${n(n - 2)}", result, "Die Ämter sind verschieden – die Reihenfolge zählt.")),
            wrong = listOf(
                bd(result / 6) because "Durch 6 geteilt, als wären die Ämter gleich. Die Ämter sind aber verschieden – die Reihenfolge zählt.",
                bd(n * n * n) because "${n(n)} $X ${n(n)} $X ${n(n)} – aber niemand darf zwei Ämter haben.",
                bd(n * 3) because "Mit der Zahl der Ämter malgenommen. Richtig: die Ämter nacheinander besetzen.",
                bd(n * (n - 1)) because "Nur zwei Ämter besetzt – es sind drei.",
            ),
        )
    }

    private fun handshakes(r: Random): ApTask {
        val couples = r.nextBoolean()
        val people = if (couples) 2 * r.betweenL(3, 7) + 2 else r.betweenL(5, 14)
        val result = people * (people - 1) / 2
        val facts = if (couples) {
            val p = (people - 2) / 2
            listOf("Zu einer Silvesterfeier kommen ${n(p)} Paare zu einem Gastgeberpaar. Um Mitternacht stößt jede Person genau einmal mit jeder anderen an.")
        } else {
            listOf("Bei einem Teamtreffen begrüßt jede der ${n(people)} Personen jede andere genau einmal mit Handschlag.")
        }
        val question = if (couples) "Wie oft klingen die Gläser?" else "Wie viele Handschläge gibt es?"
        val steps = mutableListOf<Step>()
        if (couples) steps += step("Personen: 2 $X ${n((people - 2) / 2)} + 2", people, "Das Gastgeberpaar nicht vergessen.")
        steps += step("Jede Person mit jeder anderen: ${n(people)} $X ${n(people - 1)}", people * (people - 1))
        steps += step("Jedes Paar doppelt gezählt: ${n(people * (people - 1))} $DIV 2", result)
        return ApTask(
            category = ApCategory.COMBI,
            key = "hs:$people,$couples",
            text = compose(facts, question, if (couples) 3 else 2, r),
            unit = null,
            value = bd(result),
            steps = steps,
            wrong = listOf(
                bd(people * (people - 1)) because "${if (couples) "Jedes Anstoßen" else "Jeder Handschlag"} doppelt gezählt – noch durch 2 teilen.",
                if (couples) bd((people - 2) * (people - 3) / 2) because "Das Gastgeberpaar vergessen." else near(bd((people - 2) * (people - 3) / 2)),
                if (people % 2 == 0L) {
                    bd(people * people / 2) because "${n(people)} $X ${n(people)} statt ${n(people)} $X ${n(people - 1)} – mit sich selbst zählt nicht."
                } else {
                    near(bd(result + people))
                },
                near(bd((people - 1) * (people - 1))),
            ),
        )
    }

    private fun teams(r: Random): ApTask {
        val n = r.betweenL(5, 10)
        val ordered = n * (n - 1) * (n - 2)
        val result = ordered / 6
        return ApTask(
            category = ApCategory.COMBI,
            key = "team:$n",
            text = compose(listOf("Für eine Studie sollen aus ${n(n)} Ärztinnen drei für ein Team ausgewählt werden. Alle drei haben die gleiche Aufgabe."), "Wie viele verschiedene Teams sind möglich?", 3, r),
            unit = "Teams",
            value = bd(result),
            steps = listOf(
                step("Mit Reihenfolge: ${n(n)} $X ${n(n - 1)} $X ${n(n - 2)}", ordered),
                step("Reihenfolge egal (3 $X 2 $X 1 = 6): ${n(ordered)} $DIV 6", result, "Jedes Team wurde sechsmal gezählt – einmal in jeder Reihenfolge."),
            ),
            wrong = listOf(
                bd(ordered) because "Mit Reihenfolge gezählt. Alle haben die gleiche Aufgabe – jedes Team kommt so sechsmal vor: noch durch 6 teilen.",
                bd(n * (n - 1) / 2) because "Nur Zweierteams gezählt – gesucht sind Dreierteams.",
                bd(n * 3) because "Mit der Teamgröße malgenommen. Richtig: ${n(n)} $X ${n(n - 1)} $X ${n(n - 2)}, dann durch 6.",
                bd(ordered / 3) because "Durch 3 statt durch 6 geteilt – drei Personen lassen sich auf 3 $X 2 $X 1 = 6 Arten anordnen.",
            ),
        )
    }

    // --- 12. Wahrscheinlichkeiten -------------------------------------------------------------

    private fun prob(d: Int, r: Random): ApTask = when (d) {
        1 -> laplace(r)
        2 -> when (r.nextInt(3)) {
            0 -> twoDraws(r)
            1 -> bothTests(r)
            else -> diceSum(r)
        }
        else -> if (r.nextBoolean()) atLeastOne(r) else twoDraws(r)
    }

    private fun laplace(r: Random): ApTask {
        val pos = r.betweenL(2, 12)
        val neg = r.betweenL(2, 20)
        val total = pos + neg
        val (rn, rd) = reduce(pos, total)
        return ApTask(
            category = ApCategory.PROB,
            key = "lap:$pos,$neg",
            text = compose(listOf("Von ${n(total)} Blutproben sind ${n(pos)} positiv. Eine Probe wird zufällig ausgewählt."), "Wie groß ist die Wahrscheinlichkeit, dass sie positiv ist?", 1, r),
            unit = null,
            value = null,
            frac = rn to rd,
            steps = listOf(
                step("Günstige Fälle", pos),
                step("Mögliche Fälle: ${n(pos)} + ${n(neg)}", total),
                fracStep("Wahrscheinlichkeit ${n(pos)}/${n(total)}, gekürzt", pos, total, "Günstige durch mögliche Fälle – und kürzen."),
            ),
            wrongFrac = listOf(
                if (pos < neg) reduce(pos, neg) because "Positive durch negative Proben geteilt – richtig ist: durch alle ${n(total)} Proben." else nearFrac(1L to total),
                reduce(neg, total) because "Das ist die Wahrscheinlichkeit für eine negative Probe.",
                nearFrac(1L to pos),
            ),
        )
    }

    private fun twoDraws(r: Random): ApTask {
        val bad = r.betweenL(2, 5)
        val total = r.betweenL(bad + 3, 12)
        val fav = bad * (bad - 1)
        val all = total * (total - 1)
        val (rn, rd) = reduce(fav, all)
        return ApTask(
            category = ApCategory.PROB,
            key = "two:$bad,$total",
            text = compose(
                listOf("Von ${n(total)} Proben sind ${n(bad)} verunreinigt. Es werden nacheinander zwei Proben zufällig entnommen, ohne die erste zurückzulegen."),
                "Wie groß ist die Wahrscheinlichkeit, dass beide verunreinigt sind?",
                2,
                r,
            ),
            unit = null,
            value = null,
            frac = rn to rd,
            steps = listOf(
                step("Günstig: ${n(bad)} $X ${n(bad - 1)}", fav, "Beim zweiten Ziehen ist eine verunreinigte Probe weniger da."),
                step("Möglich: ${n(total)} $X ${n(total - 1)}", all),
                fracStep("Wahrscheinlichkeit ${n(fav)}/${n(all)}, gekürzt", fav, all),
            ),
            wrongFrac = listOf(
                reduce(bad * bad, total * total) because "Mit Zurücklegen gerechnet. Beim zweiten Ziehen ist aber eine Probe weniger da.",
                reduce(bad, total) because "Nur das erste Ziehen gezählt.",
                if (2 * bad < total) reduce(2 * bad, total) because "Die Wahrscheinlichkeiten addiert statt malgenommen – bei „und“ wird multipliziert." else nearFrac(1L to total),
            ),
        )
    }

    private fun bothTests(r: Random): ApTask {
        val p = r.pick(listOf(60L, 70L, 80L, 90L))
        val both = p * p / 100
        return ApTask(
            category = ApCategory.PROB,
            key = "both:$p",
            text = compose(
                listOf("Ein Schnelltest erkennt eine Infektion mit einer Wahrscheinlichkeit von ${n(p)} %. Eine infizierte Person macht zwei unabhängige Tests."),
                "Mit welcher Wahrscheinlichkeit erkennen beide Tests die Infektion?",
                2,
                r,
            ),
            unit = "%",
            value = bd(both),
            steps = listOf(step("Beide: ${n(p)} $X ${n(p)} $DIV 100", both, "Bei „und“ mit unabhängigen Ereignissen werden die Wahrscheinlichkeiten multipliziert.")),
            wrong = listOf(
                bd(p) because "Nur ein Test gezählt – beide müssen anschlagen.",
                near(bd(2 * p - 100)),
                bd(100 - (100 - p) * (100 - p) / 100) because "Das ist „mindestens ein Test“ – gefragt ist: beide Tests.",
                near(bd(p / 2)),
            ),
        )
    }

    private fun diceSum(r: Random): ApTask {
        val s = r.betweenL(3, 11)
        val count = minOf(s - 1, 13 - s)
        val (rn, rd) = reduce(count, 36)
        return ApTask(
            category = ApCategory.PROB,
            key = "dice:$s",
            text = compose(listOf("Zwei faire Würfel werden gleichzeitig geworfen."), "Wie groß ist die Wahrscheinlichkeit, dass die Augensumme genau ${n(s)} beträgt?", 2, r),
            unit = null,
            value = null,
            frac = rn to rd,
            steps = listOf(
                step("Mögliche Paare: 6 $X 6", 36),
                step("Paare mit Summe ${n(s)} abzählen", count, "Zum Beispiel ${(maxOf(1, s - 6)..minOf(6, s - 1)).joinToString(", ") { "$it+${s - it}" }}."),
                fracStep("Wahrscheinlichkeit ${n(count)}/36, gekürzt", count, 36),
            ),
            wrongFrac = listOf(
                (1L to 11L) because "Die 11 möglichen Summen (2 bis 12) sind nicht gleich wahrscheinlich – gezählt werden die 36 Paare.",
                reduce(count, 12) because "Mit 6 + 6 = 12 statt 6 $X 6 = 36 möglichen Paaren gerechnet.",
                nearFrac(reduce(1, s)),
            ),
        )
    }

    private fun atLeastOne(r: Random): ApTask {
        val q = r.pick(listOf(3L, 4L, 5L, 6L, 10L))
        val none = (q - 1) * (q - 1)
        val all = q * q
        val fav = all - none
        val (rn, rd) = reduce(fav, all)
        val dice = q == 6L && r.nextBoolean()
        val text = if (dice) {
            compose(listOf("Ein fairer Würfel wird zweimal geworfen."), "Wie groß ist die Wahrscheinlichkeit, mindestens eine Sechs zu würfeln?", 3, r)
        } else {
            compose(
                listOf("Ein Messgerät zeigt bei jeder Messung mit einer Wahrscheinlichkeit von 1/${n(q)} einen Fehler an. Es wird zweimal unabhängig gemessen."),
                "Wie groß ist die Wahrscheinlichkeit, dass mindestens eine der beiden Messungen einen Fehler zeigt?",
                3,
                r,
            )
        }
        return ApTask(
            category = ApCategory.PROB,
            key = "one:$q,$dice",
            text = text,
            unit = null,
            value = null,
            frac = rn to rd,
            steps = listOf(
                step("Keinmal (Zähler): ${n(q - 1)} $X ${n(q - 1)}", none, "Gegenwahrscheinlichkeit: „mindestens einmal“ = 1 minus „keinmal“."),
                step("Nenner: ${n(q)} $X ${n(q)}", all),
                step("Mindestens einmal (Zähler): ${n(all)} $MINUS ${n(none)}", fav),
                fracStep("Wahrscheinlichkeit ${n(fav)}/${n(all)}, gekürzt", fav, all),
            ),
            wrongFrac = listOf(
                reduce(2, q) because "Die Wahrscheinlichkeiten addiert (1/${n(q)} + 1/${n(q)}) – so wird „beide Male“ doppelt gezählt.",
                reduce(1, all) because "Das ist „beide Male“ – gefragt ist „mindestens einmal“.",
                reduce(none, all) because "Das ist „keinmal“ – noch von 1 abziehen.",
            ),
        )
    }

    // --- Zusammenstellungen -------------------------------------------------------------------

    /** Der Untertest im TMSnat: 16 Aufgaben in 25 Minuten (im HAM-Nat waren es 30 Minuten). */
    const val TEST_COUNT = 16
    const val TEST_SECONDS = 1_500

    /** Name einer Testsimulation nach Zahl der Aufgaben und Gesamtzeit in Sekunden. */
    fun simulationName(count: Int, seconds: Int): String = when {
        count < TEST_COUNT -> "Kurztest"
        seconds < TEST_SECONDS -> "Tempo-Test"
        seconds > TEST_SECONDS -> "Simulation mit mehr Zeit"
        else -> "TMSnat-Simulation"
    }

    // --- Aufgabentypen (Fehlertraining) -------------------------------------------------------

    /** Lesbare Namen der Aufgabentypen: Kategorie/Art → Name. */
    private val typeNames = mapOf(
        "RATIO/prop" to "Dreisatz", "RATIO/inv" to "Antiproportional", "RATIO/two" to "Dreisatz mit zwei Größen",
        "PERCENT/part" to "Prozentwert", "PERCENT/chg" to "Preis nach Änderung", "PERCENT/base" to "Preis vorher (Grundwert)",
        "PERCENT/rate" to "Prozentsatz der Änderung", "PERCENT/rel" to "Prozente mit Beziehung", "PERCENT/twice" to "Zwei Änderungen nacheinander",
        "RELATIONS/rel" to "Beziehungskette",
        "FRACTIONS/tot" to "Anteil vom Ganzen", "FRACTIONS/rest" to "Rest in Teilen", "FRACTIONS/ror" to "Anteil vom Rest",
        "UNITS/tab" to "Tabletten pro Tag", "UNITS/rate" to "Infusion pro Stunde", "UNITS/kg" to "Dosis pro kg",
        "UNITS/inj" to "Injektion in ml", "UNITS/drop" to "Tropfen pro Minute", "UNITS/ug" to "µg und mg",
        "SPEED/kmh" to "km/h aus Minuten", "SPEED/steps" to "Schritte und Meter", "SPEED/avg" to "Durchschnittstempo",
        "SPEED/meet" to "Begegnung", "SPEED/catch" to "Einholen", "SPEED/ms" to "m/s in km/h",
        "WORK/tog" to "Gemeinsam arbeiten", "WORK/fd" to "Zulauf und Abfluss", "WORK/join" to "Verstärkung kommt dazu",
        "MIX/dil" to "Verdünnen", "MIX/wat" to "Wasser zugeben", "MIX/comb" to "Zwei Lösungen mischen", "MIX/cross" to "Mischungskreuz",
        "GROWTH/dbl" to "Verdopplung", "GROWTH/hl" to "Halbwertszeit", "GROWTH/hlb" to "Halbwertszeit rückwärts", "GROWTH/hlt" to "Zeit aus Halbwertszeit",
        "EQUATIONS/sd" to "Summe und Unterschied", "EQUATIONS/amp" to "Zwei Sorten", "EQUATIONS/age" to "Altersaufgabe", "EQUATIONS/pr" to "Zwei Bestellungen",
        "COMBI/perm" to "Reihenfolgen", "COMBI/prod" to "Kombinieren", "COMBI/ord" to "Ämter besetzen", "COMBI/hs" to "Jeder mit jedem", "COMBI/team" to "Teams auswählen",
        "PROB/lap" to "Einfache Wahrscheinlichkeit", "PROB/two" to "Zweimal ziehen", "PROB/both" to "Beide Ereignisse", "PROB/dice" to "Zwei Würfel", "PROB/one" to "Mindestens einmal",
    )

    /** Alle bekannten Aufgabentypen. */
    val types: Set<String> get() = typeNames.keys

    /** Aufgabentyp einer Aufgabe, z. B. „PERCENT/base“; null bei Aufgaben außerhalb des TMSnat-Trainings. */
    fun typeOf(problem: Problem): String? {
        val category = ApCategory.of(problem) ?: return null
        return "${category.name}/${problem.key.substringBefore(':')}"
    }

    /** Lesbarer Name eines Aufgabentyps, z. B. „Preis vorher (Grundwert)“. */
    fun typeName(type: String): String =
        typeNames[type] ?: ApCategory.entries.firstOrNull { it.name == type.substringBefore('/') }?.title ?: type

    /** Eine neue Aufgabe desselben Typs (andere Zahlen); null, wenn der Typ unbekannt ist. */
    fun taskOfType(type: String, r: Random): ApTask? {
        val category = ApCategory.entries.firstOrNull { it.name == type.substringBefore('/') } ?: return null
        val kind = type.substringAfter('/')
        repeat(600) { i ->
            val t = task(category, listOf(2, 3, 1)[i % 3], r)
            if (t.key.substringBefore(':') == kind) return t
        }
        return null
    }

    /**
     * Aufgaben wie in einer Testsimulation: alle Kategorien kommen vor, die Aufgaben werden
     * nach hinten schwerer.
     */
    fun simulation(count: Int, options: Int, r: Random): List<Problem> {
        val categories = ApCategory.entries.shuffled(r).toMutableList()
        while (categories.size < count) categories += ApCategory.entries.shuffled(r).take(count - categories.size)
        val chosen = categories.take(count).shuffled(r)
        return chosen.mapIndexed { i, c ->
            val difficulty = 1 + (3 * i) / count
            choice(task(c, difficulty, r), options, r)
        }
    }

    /** Eine gemischte Aufgabe mit Antwortmöglichkeiten. */
    fun mixed(options: Int, difficulty: Int, r: Random, categories: Collection<ApCategory> = ApCategory.entries): Problem =
        choice(task(categories.toList()[r.nextInt(categories.size)], difficulty, r), options, r)
}
