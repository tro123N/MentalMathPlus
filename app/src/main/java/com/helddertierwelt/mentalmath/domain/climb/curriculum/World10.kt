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

import com.helddertierwelt.mentalmath.domain.climb.math.Phonetic
import com.helddertierwelt.mentalmath.domain.climb.model.Block
import com.helddertierwelt.mentalmath.domain.climb.model.BookRef
import com.helddertierwelt.mentalmath.domain.climb.model.Expected
import com.helddertierwelt.mentalmath.domain.climb.model.Goals
import com.helddertierwelt.mentalmath.domain.climb.model.InputKind
import com.helddertierwelt.mentalmath.domain.climb.model.Problem
import com.helddertierwelt.mentalmath.domain.climb.model.Prompt
import com.helddertierwelt.mentalmath.domain.climb.model.Skill
import com.helddertierwelt.mentalmath.domain.climb.model.Step
import com.helddertierwelt.mentalmath.domain.climb.model.World
import kotlin.random.Random

/** Welt 10 – Zahlen merken mit dem phonetischen Code (7. Kapitel). */
internal object World10 {

    private const val CH = 7

    /** Eigene Eselsbrücken für die Ziffern. */
    private val aids = listOf(
        "0 = z, s: z wie „zero“, das englische Wort für null.",
        "1 = t, d: Diese Buchstaben haben einen einzigen Abstrich – wie die 1.",
        "2 = n: Das kleine n steht auf zwei Beinen.",
        "3 = m: Das kleine m steht auf drei Beinen.",
        "4 = r: „vier“ endet auf r.",
        "5 = l: L ist die römische 50.",
        "6 = j, ch, sch: Ein großes J, gespiegelt, sieht fast aus wie eine 6.",
        "7 = k, g: Aus einer 7 und einer gespiegelten 7 lässt sich ein K legen.",
        "8 = f, v: Ein schreibschriftliches f hat oben und unten eine Schlaufe – wie die 8.",
        "9 = p, b: Eine 9 gespiegelt ergibt ein P, auf den Kopf gestellt ein b.",
    )

    private val soundChoices = Phonetic.sounds

    // --- Code lernen -------------------------------------------------------------------------

    fun digitForSound(digit: Int, soundIndex: Int): Problem {
        val options = soundChoices[digit].split(", ")
        val sound = options[soundIndex % options.size]
        return intProblem(
            skillId = "w10.code",
            key = "s$sound",
            prompt = Prompt("„$sound“", caption = "Welche Ziffer steht für diesen Laut?"),
            value = digit.toLong(),
            steps = listOf(step("Ziffer für „$sound“", digit.toLong(), aids[digit])),
            method = "Phonetischer Code",
        )
    }

    fun soundForDigit(digit: Int, r: Random): Problem {
        val others = (0..9).filter { it != digit }.shuffled(r).take(3)
        val options = (others + digit).shuffled(r)
        val choices = options.map { soundChoices[it] }
        val index = options.indexOf(digit)
        return Problem(
            skillId = "w10.code",
            key = "d$digit",
            prompt = Prompt("$digit", caption = "Welche Laute gehören zu dieser Ziffer?"),
            expected = Expected.Choice(index),
            input = InputKind.CHOICE,
            steps = listOf(Step("Laute für $digit", Expected.Choice(index), InputKind.CHOICE, aids[digit])),
            method = "Phonetischer Code",
            choices = choices,
        )
    }

    private val code = Skill(
        id = "w10.code",
        title = "Der Ziffern-Laut-Code",
        method = "Jede Ziffer hat einen Laut",
        book = BookRef(CH, "Der phonetische Code"),
        summary = "Die zehn Laute lernen, mit denen sich Zahlen in Wörter verwandeln.",
        explanation = listOf(
            Block.Text("Ziffern kann sich kaum jemand lange merken – Wörter und Bilder dagegen schon. Der phonetische Code ordnet deshalb jeder Ziffer einen Konsonanten-Laut zu. Vokale (a, e, i, o, u, ä, ö, ü) sowie h, w und y zählen nicht; sie dürfen beliebig eingefügt werden."),
            Block.Formula("1 t, d    2 n    3 m    4 r    5 l\n6 j, ch, sch    7 k, g    8 f, v    9 p, b    0 z, s"),
            Block.Steps(aids),
            Block.Tip("Es zählt der Laut, nicht der Buchstabe: Doppelte Buchstaben wie ll oder tt sind ein einziger Laut, ck ist ein k und ph ein f."),
        ),
        goals = Goals(testSize = 10, star1 = 6.0, star2 = 4.0, star3 = 2.5),
        generator = { r ->
            val digit = r.nextInt(10)
            if (r.nextBoolean()) digitForSound(digit, r.nextInt(3)) else soundForDigit(digit, r)
        },
    )

    // --- Wörter entschlüsseln ----------------------------------------------------------------

    fun decode(word: String): Problem {
        val parts = Phonetic.parts(word) ?: error("Wort $word lässt sich nicht eindeutig übersetzen")
        val digits = parts.joinToString("") { it.second.toString() }
        val steps = parts.mapIndexed { i, (letters, digit) ->
            step("Laut ${i + 1}: „$letters“", digit.toLong(), aids[digit])
        } + Step("Zahl in „$word“", Expected.Digits(digits), InputKind.DIGITS, "Die Ziffern der Reihe nach: ${Phonetic.explain(word)}.")
        return Problem(
            skillId = "w10.decode",
            key = "w$word",
            prompt = Prompt(word, caption = "Welche Zahl steckt in diesem Wort?"),
            expected = Expected.Digits(digits),
            input = InputKind.DIGITS,
            steps = steps,
            method = "Laute in Ziffern übersetzen",
        )
    }

    private val decodeSkill = Skill(
        id = "w10.decode",
        title = "Wörter in Zahlen verwandeln",
        method = "Laute in Ziffern übersetzen",
        book = BookRef(CH, "Der phonetische Code"),
        summary = "Jedes Wort eindeutig in seine Zahl zurückübersetzen.",
        explanation = listOf(
            Block.Text("Jedes Wort lässt sich in genau eine Zahl übersetzen: Du gehst die Laute der Reihe nach durch und schreibst für jeden Konsonanten-Laut seine Ziffer auf. Genau diese Eindeutigkeit macht das System so wertvoll – ein gemerktes Wort führt immer zur richtigen Zahl zurück."),
            Block.Example(decode("Kamel")),
            Block.Example(decode("Teppich")),
            Block.Tip("Ein s oder z am Anfang ergibt eine führende 0 – so lassen sich auch Telefonnummern wie 0151 … merken."),
        ),
        goals = Goals(testSize = 10, star1 = 10.0, star2 = 6.0, star3 = 4.0),
        generator = { r ->
            val word = if (r.nextInt(3) == 0) Phonetic.pegs[r.between(10, 99)] else r.pick(Phonetic.longWords)
            decode(word)
        },
    )

    // --- Zahlen verschlüsseln ----------------------------------------------------------------

    private val allWords: List<String> = (Phonetic.pegs.drop(10) + Phonetic.longWords)

    fun encode(word: String, r: Random): Problem {
        val code = Phonetic.code(word)!!
        val distractors = allWords
            .filter { Phonetic.code(it) != code }
            .map { other ->
                val c = Phonetic.code(other)!!
                val sameLength = if (c.length == code.length) 0 else 10
                val sharedStart = if (c.firstOrNull() == code.firstOrNull()) 0 else 3
                other to sameLength + sharedStart + r.nextInt(4)
            }
            .sortedBy { it.second }
            .map { it.first }
            .distinctBy { Phonetic.code(it) }
            .take(3)
        val options = (distractors + word).shuffled(r)
        val index = options.indexOf(word)
        val sounds = code.map { Phonetic.sounds[it - '0'] }
        return Problem(
            skillId = "w10.encode",
            key = "e$code",
            prompt = Prompt(code, caption = "Welches Wort steht für diese Zahl?"),
            expected = Expected.Choice(index),
            input = InputKind.CHOICE,
            steps = listOf(
                Step(
                    "Wort für $code",
                    Expected.Choice(index),
                    InputKind.CHOICE,
                    "Die Laute: " + code.mapIndexed { i, ch -> "$ch = ${sounds[i]}" }.joinToString(" · ") + " – das passt zu „$word“ (${Phonetic.explain(word)}).",
                ),
            ),
            method = "Ziffern in Laute übersetzen",
            choices = options,
        )
    }

    private val encodeSkill = Skill(
        id = "w10.encode",
        title = "Zahlen in Wörter verwandeln",
        method = "Ziffern in Laute übersetzen",
        book = BookRef(CH, "Der phonetische Code"),
        summary = "Zu einer Zahl das passende Wort erkennen.",
        explanation = listOf(
            Block.Text("Umgekehrt geht es genauso: Übersetze jede Ziffer in einen ihrer Laute und füge Vokale ein, bis ein Wort entsteht. Zu einer Zahl passen oft viele Wörter – nimm das, was du dir am besten bildlich vorstellen kannst."),
            Block.Steps(
                listOf(
                    "Zahl 795: 7 = k/g, 9 = p/b, 5 = l.",
                    "Mit Vokalen auffüllen: G-a-b-e-l → Gabel. Oder K-a-b-e-l → Kabel.",
                    "Beide Wörter führen sicher zu 795 zurück.",
                ),
            ),
            Block.Tip("Prüfe dein Wort immer rückwärts: Ergibt es wirklich wieder die Zahl? Dann kann nichts schiefgehen."),
        ),
        goals = Goals(testSize = 10, star1 = 10.0, star2 = 6.5, star3 = 4.0),
        generator = { r -> encode(r.pick(allWords), r) },
    )

    // --- Merkwörter 0–99 ---------------------------------------------------------------------

    fun pegWord(number: Int, r: Random): Problem {
        val near = listOf(number + 1, number - 1, number + 10, number - 10, (number % 10) * 10 + number / 10)
            .filter { it in 0..99 && it != number }
            .distinct()
            .shuffled(r)
            .take(3)
        val options = (near.map { Phonetic.pegs[it] } + Phonetic.pegs[number]).shuffled(r)
        val index = options.indexOf(Phonetic.pegs[number])
        val label = if (number < 10) "$number" else "$number"
        return Problem(
            skillId = "w10.pegs",
            key = "p$number",
            prompt = Prompt(label, caption = "Welches Merkwort gehört zu dieser Zahl?"),
            expected = Expected.Choice(index),
            input = InputKind.CHOICE,
            steps = listOf(Step("Merkwort für $number", Expected.Choice(index), InputKind.CHOICE, "${Phonetic.pegs[number]}: ${Phonetic.explain(Phonetic.pegs[number])}.")),
            method = "Merkwörter",
            choices = options,
        )
    }

    fun pegNumber(number: Int): Problem {
        val word = Phonetic.pegs[number]
        return intProblem(
            skillId = "w10.pegs",
            key = "n$number",
            prompt = Prompt(word, caption = "Für welche Zahl steht dieses Merkwort?"),
            value = number.toLong(),
            steps = listOf(step("Zahl für „$word“", number.toLong(), Phonetic.explain(word) + ".")),
            method = "Merkwörter",
        )
    }

    private val pegList: String = (0..99).chunked(10).joinToString("\n") { row ->
        row.joinToString("  ") { "$it ${Phonetic.pegs[it]}" }
    }

    private val pegsSkill = Skill(
        id = "w10.pegs",
        title = "Merkwörter von 0 bis 99",
        method = "Ein festes Wort pro Zahl",
        book = BookRef(CH, "Der phonetische Code"),
        summary = "Für jede Zahl bis 99 sofort ein festes Bild im Kopf haben.",
        explanation = listOf(
            Block.Text("Am schnellsten wird der Code, wenn du für jede Zahl von 0 bis 99 ein festes Merkwort hast. Dann musst du nicht jedes Mal neu überlegen, sondern siehst bei 47 sofort ein Bild vor dir. Die Liste hier ist ein Vorschlag – eigene Wörter, die du dir besser vorstellen kannst, sind genauso gut, solange sie zum Code passen."),
            Block.Formula(pegList),
            Block.Tip("Lerne die Liste in Zehnerblöcken: erst 10 bis 19 (alle beginnen mit t oder d), dann 20 bis 29 (alle mit n) und so weiter."),
        ),
        goals = Goals(testSize = 10, star1 = 7.0, star2 = 4.5, star3 = 3.0),
        generator = { r ->
            val number = r.nextInt(100)
            if (r.nextBoolean()) pegWord(number, r) else pegNumber(number)
        },
    )

    // --- Lange Zahlen merken -----------------------------------------------------------------

    fun memorize(digits: String): Problem {
        val pairs = digits.chunked(2)
        val words = pairs.map { Phonetic.wordFor(it) }
        val steps = pairs.mapIndexed { i, pair ->
            Step("Paar ${i + 1}", Expected.Digits(pair), InputKind.DIGITS, "Merkwort: ${words[i]}.")
        } + Step("Die ganze Zahl", Expected.Digits(digits), InputKind.DIGITS, "Alle Paare hintereinander: ${pairs.joinToString(" ")}.")
        return Problem(
            skillId = "w10.memory",
            key = "m$digits",
            prompt = Prompt(
                text = "Merken!",
                caption = "Einprägen – sobald du tippst, verschwindet die Zahl",
                lines = listOf(pairs.joinToString(" "), words.joinToString(" · ")),
                hideAfterMs = 4_000L + 2_000L * pairs.size,
            ),
            expected = Expected.Digits(digits),
            input = InputKind.DIGITS,
            steps = steps,
            method = "Paare in Merkwörter verwandeln",
        )
    }

    private val memory = Skill(
        id = "w10.memory",
        title = "Lange Zahlen merken",
        method = "Paare in Merkwörter verwandeln",
        book = BookRef(CH, "Gedächtnis-Magie"),
        summary = "Acht Ziffern und mehr sicher im Kopf behalten.",
        explanation = listOf(
            Block.Text("Ohne Hilfe behält man etwa sieben bis acht Ziffern. Mit dem Code werden daraus Wörter – und Wörter lassen sich zu einer kleinen, gern verrückten Geschichte verbinden. Je lustiger das Bild, desto besser bleibt es hängen."),
            Block.Steps(
                listOf(
                    "Die Zahl in Paare teilen: 47 29 15 85.",
                    "Jedes Paar in sein Merkwort verwandeln: Riege, Neubau, Teil, Falle.",
                    "Eine Geschichte bauen: Eine Turner-Riege turnt im Neubau, ein Teil fällt in die Falle.",
                    "Zum Abrufen die Geschichte durchgehen und jedes Wort zurückübersetzen.",
                ),
            ),
            Block.Example(memorize("47291585")),
            Block.Tip("Beim Kopfrechnen hilft der Code, Zwischenergebnisse festzuhalten: 115.200 wird zu einem Wort für 115 – die 200 kannst du dir mit zwei Fingern merken."),
        ),
        goals = Goals(testSize = 5, minAccuracy = 0.8, star1 = 30.0, star2 = 22.0, star3 = 15.0),
        guidedCount = 3,
        practiceCount = 6,
        generator = { r ->
            val length = r.pick(listOf(8, 8, 10))
            memorize((1..length).map { r.nextInt(10) }.joinToString(""))
        },
    )

    val world = World(
        id = "w10",
        number = 10,
        title = "Zahlen merken",
        subtitle = "Kapitel 7 – Ein erinnerungswürdiges Kapitel",
        skills = listOf(code, decodeSkill, encodeSkill, pegsSkill, memory),
    )
}
