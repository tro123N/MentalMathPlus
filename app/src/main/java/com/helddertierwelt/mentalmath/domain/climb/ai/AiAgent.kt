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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Eine Werkzeug-Anfrage des Modells. */
data class LlmToolCall(val name: String, val argsJson: String)

/** Ein Stück der gestreamten Antwort des Modells. */
sealed interface LlmChunk {
    data class Text(val text: String) : LlmChunk
    data class Thought(val text: String) : LlmChunk
    data class ToolCalls(val calls: List<LlmToolCall>) : LlmChunk
}

/** Was an das Modell geht: Text der Person oder die Ergebnisse der Werkzeuge. */
sealed interface LlmInput {
    data class User(val text: String) : LlmInput

    /** Name des Werkzeugs und Ergebnis als JSON. */
    data class ToolResults(val results: List<Pair<String, String>>) : LlmInput
}

/** Ein laufendes Gespräch mit dem Modell (LiteRT-LM auf dem Handy oder eine Attrappe für Tests). */
interface LlmSession {
    /** Nachricht schicken; die Antwort kommt Stück für Stück. */
    fun send(input: LlmInput, thinking: Boolean): Flow<LlmChunk>

    /** Laufende Antwort abbrechen. */
    fun cancel()

    /** So viele Tokens belegt das Gespräch schon (-1 = unbekannt). */
    fun usedTokens(): Int = -1
}

/** Was während einer Antwort passiert – für die Oberfläche. */
sealed interface AgentEvent {
    data class Text(val text: String) : AgentEvent
    data class Thought(val text: String) : AgentEvent
    data class ToolStarted(val name: String) : AgentEvent
    data class ToolFinished(val outcome: ToolOutcome) : AgentEvent
}

/**
 * Der Ablauf einer Antwort: Frage an das Modell, Werkzeuge ausführen, Ergebnisse zurückgeben –
 * so lange, bis das Modell ohne Werkzeug antwortet.
 */
object AiAgent {

    /** So viele Runden mit Werkzeugen sind höchstens erlaubt. */
    const val MAX_ROUNDS = 6

    /** So viele Werkzeuge werden pro Runde höchstens ausgeführt. */
    const val MAX_CALLS_PER_ROUND = 4

    const val TOO_MANY_TOOLS = "Ich habe mich beim Nachschlagen verheddert. Frag mich bitte noch einmal, gern etwas genauer."

    /**
     * Antwort auf [prompt]. [context] liefert den aktuellen Stand der App; er wird vor jedem
     * Werkzeug neu abgefragt, damit z. B. eine gerade gestellte Aufgabe schon bekannt ist.
     */
    fun answer(session: LlmSession, prompt: String, thinking: Boolean, context: () -> AiContext): Flow<AgentEvent> = flow {
        var input: LlmInput = LlmInput.User(prompt)
        var round = 0
        val seen = mutableMapOf<String, Int>()
        // Steht schon Text da (z. B. „Ich rechne nach.“ vor einem Werkzeug), beginnt die Fortsetzung in einem neuen Absatz.
        var wroteText = false
        while (true) {
            val calls = mutableListOf<LlmToolCall>()
            var firstInRound = true
            session.send(input, thinking).collect { chunk ->
                when (chunk) {
                    is LlmChunk.Text -> if (chunk.text.isNotEmpty()) {
                        if (firstInRound && wroteText) emit(AgentEvent.Text("\n\n"))
                        firstInRound = false
                        wroteText = true
                        emit(AgentEvent.Text(chunk.text))
                    }
                    is LlmChunk.Thought -> if (chunk.text.isNotEmpty()) emit(AgentEvent.Thought(chunk.text))
                    is LlmChunk.ToolCalls -> calls += chunk.calls
                }
            }
            if (calls.isEmpty()) return@flow
            round++
            if (round > MAX_ROUNDS) {
                emit(AgentEvent.Text("\n\n$TOO_MANY_TOOLS"))
                return@flow
            }
            val results = mutableListOf<Pair<String, String>>()
            for (call in calls.take(MAX_CALLS_PER_ROUND)) {
                emit(AgentEvent.ToolStarted(call.name))
                val key = call.name + "|" + call.argsJson.filterNot { it.isWhitespace() }
                val repeats = seen.merge(key, 1, Int::plus) ?: 1
                val outcome = if (repeats > 2) {
                    ToolOutcome(
                        call.name,
                        kotlinx.serialization.json.buildJsonObject {
                            put("hinweis", kotlinx.serialization.json.JsonPrimitive("Das hast du gerade schon abgefragt. Antworte jetzt mit den vorhandenen Ergebnissen."))
                        },
                        label = "",
                        ok = false,
                    )
                } else {
                    AiToolbox.execute(call.name, call.argsJson, context())
                }
                emit(AgentEvent.ToolFinished(outcome))
                results += call.name to outcome.result.toString()
            }
            input = LlmInput.ToolResults(results)
        }
    }
}
