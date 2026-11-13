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

import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.bd
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.ds
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.mistake
import com.helddertierwelt.mentalmath.domain.climb.curriculum.ExtraKit.signed
import com.helddertierwelt.mentalmath.domain.climb.model.BookRef
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.World
import java.math.BigDecimal
import java.math.MathContext
import kotlin.random.Random

/**
 * Welt 16 (Ergänzung): Mathe im Alltag – negative Zahlen, Mehrwertsteuer, Rabatte, Zinsen,
 * Verbrauch, Rezepte, Uhrzeiten, Rechnung teilen, Grundpreis.
 */
object World16 {

    private val book = BookRef(null, "Ergänzung: Mathe im Alltag")

    private fun euro(x: BigDecimal): String = d(x.setScale(2, java.math.RoundingMode.HALF_UP))

    private fun negative(id: String, r: Random): Problem {
        val a = r.between(-20, 20).toLong()
        val b = r.between(2, 25).toLong()
        val c = r.between(2, 25).toLong()
        val plusB = r.nextBoolean()
        val plusC = r.nextBoolean()
        val res = a + (if (plusB) b else -b) + (if (plusC) c else -c)
        val text = "${signed(a)} ${if (plusB) "+" else MINUS} ${n(b)} ${if (plusC) "+" else MINUS} ${n(c)}"
        val mid = a + if (plusB) b else -b
        val wrong = listOf(-res, res + 2, res - 2, -a + (if (plusB) b else -b) + (if (plusC) c else -c), res + 10).map { signed(it) }
        return ExtraKit.choice(
            id, "neg:$text", Prompt(text, caption = "Rechnen mit negativen Zahlen"), signed(res), wrong, "Ergebnis",
            "Zuerst ${signed(a)} ${if (plusB) "+" else MINUS} ${n(b)} = ${signed(mid)}, dann ${if (plusC) "+" else MINUS} ${n(c)} = ${signed(res)}. Auf dem Zahlenstrahl heißt Plus nach rechts, Minus nach links.",
            "Zahlenstrahl: Plus nach rechts, Minus nach links", r,
            mapOf(signed(-res) to "Vorzeichen vertauscht – überlege, auf welcher Seite der Null du landest."),
        )
    }

    private fun vat(id: String, r: Random): Problem {
        val rate = r.pick(listOf(19L, 19L, 7L))
        return if (r.nextBoolean()) {
            val net = bd(r.between(2, 99).toLong() * r.pick(listOf(10L, 5L, 20L, 50L)))
            val tax = net.multiply(bd(rate)).divide(bd(100)).stripTrailingZeros()
            val gross = net + tax
            ExtraKit.dec(
                id, "vat:$net+$rate", Prompt("Netto ${d(net)} € + $rate % MwSt = ? €", caption = "Bruttopreis"), gross,
                listOf(ds("${d(net)} $X 0,${"%02d".format(rate)}", tax, "Die Steuer: $rate % vom Nettopreis."), ds("${d(net)} + ${d(tax)}", gross, "Netto plus Steuer ergibt brutto.")),
                "Netto × 1,${"%02d".format(rate)}",
                listOf(mistake(tax, "Das ist nur die Steuer – der Bruttopreis enthält auch den Nettopreis.")),
            )
        } else {
            val net = bd(r.between(2, 60).toLong() * 100)
            val gross = net.multiply(bd("1.$rate".let { if (rate < 10) "1.0$rate" else it })).stripTrailingZeros()
            val factor = if (rate < 10) "1,07" else "1,19"
            val wrongNet = gross.multiply(bd(100 - rate)).divide(bd(100)).stripTrailingZeros()
            ExtraKit.dec(
                id, "vat:$gross-$rate", Prompt("Brutto ${d(gross)} € mit $rate % MwSt = ? € netto", caption = "Nettopreis"), net,
                listOf(ds("${d(gross)} $DIV $factor", net, "Brutto sind ${100 + rate} % des Nettopreises – also durch $factor teilen.")),
                "Brutto ÷ $factor",
                listOf(mistake(wrongNet, "$rate % vom Bruttopreis abgezogen – die Steuer wird aber auf den Nettopreis berechnet.")),
            )
        }
    }

    private fun discount(id: String, r: Random): Problem {
        return if (r.nextBoolean()) {
            val price = bd(r.between(2, 60).toLong() * r.pick(listOf(5L, 10L, 20L)))
            val pct = r.pick(listOf(10L, 15L, 20L, 25L, 30L, 40L, 50L, 70L))
            val off = price.multiply(bd(pct)).divide(bd(100)).stripTrailingZeros()
            val res = price - off
            ExtraKit.dec(
                id, "disc:$price-$pct", Prompt("${d(price)} € $MINUS $pct % = ? €", caption = "Preis nach Rabatt"), res,
                listOf(ds("${d(price)} $X $pct $DIV 100", off, "Der Rabatt in Euro."), ds("${d(price)} $MINUS ${d(off)}", res, "Vom Preis abziehen.")),
                "Rabatt abziehen",
                listOf(mistake(off, "Das ist der Rabatt – gefragt ist der neue Preis.")),
            )
        } else {
            val p1 = r.pick(listOf(10L, 20L, 25L, 30L, 50L))
            val p2 = r.pick(listOf(10L, 20L, 50L))
            val f1 = bd(100 - p1).divide(bd(100))
            val f2 = bd(100 - p2).divide(bd(100))
            val rest = f1.multiply(f2).stripTrailingZeros()
            val total = bd(100) - rest.multiply(bd(100))
            ExtraKit.dec(
                id, "disc:$p1+$p2", Prompt("Erst $p1 % Rabatt, dann noch $p2 %: zusammen ? %", caption = "Rabatt auf Rabatt"), total,
                listOf(ds("${d(f1)} $X ${d(f2)}", rest, "Es bleiben ${100 - p1} % und davon ${100 - p2} %."), ds("100 $MINUS ${d(rest.multiply(bd(100)))}", total, "Der Rest zu 100 % ist der Gesamtrabatt.")),
                "Restanteile malnehmen",
                listOf(mistake(bd(p1 + p2), "Rabatte addiert – der zweite Rabatt gilt aber nur noch für den schon reduzierten Preis.")),
            )
        }
    }

    private fun interest(id: String, r: Random): Problem {
        return if (r.nextInt(3) == 0) {
            val k = bd(r.between(1, 50).toLong() * 100)
            val p = r.pick(listOf(1L, 2L, 3L, 4L, 5L, 6L))
            val z = k.multiply(bd(p)).divide(bd(100)).stripTrailingZeros()
            ExtraKit.dec(
                id, "int:$k*$p", Prompt("${d(k)} € zu $p % Zinsen für 1 Jahr = ? € Zinsen", caption = "Zinsen"), z,
                listOf(ds("${d(k)} $X $p $DIV 100", z, "1 % von ${d(k)} € sind ${d(k.divide(bd(100)))} €.")),
                "Kapital × Zinssatz",
            )
        } else {
            val k = bd(r.pick(listOf(100L, 200L, 500L, 1000L, 2000L, 5000L, 10000L)))
            val p = r.pick(listOf(10L, 20L, 5L, 50L))
            val years = if (p == 5L) 2 else r.between(2, 3)
            val f = bd(100 + p).divide(bd(100))
            val steps = mutableListOf<com.helddertierwelt.mentalmath.domain.climb.model.Step>()
            var v = k
            repeat(years) { i ->
                val next = v.multiply(f).stripTrailingZeros()
                steps += ds("${d(v)} $X ${d(f)}", next, if (i == 0) "Jedes Jahr kommen $p % dazu – auch auf die Zinsen des Vorjahres." else null)
                v = next
            }
            val simple = k + k.multiply(bd(p * years)).divide(bd(100))
            ExtraKit.dec(
                id, "comp:$k*$p^$years", Prompt("${d(k)} € mit $p % Zinseszins, $years Jahre = ? €", caption = "Zinseszins"), v, steps,
                "Jedes Jahr × ${d(f)}",
                listOf(mistake(simple, "Ohne Zinseszins gerechnet – die Zinsen werden jedes Jahr mitverzinst.")),
            )
        }
    }

    private fun fuel(id: String, r: Random): Problem = when (r.nextInt(3)) {
        0 -> {
            val per100 = BigDecimal(r.pick(listOf("4", "5", "5.5", "6", "6.5", "7", "7.5", "8", "9", "10")))
            val km = r.between(1, 12).toLong() * 50
            val hundreds = bd(km).divide(bd(100)).stripTrailingZeros()
            val l = per100.multiply(hundreds).stripTrailingZeros()
            ExtraKit.dec(
                id, "fuel:$per100*$km", Prompt("${d(per100)} l/100 km, ${n(km)} km = ? l", caption = "Spritverbrauch"), l,
                listOf(ds("${n(km)} $DIV 100", hundreds, "So viele Hundert-Kilometer-Stücke."), ds("${d(hundreds)} $X ${d(per100)}", l, "Je 100 km ${d(per100)} Liter.")),
                "Strecke ÷ 100 × Verbrauch",
            )
        }
        1 -> {
            val l = bd(r.between(10, 60).toLong())
            val price = BigDecimal(r.pick(listOf("1.5", "1.6", "1.7", "1.8", "1.9", "2")))
            val cost = l.multiply(price).stripTrailingZeros()
            ExtraKit.dec(
                id, "fuel:$l*$price", Prompt("${d(l)} l zu ${euro(price)} € pro Liter = ? €", caption = "Tanken"), cost,
                listOf(ds("${d(l)} $X ${d(price)}", cost, "Liter mal Preis pro Liter.")),
                "Menge × Preis",
            )
        }
        else -> {
            val per100 = bd(r.between(4, 10).toLong())
            val km = r.between(2, 12).toLong() * 100
            val used = per100.multiply(bd(km)).divide(bd(100)).stripTrailingZeros()
            val hundreds = bd(km).divide(bd(100))
            ExtraKit.dec(
                id, "fuel:$used/$km", Prompt("${d(used)} l auf ${n(km)} km = ? l/100 km", caption = "Verbrauch ausrechnen"), per100,
                listOf(ds("${d(used)} $DIV ${d(hundreds)}", per100, "Durch die Anzahl der Hundert-Kilometer-Stücke teilen.")),
                "Liter ÷ (km ÷ 100)",
            )
        }
    }

    private fun recipe(id: String, r: Random): Problem {
        val from = r.pick(listOf(2L, 3L, 4L, 5L, 6L, 8L))
        val to = r.pick(listOf(2L, 3L, 4L, 5L, 6L, 8L, 10L, 12L).filter { it != from })
        val perPerson = r.pick(listOf(25L, 50L, 75L, 100L, 120L, 150L, 40L, 60L, 80L))
        val amount = perPerson * from
        val unit = r.pick(listOf("g Mehl", "g Nudeln", "ml Milch", "g Zucker", "ml Sahne"))
        return ExtraKit.int(
            id, "recipe:$amount/$from*$to", Prompt("$from Personen: ${n(amount)} $unit. Für $to Personen = ?", caption = "Rezept umrechnen"), perPerson * to,
            listOf(step("${n(amount)} $DIV $from", perPerson, "Erst auf eine Person herunterrechnen."), step("${n(perPerson)} $X $to", perPerson * to, "Dann für alle Personen.")),
            "Dreisatz über eine Person",
            listOf(mistake(amount + (to - from), "Personen-Unterschied einfach dazugezählt – die Menge ändert sich im Verhältnis.")),
        )
    }

    private fun clock(id: String, r: Random): Problem {
        return if (r.nextBoolean()) {
            val h = r.between(6, 20)
            val m = r.between(0, 11) * 5
            val dh = r.between(0, 4)
            val dm = r.between(1, 11) * 5
            val total = h * 60 + m + dh * 60 + dm
            val eh = (total / 60) % 24
            val em = total % 60
            fun t(hh: Int, mm: Int) = "$hh:${"%02d".format(mm)}"
            val correct = t(eh, em)
            val decimalWrong = t((h + dh + (m + dm) / 100) % 24, (m + dm) % 100 % 60)
            val wrong = listOf(t((eh + 1) % 24, em), t(eh, (em + 10) % 60), t((eh + 23) % 24, em), decimalWrong, t(eh, (em + 50) % 60))
            ExtraKit.choice(
                id, "clock:${t(h, m)}+$dh:$dm", Prompt("${t(h, m)} Uhr + ${if (dh > 0) "$dh h " else ""}$dm min = ?", caption = "Uhrzeit nach einer Dauer"), correct, wrong, "Uhrzeit",
                "Erst die Stunden, dann die Minuten dazu: Über 60 Minuten wird eine Stunde mehr.", "Bis zur vollen Stunde, dann weiter", r,
            )
        } else {
            val h1 = r.between(18, 23)
            val m1 = r.between(0, 11) * 5
            val h2 = r.between(5, 9)
            val m2 = r.between(0, 11) * 5
            val total = (24 * 60 - (h1 * 60 + m1)) + h2 * 60 + m2
            val toMid = 24 * 60 - (h1 * 60 + m1)
            ExtraKit.int(
                id, "night:$h1:$m1-$h2:$m2", Prompt("$h1:${"%02d".format(m1)} bis $h2:${"%02d".format(m2)} = ? min", caption = "Über Mitternacht (z. B. Schlaf)"), total.toLong(),
                listOf(step("1.440 $MINUS ${n((h1 * 60 + m1).toLong())}", toMid.toLong(), "Minuten bis Mitternacht (ein Tag hat 1.440 min)."), step("${n(toMid.toLong())} + ${n((h2 * 60 + m2).toLong())}", total.toLong(), "Dazu die Minuten nach Mitternacht.")),
                "Bis Mitternacht, dann weiter",
            )
        }
    }

    private fun split(id: String, r: Random): Problem {
        val bill = bd(r.between(20, 200).toLong())
        val tip = r.pick(listOf(10L, 10L, 5L, 15L, 20L))
        val people = r.pick(listOf(2L, 4L, 5L))
        val tipAmount = bill.multiply(bd(tip)).divide(bd(100)).stripTrailingZeros()
        val total = bill + tipAmount
        val each = total.divide(bd(people), MathContext.DECIMAL64).stripTrailingZeros()
        if (each.scale() > 2) return split(id, Random(r.nextLong()))
        return ExtraKit.dec(
            id, "split:$bill+$tip/$people", Prompt("${d(bill)} € + $tip % Trinkgeld, durch $people = ? €", caption = "Rechnung teilen"), each,
            listOf(ds("${d(bill)} $X $tip $DIV 100", tipAmount, "Das Trinkgeld."), ds("${d(bill)} + ${d(tipAmount)}", total, "Gesamtbetrag."), ds("${d(total)} $DIV $people", each, "Auf alle verteilen.")),
            "Erst Trinkgeld, dann teilen",
            listOf(mistake(bill.divide(bd(people), MathContext.DECIMAL64), "Trinkgeld vergessen.")),
        )
    }

    private fun unitPrice(id: String, r: Random): Problem {
        val g = r.pick(listOf(125L, 200L, 250L, 400L, 500L, 750L, 800L))
        val perKg = BigDecimal(r.pick(listOf("2", "2.4", "3.2", "4", "4.8", "6", "8", "9.6", "12", "16")))
        val price = perKg.multiply(bd(g)).divide(bd(1000)).stripTrailingZeros()
        if (price.scale() > 2) return unitPrice(id, Random(r.nextLong()))
        val thousand = price.multiply(bd(1000)).stripTrailingZeros()
        return ExtraKit.dec(
            id, "price:$g@$price", Prompt("${n(g)} g für ${euro(price)} € = ? € pro kg", caption = "Grundpreis vergleichen"), perKg,
            listOf(ds("${d(price)} $X 1.000", thousand, "Preis für 1.000 Packungsgramm …"), ds("${d(thousand)} $DIV ${n(g)}", perKg, "… geteilt durch das Gewicht: Preis pro Kilogramm.")),
            "Preis × 1.000 ÷ Gramm",
        )
    }

    private val skillList by lazy {
        val b = book
        listOf(
            ExtraKit.skill(
                "w16.neg", "Negative Zahlen", "Zahlenstrahl",
                "Plus und Minus über die Null hinweg.",
                "Negative Zahlen begegnen dir bei Temperaturen, Kontoständen oder Höhenangaben. Stell dir einen Zahlenstrahl vor: Plus heißt nach rechts gehen, Minus nach links. Wer bei −4 steht und 9 nach rechts geht, landet bei 5.",
                listOf("Startpunkt auf dem Zahlenstrahl suchen.", "Bei Plus nach rechts, bei Minus nach links gehen.", "Schritt für Schritt von links nach rechts rechnen."),
                listOf("−4 + 9 = 5 · 3 − 8 = −5 · −2 − 6 = −8"),
                "Zwei Minuszeichen hintereinander (− (−3)) werden zu Plus.",
                ExtraKit.quick, b, 1601, ::negative,
            ),
            ExtraKit.skill(
                "w16.vat", "Mehrwertsteuer", "Netto × 1,19",
                "Von netto zu brutto und zurück – mit 19 % und 7 %.",
                "Der Bruttopreis ist der Nettopreis plus Mehrwertsteuer, in Deutschland meist 19 % (Lebensmittel, Bücher: 7 %). Brutto sind also 119 % des Nettopreises. Zurück geht es nur durch Teilen durch 1,19 – nicht durch Abziehen von 19 %.",
                listOf("Netto → brutto: Steuer ausrechnen und dazuzählen (oder gleich × 1,19).", "Brutto → netto: durch 1,19 (bzw. 1,07) teilen."),
                listOf("Brutto = Netto × 1,19", "Netto = Brutto ÷ 1,19"),
                "119 € brutto sind genau 100 € netto – daran erkennst du, ob du richtig herum rechnest.",
                ExtraKit.medium, b, 1602, ::vat,
            ),
            ExtraKit.skill(
                "w16.discount", "Rabatt und Rabatt auf Rabatt", "Restanteile malnehmen",
                "Preise nach Rabatt und zwei Rabatte hintereinander.",
                "Ein Rabatt von 25 % heißt: Du zahlst noch 75 %. Kommen zwei Rabatte hintereinander, gilt der zweite nur noch für den schon reduzierten Preis. Darum nimmst du die Restanteile mal: 80 % von 90 % sind 72 % – zusammen also 28 % Rabatt, nicht 30 %.",
                listOf("Rabatt in Euro ausrechnen und abziehen – oder gleich mit dem Restanteil malnehmen.", "Bei zwei Rabatten: Restanteile malnehmen.", "Der Rest zu 100 % ist der Gesamtrabatt."),
                listOf("Neuer Preis = Preis × (1 − Rabatt)", "20 % + 10 % Rabatt = 1 − 0,8 × 0,9 = 28 %"),
                "Zwei Rabatte sind immer etwas weniger als ihre Summe.",
                ExtraKit.medium, b, 1603, ::discount,
            ),
            ExtraKit.skill(
                "w16.interest", "Zinsen und Zinseszins", "Jedes Jahr × (1 + p)",
                "Jahreszinsen und wie Geld mit Zinseszins wächst.",
                "Zinsen für ein Jahr sind Kapital mal Zinssatz. Beim Zinseszins kommen die Zinsen jedes Jahr zum Kapital dazu und werden im nächsten Jahr mitverzinst – darum wächst das Geld jedes Jahr mit demselben Faktor, z. B. × 1,1 bei 10 %.",
                listOf("Zinssatz in einen Faktor verwandeln: 10 % → 1,1; 5 % → 1,05.", "Für jedes Jahr einmal mit dem Faktor malnehmen.", "Zum Überschlagen: Bei p % verdoppelt sich das Geld nach etwa 72 ÷ p Jahren."),
                listOf("Zinsen = Kapital × p ÷ 100", "Endkapital = Kapital × (1 + p/100)ⁿ"),
                "Zinseszins ist immer etwas mehr als einfache Zinsen – im ersten Jahr sind beide gleich.",
                ExtraKit.medium, b, 1604, ::interest,
            ),
            ExtraKit.skill(
                "w16.fuel", "Verbrauch und Tanken", "Strecke ÷ 100 × Verbrauch",
                "Liter pro 100 km, Spritkosten und Verbrauch ausrechnen.",
                "Der Verbrauch eines Autos wird in Litern pro 100 Kilometer angegeben. Für eine Strecke zählst du, wie viele 100-km-Stücke es sind, und nimmst mit dem Verbrauch mal. Umgekehrt teilst du die getankten Liter durch die Anzahl der 100-km-Stücke.",
                listOf("Strecke durch 100 teilen.", "Mit dem Verbrauch malnehmen – das sind die Liter.", "Für die Kosten: Liter mal Preis pro Liter."),
                listOf("Liter = km ÷ 100 × Verbrauch", "Verbrauch = Liter ÷ (km ÷ 100)"),
                "Bei Strom gilt dasselbe mit kWh pro 100 km.",
                ExtraKit.medium, b, 1605, ::fuel,
            ),
            ExtraKit.skill(
                "w16.recipe", "Rezepte umrechnen", "Dreisatz über eine Person",
                "Mengen von einer Personenzahl auf eine andere umrechnen.",
                "Rezepte sind ein Dreisatz: Rechne zuerst aus, wie viel eine Person braucht, und nimm das dann mit der neuen Personenzahl mal. Oft geht es noch schneller über ein einfaches Verhältnis (von 4 auf 6 Personen ist das 1,5-Fache).",
                listOf("Menge durch die alte Personenzahl teilen.", "Mit der neuen Personenzahl malnehmen."),
                listOf("neue Menge = alte Menge ÷ alte Personen × neue Personen"),
                "Passt das Verhältnis glatt (2 → 6 = dreifach), spare dir den Weg über eine Person.",
                ExtraKit.quick, b, 1606, ::recipe,
            ),
            ExtraKit.skill(
                "w16.clock", "Uhrzeiten und Dauer", "Bis zur vollen Stunde",
                "Wann bin ich fertig? Wie lange habe ich geschlafen?",
                "Bei Uhrzeiten rechnest du in Stunden zu 60 Minuten. Am sichersten geht es bis zur nächsten vollen Stunde, dann in ganzen Stunden weiter und zum Schluss mit den restlichen Minuten. Über Mitternacht rechnest du bis 24:00 und dann weiter.",
                listOf("Erst bis zur vollen Stunde (oder bis Mitternacht).", "Dann ganze Stunden.", "Zum Schluss die Restminuten."),
                listOf("1 Tag = 24 h = 1.440 min"),
                "Minuten über 60 werden zu einer Stunde mehr: 9:50 + 25 min = 10:15.",
                ExtraKit.medium, b, 1607, ::clock,
            ),
            ExtraKit.skill(
                "w16.split", "Rechnung teilen", "Erst Trinkgeld, dann teilen",
                "Restaurantrechnung mit Trinkgeld gerecht aufteilen.",
                "Beim Teilen einer Rechnung rechnest du zuerst das Trinkgeld aus, zählst es dazu und teilst dann durch die Anzahl der Personen. 10 % Trinkgeld heißt: Komma eine Stelle nach links.",
                listOf("Trinkgeld ausrechnen (10 % = Komma verschieben).", "Zur Rechnung addieren.", "Durch die Personenzahl teilen."),
                listOf("pro Person = Rechnung × (1 + Trinkgeld) ÷ Personen"),
                "Statt erst zu addieren kannst du auch mit 1,1 malnehmen (bei 10 % Trinkgeld).",
                ExtraKit.medium, b, 1608, ::split,
            ),
            ExtraKit.skill(
                "w16.price", "Grundpreis vergleichen", "Preis pro Kilogramm",
                "Welche Packung ist günstiger? Preis pro kg ausrechnen.",
                "Um Packungen zu vergleichen, rechnest du den Preis pro Kilogramm aus: Preis geteilt durch Gramm mal 1.000. Oft geht es leichter über einfache Anteile: 250 g sind ein Viertel Kilo, also mal 4.",
                listOf("Wie oft passt die Packung in 1 kg (1.000 g)?", "Den Preis entsprechend malnehmen."),
                listOf("Preis pro kg = Preis ÷ Gramm × 1.000", "250 g → × 4 · 500 g → × 2 · 750 g → ÷ 3 × 4"),
                "Achte im Laden auf das Kleingedruckte am Preisschild – dort steht der Grundpreis schon.",
                ExtraKit.medium, b, 1609, ::unitPrice,
            ),
        )
    }

    private val mixed by lazy {
        ExtraKit.skill(
            "w16.mixed", "Alltag gemischt", "Situation erkennen",
            "Alle Alltagsaufgaben durcheinander.",
            "Hier kommen alle Alltagssituationen durcheinander – wie im echten Leben.",
            listOf("Was ist gefragt: Preis, Menge, Zeit, Prozent?", "Den passenden Weg wählen.", "Ergebnis überschlagen: Ist es realistisch?"),
            emptyList(),
            "Im Alltag reicht oft ein gutes Überschlagen – hier übst du das genaue Rechnen.",
            ExtraKit.medium, book, 1610, ExtraKit.mixedOf { skillList },
        )
    }

    val world by lazy {
        World(id = "w16", number = 16, title = "Mathe im Alltag", subtitle = "Ergänzung · Geld, Zeit, Mengen", skills = skillList + mixed, independent = true)
    }
}
