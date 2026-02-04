/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025 HeldDerTierwelt

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

package com.helddertierwelt.mentalmath.util

import androidx.compose.ui.graphics.Color
import com.helddertierwelt.mentalmath.R
import com.helddertierwelt.mentalmath.presentation.theme.blue
import com.helddertierwelt.mentalmath.presentation.theme.green
import com.helddertierwelt.mentalmath.presentation.theme.red
import com.helddertierwelt.mentalmath.presentation.theme.yellow

enum class GameResultType(
    val icon: Int,
    val color: Color,
    val title: String,
    val descriptions: List<String>
) {
    WAITING(
        icon = R.drawable.hourglass_bottom_24dp,
        color = blue,
        title = "Waiting for your opponent…",
        descriptions = listOf(
            "You: %.2f | Opponent: ??\nSeed: %d\n\nYou’ve set the bar! Let’s see if your opponent can catch up!",
            "You: %.2f | Opponent: ??\nSeed: %d\n\nYour score is locked in - time for your rival to respond!",
            "You: %.2f | Opponent: ??\nSeed: %d\n\nYour turn is done - the suspense begins!",
            "You: %.2f | Opponent: ??\nSeed: %d\n\nYou’ve set a solid mark. Can they top it?",
            "You: %.2f | Opponent: ??\nSeed: %d\n\nAll eyes on your opponent now!",
            "You: %.2f | Opponent: ??\nSeed: %d\n\nYour game is in - waiting for the challenger’s reply!",
            "You: %.2f | Opponent: ??\nSeed: %d\n\nYou’ve taken the lead… or have you?",
            "You: %.2f | Opponent: ??\nSeed: %d\n\nStill waiting… maybe they rage-quit already?",
            "You: %.2f | Opponent: ??\nSeed: %d\n\nYour score’s so good they’re probably uninstalling the game.",
            "You: %.2f | Opponent: ??\nSeed: %d\n\nThey saw your score… and vanished into the void."
        )
    ),
    EASY_WIN(
        icon = R.drawable.favorite_24dp,
        color = red,
        title = "Easy win",
        descriptions = listOf(
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nA win, but without real competition!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nMaybe next time you’ll get a real challenge.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nThat was over before it even started.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYour opponent didn’t show up - you win by default!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nVictory is yours… and your opponent didn’t even try.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nDid anybody even try? Because it doesn’t look like it.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYour opponent’s strategy: Not showing up. Your strategy: Winning!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYour opponent didn’t bring their A-game… or any game at all!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYou could have played blindfolded and still won.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYou won while your opponent was still figuring out the rules."
        )
    ),
    ANYONE_HERE(
        icon = R.drawable.question_mark_24dp,
        color = blue,
        title = "Anyone here?",
        descriptions = listOf(
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nNo points, no fun…",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nIt’s quiet… too quiet.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nA point-less game!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nEcho… echo… anybody playing this game?",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nIt’s not a bug - just a very quiet match.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nA whole lot of nothing happened here.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nNobody scored… but hey, at least nobody lost!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nLooks like nobody wanted to take the first step.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nThe scoreboard had an easy day today.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nBreaking news: Nothing ever happened!"
        )
    ),
    WIN(
        icon = R.drawable.trophy_24dp,
        color = yellow,
        title = "You won",
        descriptions = listOf(
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nThat was some serious mental math!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nVictory looks good on you!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nPerfect execution - impressive!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYour brain just did a mic drop.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYour mental math is basically magic.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYour opponent might need a calculator next time!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nMath skills activated - opponent defeated!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nAnother win! Your opponent might want a tutorial.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nResults don’t lie - you nailed it.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nFacts over feelings: You crushed it!",
        )
    ),
    CLOSE_WIN(
        icon = R.drawable.trophy_24dp,
        color = yellow,
        title = "Close call",
        descriptions = listOf(
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nA tight win, but it counts!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nPhew… that was close!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nNarrow win! Heart still racing?",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nVictory just in time - nice save!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nThat was tight! Good thing you didn’t blink.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nClose call! But victory still tastes delicious.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nGood one from your opponent, but the scoreboard doesn’t lie!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nThat was a cliffhanger… ending with your name on top!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nRespect to your opponent, but a win is a win.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nProps to your rival, but you prevailed.",
        )
    ),
    DRAW(
        icon = R.drawable.partner_heart_24dp,
        color = green,
        title = "Draw",
        descriptions = listOf(
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nA perfect tie - time for a decider!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nNo winner, no loser - just champions.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nEqual forces! Time for a thrilling decider.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nNo one wants to lose. Time to go for the ultimate showdown.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nThe scoreboard can’t decide a winner.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nWho’s brave enough for a rematch?",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nEvenly matched! A tie that calls for a rematch.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nTime for a rematch to settle this once and for all.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nOnly a new game can declare the real winner.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nIt’s a tie! Let’s see who can handle the pressure next round.",
        )
    ),
    CLOSE_DEFEAT(
        icon = R.drawable.rainy_24dp,
        color = blue,
        title = "Close call",
        descriptions = listOf(
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nSo close! You almost had it!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nA narrow loss - but still a strong effort!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYour opponent just got lucky this time.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYou almost had it! Don’t worry, revenge is sweet.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nDon’t let your opponent enjoy that victory for too long.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nAlmost a win! Keep practicing, success is near.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nTight loss, but you handled it well - keep it up!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nKeep on pushing - victory is just around the corner.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nSo close! Keep going, your skills are undeniable.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nClose, but not over - the next round is yours to take!",
        )
    ),
    DEFEAT(
        icon = R.drawable.rainy_24dp,
        color = blue,
        title = "Defeat",
        descriptions = listOf(
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nShake it off and give it another try!",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nIt happens! Learn, adapt, and come back stronger.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nEvery champion has been here. Now it’s your turn to rise.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYour opponent won today, but the story’s far from over.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYou win some, you lose some. That’s life.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nNo big deal - there’s more to life.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nSure, your opponent won. But let’s not make it a life achievement.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nLet your rival enjoy the win. Everyone needs their moment.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nYour opponent is probably telling everyone already. Adorable.",
            "You: %.2f | Opponent: %.2f\nSeed: %d\n\nLet your opponent enjoy these fifteen seconds of fame.",
        )
    );

    fun getRandomDescription(currentScore: Float, linkScore: Float, seed: Long): String {
        val template = descriptions.random()
        if (linkScore < 0.0f) {
            return template.format(currentScore, seed)
        }
        return template.format(currentScore, linkScore, seed)
    }
}