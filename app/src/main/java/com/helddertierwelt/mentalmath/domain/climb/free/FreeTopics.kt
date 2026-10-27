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

package com.helddertierwelt.mentalmath.domain.climb.free

import com.helddertierwelt.mentalmath.domain.climb.curriculum.Methods
import com.helddertierwelt.mentalmath.domain.climb.curriculum.TwoByTwo
import com.helddertierwelt.mentalmath.domain.climb.curriculum.betweenL
import com.helddertierwelt.mentalmath.domain.climb.curriculum.decStep
import com.helddertierwelt.mentalmath.domain.climb.curriculum.pick
import com.helddertierwelt.mentalmath.domain.climb.curriculum.step
import com.helddertierwelt.mentalmath.domain.climb.math.Fmt
import com.helddertierwelt.mentalmath.domain.climb.math.Num
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

/** Aufgabenarten des freien Trainings. */
enum class FreeTopic(val title: String, val subtitle: String) {
    BASIC("Grundrechenarten", "Plus, Minus, Mal, Geteilt"),
    TWO_OPS("Zwei Rechenzeichen", "Punkt vor Strich"),
    FOUR_OPS("Lange Terme", "Drei Rechenzeichen, auch Klammern"),
    PERCENT("Prozente", "Anteile, Rabatte, Zu- und Abnahme"),
    WORD("Textaufgaben", "Einkaufen, Strecken, Teilen"),
    ALGEBRA("Gleichungen", "x bestimmen"),
    POWERS("Potenzen und Wurzeln", "Quadrate, Kuben, Zweierpotenzen"),
    GEOMETRY("Geometrie", "Flächen, Umfänge, Winkel, Volumen"),
    NUMBER_THEORY("Zahlentheorie", "ggT, kgV, Primzahlen, Reste"),
}

/** Erzeugt Aufgaben für das freie Training; Schwierigkeit 1 (leicht) bis 3 (schwer). */
object FreeTopics {

    private const val X = Fmt.TIMES
    private const val MINUS = Fmt.MINUS
    private const val DIV = Fmt.DIVIDE
    private val yesNo = listOf("Ja", "Nein")

    private fun n(x: Long) = Fmt.int(x)
    private fun d(x: BigDecimal, places: Int? = null) = Fmt.dec(x, places)
    private fun id(topic: FreeTopic) = "free." + topic.name.lowercase()

    fun generate(topic: FreeTopic, level: Int, r: Random): Problem = when (topic) {
        FreeTopic.BASIC -> basic(level, r)
        FreeTopic.TWO_OPS -> twoOps(level, r)
        FreeTopic.FOUR_OPS -> fourOps(level, r)
        FreeTopic.PERCENT -> percent(level, r)
        FreeTopic.WORD -> word(level, r)
        FreeTopic.ALGEBRA -> algebra(level, r)
        FreeTopic.POWERS -> powers(level, r)
        FreeTopic.GEOMETRY -> geometry(level, r)
        FreeTopic.NUMBER_THEORY -> numberTheory(level, r)
    }

    private fun intProblem(topic: FreeTopic, key: String, prompt: Prompt, value: Long, steps: List<Step>, method: String? = null) =
        Problem(id(topic), key, prompt, Expected.Exact(BigDecimal.valueOf(value)), InputKind.INTEGER, steps, method)

    private fun decProblem(topic: FreeTopic, key: String, prompt: Prompt, value: BigDecimal, places: Int?, steps: List<Step>, method: String? = null) =
        Problem(id(topic), key, prompt, Expected.Exact(value, places), InputKind.DECIMAL, steps, method)

    private fun one(label: String, value: Long, hint: String? = null) = listOf(step(label, value, hint))

    // --- Grundrechenarten --------------------------------------------------------------------

    private fun basic(level: Int, r: Random): Problem {
        val t = FreeTopic.BASIC
        return when (r.nextInt(4)) {
            0 -> {
                val (a, b) = when (level) {
                    1 -> r.betweenL(2, 60) to r.betweenL(2, 40)
                    2 -> r.betweenL(23, 99) to r.betweenL(12, 99)
                    else -> r.betweenL(123, 999) to r.betweenL(102, 999)
                }
                intProblem(t, "$a+$b", Prompt("${n(a)} + ${n(b)}"), a + b, Methods.addLeftToRight(a, b), "Von links nach rechts")
            }

            1 -> {
                val a = when (level) { 1 -> r.betweenL(10, 99); 2 -> r.betweenL(50, 199); else -> r.betweenL(300, 999) }
                val b = r.betweenL(1, a - 1)
                intProblem(t, "$a-$b", Prompt("${n(a)} $MINUS ${n(b)}"), a - b, Methods.subtractLeftToRight(a, b), "Von links nach rechts")
            }

            2 -> when (level) {
                1 -> {
                    val a = r.betweenL(2, 10); val b = r.betweenL(2, 10)
                    intProblem(t, "$a*$b", Prompt("$a $X $b"), a * b, one("$a $X $b", a * b, "Einmaleins."))
                }
                2 -> {
                    val a = r.betweenL(12, 99); val b = r.betweenL(3, 9)
                    intProblem(t, "$a*$b", Prompt("${n(a)} $X $b"), a * b, Methods.multiplyLeftToRight(a, b), "Von links nach rechts")
                }
                else -> {
                    val a = r.betweenL(12, 99); val b = r.betweenL(12, 99)
                    val s = TwoByTwo.best(a, b)
                    intProblem(t, "$a*$b", Prompt("${n(a)} $X ${n(b)}"), a * b, s.steps, s.method)
                }
            }

            else -> {
                val divisor = if (level == 1) r.betweenL(2, 10) else r.betweenL(3, 12)
                val q = when (level) { 1 -> r.betweenL(2, 10); 2 -> r.betweenL(11, 30); else -> r.betweenL(21, 99) }
                val a = divisor * q
                intProblem(t, "$a/$divisor", Prompt("${n(a)} $DIV ${n(divisor)}"), q, one("${n(a)} $DIV ${n(divisor)}", q, "Welche Zahl mal ${n(divisor)} ergibt ${n(a)}?"))
            }
        }
    }

    // --- Mehrere Rechenzeichen ---------------------------------------------------------------

    private fun twoOps(level: Int, r: Random): Problem {
        val t = FreeTopic.TWO_OPS
        val max = when (level) { 1 -> 10L; 2 -> 20L; else -> 50L }
        val a = r.betweenL(2, max * 2)
        val b = r.betweenL(2, if (level == 3) 15 else 9)
        val c = r.betweenL(2, if (level == 3) 15 else 9)
        return when (r.nextInt(4)) {
            0 -> intProblem(t, "$a+$b*$c", Prompt("$a + $b $X $c"), a + b * c, listOf(step("$b $X $c", b * c, "Punkt vor Strich: zuerst malnehmen."), step("$a + ${n(b * c)}", a + b * c)))
            1 -> {
                val p = b * c
                val big = p + a
                intProblem(t, "$big-$b*$c", Prompt("${n(big)} $MINUS $b $X $c"), big - p, listOf(step("$b $X $c", p, "Punkt vor Strich: zuerst malnehmen."), step("${n(big)} $MINUS ${n(p)}", big - p)))
            }
            2 -> intProblem(t, "($a+$b)*$c", Prompt("($a + $b) $X $c"), (a + b) * c, listOf(step("$a + $b", a + b, "Die Klammer zuerst."), step("${n(a + b)} $X $c", (a + b) * c)))
            else -> {
                val q = b * c
                intProblem(t, "$q/$b+$a", Prompt("${n(q)} $DIV $b + $a"), c + a, listOf(step("${n(q)} $DIV $b", c, "Punkt vor Strich: zuerst teilen."), step("$c + $a", c + a)))
            }
        }
    }

    private fun fourOps(level: Int, r: Random): Problem {
        val t = FreeTopic.FOUR_OPS
        val m = if (level == 1) 6L else if (level == 2) 9L else 15L
        val a = r.betweenL(2, m); val b = r.betweenL(2, m); val c = r.betweenL(2, m); val e = r.betweenL(2, m)
        return when (r.nextInt(3)) {
            0 -> {
                val p1 = a * b; val p2 = c * e
                val total = p1 + p2
                val f = r.betweenL(1, minOf(total - 1, 30))
                intProblem(
                    t, "$a*$b+$c*$e-$f", Prompt("$a $X $b + $c $X $e $MINUS $f"), total - f,
                    listOf(step("$a $X $b", p1, "Erst alle Malaufgaben."), step("$c $X $e", p2), step("${n(p1)} + ${n(p2)}", total, "Dann von links nach rechts."), step("${n(total)} $MINUS $f", total - f)),
                )
            }
            1 -> {
                val s = a + b
                val q = c * e
                intProblem(
                    t, "($a+$b)*$c-$q/$e", Prompt("($a + $b) $X $c $MINUS ${n(q)} $DIV $e"), s * c - c,
                    listOf(step("$a + $b", s, "Klammer zuerst."), step("${n(s)} $X $c", s * c, "Dann Punktrechnung."), step("${n(q)} $DIV $e", c), step("${n(s * c)} $MINUS $c", s * c - c, "Zum Schluss Strichrechnung.")),
                )
            }
            else -> {
                val inner = a * b + c
                intProblem(
                    t, "($a*$b+$c)*$e", Prompt("($a $X $b + $c) $X $e"), inner * e,
                    listOf(step("$a $X $b", a * b, "In der Klammer: Punkt vor Strich."), step("${n(a * b)} + $c", inner), step("${n(inner)} $X $e", inner * e, "Dann die Klammer malnehmen.")),
                )
            }
        }
    }

    // --- Prozente ----------------------------------------------------------------------------

    private fun percent(level: Int, r: Random): Problem {
        val t = FreeTopic.PERCENT
        val pcts = if (level == 1) listOf(10L, 20L, 25L, 50L) else if (level == 2) listOf(5L, 15L, 30L, 40L, 60L, 75L) else listOf(12L, 18L, 35L, 45L, 65L, 85L)
        val p = r.pick(pcts)
        return when (r.nextInt(3)) {
            0 -> {
                val base = if (p % 5 == 0L) r.betweenL(2, if (level == 3) 90 else 40) * 20 else r.betweenL(2, 40) * 100
                val value = base * p / 100
                intProblem(t, "$p%$base", Prompt("$p % von ${n(base)}"), value, listOf(step("1 % von ${n(base)}", base / 100, "Durch 100 teilen.").let { it.copy(expected = Expected.Exact(BigDecimal.valueOf(base).divide(BigDecimal(100))), input = InputKind.DECIMAL) }, step("${n(base)} $X $p $DIV 100", value, "$p-mal so viel.")))
            }
            1 -> {
                val price = if (p % 5 == 0L) r.betweenL(2, 40) * 20 else r.betweenL(1, 20) * 100
                val off = price * p / 100
                intProblem(
                    t, "r$p$price",
                    Prompt("? €", caption = "Ein Artikel kostet ${n(price)} €. Es gibt $p % Rabatt. Wie viel kostet er jetzt?"),
                    price - off,
                    listOf(step("$p % von ${n(price)}", off, "Erst den Rabatt ausrechnen."), step("${n(price)} $MINUS ${n(off)}", price - off, "Vom Preis abziehen.")),
                )
            }
            else -> {
                val from = if (p % 5 == 0L) r.betweenL(2, 20) * 20 else r.betweenL(1, 10) * 100
                val up = r.nextBoolean()
                val change = from * p / 100
                val to = if (up) from + change else from - change
                intProblem(
                    t, "c$from>$to",
                    Prompt("? %", caption = "Von ${n(from)} auf ${n(to)} – um wie viel Prozent ${if (up) "gestiegen" else "gesunken"}?"),
                    p,
                    listOf(step(if (up) "${n(to)} $MINUS ${n(from)}" else "${n(from)} $MINUS ${n(to)}", change, "Erst die Änderung."), step("${n(change)} $X 100 $DIV ${n(from)}", p, "Dann als Anteil vom Ausgangswert in Prozent.")),
                )
            }
        }
    }

    // --- Textaufgaben ------------------------------------------------------------------------

    private val items = listOf("Heft" to "Hefte", "Brötchen" to "Brötchen", "Kugel Eis" to "Kugeln Eis", "Busticket" to "Bustickets", "Apfel" to "Äpfel")

    private fun word(level: Int, r: Random): Problem {
        val t = FreeTopic.WORD
        return when (r.nextInt(4)) {
            0 -> {
                val (one, many) = r.pick(items)
                val cents = r.betweenL(if (level == 1) 1 else 6, if (level == 3) 39 else 19) * 5 + if (level == 3) 0 else 0
                val count = r.betweenL(3, if (level == 1) 5 else 9)
                val total = BigDecimal.valueOf(cents * count, 2)
                decProblem(
                    t, "w$cents*$count",
                    Prompt("? €", caption = "Ein $one kostet ${Fmt.money(BigDecimal.valueOf(cents, 2))}. Wie viel kosten $count $many?"),
                    total, 2,
                    listOf(step("${n(cents)} $X $count", cents * count, "In Cent rechnen."), decStep("Ergebnis in Euro", total, 2, "Durch 100: Komma zwei Stellen nach links.")),
                )
            }
            1 -> {
                val speed = r.pick(listOf(40L, 60L, 80L, 90L, 120L))
                val halfHours = r.betweenL(1, if (level == 1) 4 else 8)
                val km = speed * halfHours / 2
                val hours = BigDecimal.valueOf(halfHours * 5, 1)
                intProblem(
                    t, "s$speed*$halfHours",
                    Prompt("? km", caption = "Ein Auto fährt ${d(hours)} Stunden lang mit ${n(speed)} km/h. Wie weit kommt es?"),
                    km,
                    listOf(step("${n(speed)} $X ${d(hours)}", km, "Strecke = Geschwindigkeit mal Zeit.")),
                )
            }
            2 -> {
                val kids = r.betweenL(3, if (level == 1) 6 else 9)
                val each = r.betweenL(3, if (level == 3) 25 else 12)
                val total = kids * each
                intProblem(
                    t, "t$total/$kids",
                    Prompt("?", caption = "${n(total)} Bonbons werden gerecht an $kids Kinder verteilt. Wie viele bekommt jedes Kind?"),
                    each,
                    listOf(step("${n(total)} $DIV $kids", each, "Gerecht verteilen heißt teilen.")),
                )
            }
            else -> {
                val money = r.pick(listOf(20L, 50L, 100L))
                val spent = r.betweenL(101, money * 100 - 1)
                val rest = money * 100 - spent
                decProblem(
                    t, "m$money-$spent",
                    Prompt("? €", caption = "Du bezahlst ${Fmt.money(BigDecimal.valueOf(spent, 2))} mit einem ${n(money)}-€-Schein. Wie viel Wechselgeld bekommst du?"),
                    BigDecimal.valueOf(rest, 2), 2,
                    listOf(step("${n(money * 100)} $MINUS ${n(spent)}", rest, "In Cent: bis zum Schein ergänzen."), decStep("Wechselgeld in Euro", BigDecimal.valueOf(rest, 2), 2)),
                )
            }
        }
    }

    // --- Gleichungen -------------------------------------------------------------------------

    private fun algebra(level: Int, r: Random): Problem {
        val t = FreeTopic.ALGEBRA
        val x = r.betweenL(1, if (level == 1) 10 else 20)
        val a = r.betweenL(2, if (level == 1) 5 else 9)
        val b = r.betweenL(1, 30)
        return if (level == 3 && r.nextBoolean()) {
            val c = r.betweenL(1, a - 1).coerceAtLeast(1)
            val dd = a * x + b - c * x
            intProblem(
                t, "e$a,$b,$c,$dd",
                Prompt("${a}x + $b = ${c}x + ${n(dd)}", caption = "x = ?"),
                x,
                listOf(step("$a $MINUS $c", a - c, "Alle x auf eine Seite: ${a}x − ${c}x."), step("${n(dd)} $MINUS $b", dd - b, "Die Zahlen auf die andere Seite."), step("${n(dd - b)} $DIV ${a - c}", x, "Durch die Anzahl der x teilen.")),
            )
        } else if (r.nextBoolean()) {
            val rhs = a * x + b
            intProblem(
                t, "l$a,$b,$rhs",
                Prompt("${a}x + $b = ${n(rhs)}", caption = "x = ?"),
                x,
                listOf(step("${n(rhs)} $MINUS $b", rhs - b, "Erst die $b auf beiden Seiten abziehen."), step("${n(rhs - b)} $DIV $a", x, "Dann durch $a teilen.")),
            )
        } else {
            val rhs = a * x - minOf(b, a * x - 1)
            val bb = a * x - rhs
            intProblem(
                t, "m$a,$bb,$rhs",
                Prompt("${a}x $MINUS $bb = ${n(rhs)}", caption = "x = ?"),
                x,
                listOf(step("${n(rhs)} + $bb", rhs + bb, "Erst $bb auf beiden Seiten addieren."), step("${n(rhs + bb)} $DIV $a", x, "Dann durch $a teilen.")),
            )
        }
    }

    // --- Potenzen und Wurzeln ----------------------------------------------------------------

    private fun powers(level: Int, r: Random): Problem {
        val t = FreeTopic.POWERS
        return when (r.nextInt(4)) {
            0 -> {
                val a = r.betweenL(2, when (level) { 1 -> 15L; 2 -> 30L; else -> 99L })
                intProblem(t, "sq$a", Prompt("${n(a)}²"), a * a, one("${n(a)} $X ${n(a)}", a * a, "Eine Zahl mal sich selbst."))
            }
            1 -> {
                val a = r.betweenL(2, if (level == 1) 5 else if (level == 2) 10 else 20)
                intProblem(t, "cu$a", Prompt("${n(a)}³"), a * a * a, listOf(step("${n(a)} $X ${n(a)}", a * a), step("${n(a * a)} $X ${n(a)}", a * a * a)))
            }
            2 -> {
                val e = r.betweenL(2, if (level == 1) 6 else if (level == 2) 10 else 16).toInt()
                val v = Num.pow2(e)
                intProblem(t, "p$e", Prompt("2^$e"), v, one("2 hoch $e", v, "Immer wieder verdoppeln: 2, 4, 8, 16, …"))
            }
            else -> {
                val root = r.betweenL(2, if (level == 1) 12 else if (level == 2) 25 else 99)
                intProblem(t, "r$root", Prompt("√${n(root * root)}"), root, one("√${n(root * root)}", root, "Welche Zahl mal sich selbst ergibt ${n(root * root)}?"))
            }
        }
    }

    // --- Geometrie ---------------------------------------------------------------------------

    private fun geometry(level: Int, r: Random): Problem {
        val t = FreeTopic.GEOMETRY
        val m = if (level == 1) 12L else if (level == 2) 25L else 60L
        return when (r.nextInt(6)) {
            0 -> {
                val a = r.betweenL(2, m); val b = r.betweenL(2, m)
                intProblem(t, "ra$a,$b", Prompt("? cm²", caption = "Rechteck: ${n(a)} cm lang, ${n(b)} cm breit. Fläche?"), a * b, one("${n(a)} $X ${n(b)}", a * b, "Fläche = Länge mal Breite."))
            }
            1 -> {
                val a = r.betweenL(2, m); val b = r.betweenL(2, m)
                intProblem(t, "ru$a,$b", Prompt("? cm", caption = "Rechteck: ${n(a)} cm lang, ${n(b)} cm breit. Umfang?"), 2 * (a + b), listOf(step("${n(a)} + ${n(b)}", a + b, "Umfang = 2 × (Länge + Breite)."), step("${n(a + b)} $X 2", 2 * (a + b))))
            }
            2 -> {
                val g = r.betweenL(2, m) * 2; val h = r.betweenL(2, m)
                intProblem(t, "dr$g,$h", Prompt("? cm²", caption = "Dreieck: Grundseite ${n(g)} cm, Höhe ${n(h)} cm. Fläche?"), g * h / 2, listOf(step("${n(g)} $X ${n(h)}", g * h, "Fläche = Grundseite mal Höhe, halbiert."), step("${n(g * h)} $DIV 2", g * h / 2)))
            }
            3 -> {
                val a = r.betweenL(20, 100); val b = r.betweenL(20, 150 - a)
                intProblem(t, "wi$a,$b", Prompt("? °", caption = "Ein Dreieck hat die Winkel ${n(a)}° und ${n(b)}°. Wie groß ist der dritte?"), 180 - a - b, listOf(step("${n(a)} + ${n(b)}", a + b, "Im Dreieck sind alle Winkel zusammen 180°."), step("180 $MINUS ${n(a + b)}", 180 - a - b)))
            }
            4 -> {
                val a = r.betweenL(2, m / 2 + 2); val b = r.betweenL(2, m / 2 + 2); val c = r.betweenL(2, 10)
                intProblem(t, "qu$a,$b,$c", Prompt("? cm³", caption = "Quader: ${n(a)} cm × ${n(b)} cm × ${n(c)} cm. Volumen?"), a * b * c, listOf(step("${n(a)} $X ${n(b)}", a * b, "Grundfläche …"), step("${n(a * b)} $X ${n(c)}", a * b * c, "… mal Höhe.")))
            }
            else -> {
                val dia = r.betweenL(2, if (level == 1) 10 else 30)
                val u = BigDecimal("3.14").multiply(BigDecimal.valueOf(dia))
                Problem(
                    id(t), "ku$dia",
                    Prompt("? cm", caption = "Kreis mit ${n(dia)} cm Durchmesser. Umfang (π ≈ 3,14)?"),
                    Expected.Approx(u, BigDecimal("0.5"), 2),
                    InputKind.DECIMAL,
                    listOf(Step("3,14 $X ${n(dia)}", Expected.Exact(u, 2), InputKind.DECIMAL, "Umfang = π mal Durchmesser.")),
                    "Umfang = π × d",
                )
            }
        }
    }

    // --- Zahlentheorie -----------------------------------------------------------------------

    private fun numberTheory(level: Int, r: Random): Problem {
        val t = FreeTopic.NUMBER_THEORY
        val m = if (level == 1) 12L else if (level == 2) 30L else 99L
        return when (r.nextInt(4)) {
            0 -> {
                val g = r.betweenL(2, if (level == 1) 6 else 12)
                val a = g * r.betweenL(2, m / 2 + 2); val b = g * r.betweenL(2, m / 2 + 2)
                val gg = Num.gcd(a, b)
                intProblem(t, "g$a,$b", Prompt("ggT(${n(a)}, ${n(b)})", caption = "Größter gemeinsamer Teiler"), gg, one("ggT(${n(a)}, ${n(b)})", gg, "Die größte Zahl, die beide teilt."))
            }
            1 -> {
                val a = r.betweenL(2, m / 2 + 4); val b = r.betweenL(2, m / 2 + 4)
                val l = a / Num.gcd(a, b) * b
                intProblem(t, "k$a,$b", Prompt("kgV(${n(a)}, ${n(b)})", caption = "Kleinstes gemeinsames Vielfaches"), l, one("kgV(${n(a)}, ${n(b)})", l, "${n(a)} $X ${n(b)} $DIV ggT = ${n(l)}."))
            }
            2 -> {
                val x = r.betweenL(11, if (level == 1) 60 else if (level == 2) 150 else 400)
                val prime = Num.isPrime(x)
                val divisor = (2L..x).firstOrNull { x % it == 0L && it < x }
                Problem(
                    id(t), "pr$x",
                    Prompt("${n(x)}", caption = "Ist das eine Primzahl?"),
                    Expected.Choice(if (prime) 0 else 1),
                    InputKind.CHOICE,
                    listOf(Step("${n(x)} prim?", Expected.Choice(if (prime) 0 else 1), InputKind.CHOICE, if (prime) "Keine Zahl außer 1 und ${n(x)} teilt sie." else "Teilbar durch ${divisor?.let { n(it) }}.")),
                    "Teiler suchen",
                    choices = yesNo,
                )
            }
            else -> {
                val k = r.betweenL(3, if (level == 3) 12 else 9)
                val x = r.betweenL(20, if (level == 1) 99 else 999)
                Problem(
                    id(t), "re$x,$k",
                    Prompt("${n(x)} $DIV $k", caption = "Mit Rest – Taste „Rest“ für R"),
                    Expected.Remainder(x / k, x % k),
                    InputKind.REMAINDER,
                    listOf(Step("${n(x)} $DIV $k", Expected.Remainder(x / k, x % k), InputKind.REMAINDER, "${n(k)} $X ${n(x / k)} = ${n(k * (x / k))}, Rest ${n(x % k)}.")),
                    "Teilen mit Rest",
                )
            }
        }
    }
}
