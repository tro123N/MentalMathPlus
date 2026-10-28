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

package com.helddertierwelt.mentalmath.domain.climb.ai

/** Aufbereitung der Antworten des Modells für die Anzeige. */
object AiText {

    /** Steuerzeichen des Modells, die nie angezeigt werden sollen. */
    private val specialTokens = Regex(
        "<\\|[^<>|\\n]{0,40}\\|>|<\\|[a-z_]{2,40}>|<[a-z_]{2,40}\\|>|</?(?:end_of_turn|start_of_turn|eos|bos|pad|end_of_text)>",
    )

    /** LaTeX-Befehle (ohne Backslash) und ihre Zeichen. */
    private val commands = mapOf(
        "cdot" to "·", "times" to "×", "div" to ":", "approx" to "≈", "neq" to "≠", "ne" to "≠",
        "leq" to "≤", "le" to "≤", "geq" to "≥", "ge" to "≥", "pm" to "±", "rightarrow" to "→",
        "Rightarrow" to "⇒", "implies" to "⇒", "Leftrightarrow" to "⇔", "iff" to "⇔", "to" to "→", "infty" to "∞",
        "ldots" to "…", "dots" to "…", "cdots" to "…", "euro" to "€", "degree" to "°", "circ" to "°",
        "quad" to " ", "qquad" to " ", "left" to "", "right" to "", "displaystyle" to "", "mid" to "|",
        "equiv" to "≡", "checkmark" to "✓", "uparrow" to "↑", "downarrow" to "↓", "cdotp" to "·", "ast" to "*",
        "%" to "%", "," to " ", ";" to " ", ":" to " ", "!" to "", " " to " ", "{" to "{", "}" to "}", "$" to "$",
    )

    private val superscripts = mapOf('0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹', '-' to '⁻')

    /** Antwort des Modells säubern: Steuerzeichen entfernen, LaTeX in lesbare Zeichen umwandeln. */
    fun clean(raw: String): String = try {
        cleanUnsafe(raw)
    } catch (e: Exception) {
        // Lieber den Rohtext zeigen, als beim Anzeigen abzustürzen.
        raw
    }

    private fun cleanUnsafe(raw: String): String {
        var s = specialTokens.replace(raw, "")
        s = s.replace("\r", "")
        s = mathSegments(s)
        s = latex(s)
        s = Regex("(?<=\\S) {2,}").replace(s, " ")
        s = Regex("\n{3,}").replace(s, "\n\n")
        return s.trim()
    }

    /** Text in Mathe-Klammern ($…$, \(…\), \[…\]) ohne Klammern und geschweifte Klammern übernehmen. */
    private fun mathSegments(text: String): String {
        var s = text
        s = Regex("\\\\\\[(.+?)\\\\\\]", RegexOption.DOT_MATCHES_ALL).replace(s) { "\n" + mathBody(it.groupValues[1]) + "\n" }
        s = Regex("\\\\\\((.+?)\\\\\\)", RegexOption.DOT_MATCHES_ALL).replace(s) { mathBody(it.groupValues[1]) }
        s = Regex("\\$\\$(.+?)\\$\\$", RegexOption.DOT_MATCHES_ALL).replace(s) { "\n" + mathBody(it.groupValues[1]) + "\n" }
        // Einzelne $…$ nur, wenn der Inhalt nach Formel aussieht (kein Dollarbetrag).
        s = Regex("\\$(?=\\S)([^\\$\\n]{1,200}?)(?<=\\S)\\$").replace(s) { m ->
            val body = m.groupValues[1]
            if (body.contains('\\') || body.contains('^') || body.contains('_') || body.contains('=') || body.any { it.isDigit() }) mathBody(body) else m.value
        }
        return s
    }

    private fun mathBody(body: String): String {
        val converted = latex(body.trim())
        return converted.replace("{", "").replace("}", "")
    }

    /** LaTeX-Befehle in lesbare Zeichen umwandeln (auch außerhalb von Mathe-Klammern). */
    private fun latex(text: String): String {
        if (!text.contains('\\') && !text.contains('^')) return text
        var s = text
        // \frac{a}{b} → a/b, auch verschachtelt.
        repeat(6) {
            val i = Regex("\\\\[dt]?frac\\s*\\{").find(s) ?: return@repeat
            val (a, endA) = group(s, i.range.last) ?: return@repeat
            var j = endA + 1
            while (j < s.length && s[j].isWhitespace()) j++
            if (j >= s.length || s[j] != '{') return@repeat
            val (b, endB) = group(s, j) ?: return@repeat
            val replacement = wrap(a) + "/" + wrap(b)
            s = s.substring(0, i.range.first) + replacement + s.substring(endB + 1)
        }
        // \sqrt{x} → √x
        repeat(6) {
            val i = Regex("\\\\sqrt\\s*\\{").find(s) ?: return@repeat
            val (a, end) = group(s, i.range.last) ?: return@repeat
            s = s.substring(0, i.range.first) + "√" + wrap(a) + s.substring(end + 1)
        }
        // \text{…}, \mathrm{…}, \textbf{…} → Inhalt
        s = Regex("\\\\(?:text|mathrm|textrm|mathit|operatorname|mbox)\\s*\\{([^\\{\\}]*)\\}").replace(s) { it.groupValues[1] }
        s = Regex("\\\\(?:textbf|mathbf|boldsymbol)\\s*\\{([^\\{\\}]*)\\}").replace(s) { "**" + it.groupValues[1] + "**" }
        s = Regex("\\\\boxed\\s*\\{([^\\{\\}]*)\\}").replace(s) { "**" + it.groupValues[1] + "**" }
        s = Regex("\\\\([a-zA-Z]+|[%\\{\\},;:! \\$])").replace(s) { m ->
            val name = m.groupValues[1]
            commands[name] ?: if (name.first().isLetter()) name else m.value
        }
        // Hochzahlen: x^2 → x², x^{12} → x¹², ^\circ → °
        s = Regex("\\^\\{?°\\}?").replace(s, "°")
        s = Regex("\\^\\{(-?\\d{1,3})\\}").replace(s) { m -> m.groupValues[1].map { superscripts[it] ?: it }.joinToString("") }
        s = Regex("(?<=[\\w)])\\^(-?\\d{1,2})(?!\\d)").replace(s) { m -> m.groupValues[1].map { superscripts[it] ?: it }.joinToString("") }
        return s
    }

    /** Inhalt einer geschweiften Klammer ab Position [open] (dort steht „{“) und Index der schließenden. */
    private fun group(s: String, open: Int): Pair<String, Int>? {
        if (open >= s.length || s[open] != '{') return null
        var depth = 0
        for (k in open until s.length) {
            when (s[k]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return s.substring(open + 1, k) to k
                }
            }
        }
        return null
    }

    private fun wrap(x: String): String {
        val t = x.trim()
        return if (t.length <= 1 || t.all { it.isLetterOrDigit() || it == ',' || it == '.' }) t else "($t)"
    }

    /** Text ohne Formatierung (zum Kopieren und Vorlesen). */
    fun plain(text: String): String = AiMarkdown.parse(clean(text)).joinToString("\n") { block ->
        when (block) {
            is MdBlock.Paragraph -> block.spans.text()
            is MdBlock.Heading -> block.spans.text()
            is MdBlock.Bullet -> "  ".repeat(block.level) + "• " + block.spans.text()
            is MdBlock.Numbered -> "  ".repeat(block.level) + block.number + " " + block.spans.text()
            is MdBlock.Code -> block.text
            is MdBlock.Quote -> block.spans.text()
            is MdBlock.Table -> (listOf(block.header) + block.rows).joinToString("\n") { row -> row.joinToString(" | ") { it.text() } }
            MdBlock.Rule -> "—"
        }
    }

    private fun List<MdSpan>.text(): String = joinToString("") { it.text }
}

/** Ein Abschnitt einer formatierten Antwort. */
sealed interface MdBlock {
    data class Paragraph(val spans: List<MdSpan>) : MdBlock
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Bullet(val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Numbered(val number: String, val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Quote(val spans: List<MdSpan>) : MdBlock
    data class Table(val header: List<List<MdSpan>>, val rows: List<List<List<MdSpan>>>) : MdBlock
    data object Rule : MdBlock
}

/** Ein Textstück mit Auszeichnung. */
data class MdSpan(val text: String, val bold: Boolean = false, val italic: Boolean = false, val code: Boolean = false)

/** Kleiner Markdown-Leser für die Antworten des Modells (Absätze, Listen, Tabellen, fett, kursiv, Code). */
object AiMarkdown {

    private val bullet = Regex("^(\\s*)[-*+•]\\s+(.*)$")
    private val numbered = Regex("^(\\s*)(\\d{1,3})[.)]\\s+(.*)$")
    private val heading = Regex("^(#{1,6})\\s+(.*?)\\s*#*$")
    private val rule = Regex("^\\s*([-*_])\\s*(\\1\\s*){2,}$")
    private val tableSeparator = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")

    fun parse(text: String): List<MdBlock> {
        val lines = text.replace("\r", "").split('\n')
        val blocks = mutableListOf<MdBlock>()
        val paragraph = mutableListOf<String>()

        fun flush() {
            if (paragraph.isNotEmpty()) {
                blocks += MdBlock.Paragraph(inline(paragraph.joinToString("\n")))
                paragraph.clear()
            }
        }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            when {
                trimmed.startsWith("```") -> {
                    flush()
                    val code = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        code += lines[i]
                        i++
                    }
                    blocks += MdBlock.Code(code.joinToString("\n").trimEnd())
                }
                trimmed.isEmpty() -> flush()
                trimmed.startsWith("|") && i + 1 < lines.size && tableSeparator.matches(lines[i + 1]) -> {
                    flush()
                    val header = cells(trimmed)
                    i += 2
                    val rows = mutableListOf<List<List<MdSpan>>>()
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        rows += cells(lines[i].trim())
                        i++
                    }
                    blocks += MdBlock.Table(header, rows)
                    continue
                }
                rule.matches(trimmed) && paragraph.isEmpty() -> {
                    flush()
                    blocks += MdBlock.Rule
                }
                heading.matches(trimmed) -> {
                    flush()
                    val m = heading.find(trimmed)!!
                    blocks += MdBlock.Heading(m.groupValues[1].length, inline(m.groupValues[2]))
                }
                bullet.matches(line) -> {
                    flush()
                    val m = bullet.find(line)!!
                    blocks += MdBlock.Bullet(level(m.groupValues[1]), inline(m.groupValues[2]))
                }
                numbered.matches(line) -> {
                    flush()
                    val m = numbered.find(line)!!
                    blocks += MdBlock.Numbered(m.groupValues[2] + ".", level(m.groupValues[1]), inline(m.groupValues[3]))
                }
                trimmed.startsWith(">") -> {
                    flush()
                    blocks += MdBlock.Quote(inline(trimmed.removePrefix(">").trim()))
                }
                // Eingerückte Zeilen (z. B. nach einer Formel in einer Liste) ohne Leerzeichen davor zeigen.
                else -> paragraph += line.trim()
            }
            i++
        }
        flush()
        return blocks
    }

    private fun level(indent: String): Int = (indent.replace("\t", "    ").length / 2).coerceAtMost(3)

    private fun cells(row: String): List<List<MdSpan>> =
        row.trim().removePrefix("|").removeSuffix("|").split('|').map { inline(it.trim()) }

    /** Fett (**…**), kursiv (*…*) und Code (`…`). Nicht geschlossene Zeichen werden weggelassen. */
    fun inline(text: String): List<MdSpan> {
        val spans = mutableListOf<MdSpan>()
        val sb = StringBuilder()
        var bold = false
        var italic = false
        var i = 0

        fun emit(code: Boolean = false) {
            if (sb.isNotEmpty()) {
                spans += MdSpan(sb.toString(), bold = bold, italic = italic, code = code)
                sb.clear()
            }
        }

        while (i < text.length) {
            val c = text[i]
            when {
                c == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        emit()
                        sb.append(text, i + 1, end)
                        emit(code = true)
                        i = end + 1
                        continue
                    }
                    i++
                    continue
                }
                (c == '*' || c == '_') && i + 1 < text.length && text[i + 1] == c -> {
                    val marker = "$c$c"
                    if (bold || text.indexOf(marker, i + 2) > i + 2) {
                        emit()
                        bold = !bold
                    }
                    i += 2
                    continue
                }
                c == '*' -> {
                    // Einzelnes Sternchen: kursiv, wenn es eingeschlossenen Text gibt; „3 * 4“ bleibt stehen.
                    val prevSpace = i == 0 || text[i - 1].isWhitespace()
                    val nextSpace = i + 1 >= text.length || text[i + 1].isWhitespace()
                    if (italic && !prevSpace) {
                        emit()
                        italic = false
                        i++
                        continue
                    }
                    if (!italic && !nextSpace && text.indexOf('*', i + 1).let { e -> e > i + 1 && !text[e - 1].isWhitespace() }) {
                        emit()
                        italic = true
                        i++
                        continue
                    }
                    sb.append(c)
                }
                else -> sb.append(c)
            }
            i++
        }
        emit()
        return spans.ifEmpty { listOf(MdSpan("")) }
    }
}
