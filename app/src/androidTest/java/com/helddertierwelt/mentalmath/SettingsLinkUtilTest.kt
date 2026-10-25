/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025-2026 HeldDerTierwelt

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

package com.helddertierwelt.mentalmath

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil.NO_SCORE
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil.createLink
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil.deserialize
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil.isValidQueryParameter
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil.serialize
import com.helddertierwelt.mentalmath.domain.settings.SettingsState
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsLinkUtilTest {

    private val sharedGame = GameRecord(
        gameMode = GameMode.SURVIVAL,
        modeLimit = 3,
        additionRangeStart = 2,
        additionRangeEnd = 7,
        subtractionRangeStart = 1,
        subtractionRangeEnd = 8,
        multiplicationRangeStart = 3,
        multiplicationRangeEnd = 6,
        divisionRangeStart = 4,
        divisionRangeEnd = 9,
        seed = 1234567890123456789L,
        activeScore = 12.34f
    )

    @Test
    fun sharedGameIsRestoredFromItsLink() {
        val link = serialize(sharedGame)

        assertTrue(isValidQueryParameter(link))
        assertEquals(sharedGame, deserialize(link))
    }

    @Test
    fun everyGameModeSurvivesTheLink() {
        GameMode.entries.forEach { gameMode ->
            val game = sharedGame.copy(gameMode = gameMode)

            assertEquals(game, deserialize(serialize(game)))
        }
    }

    @Test
    fun linksFromOlderAppVersionsStillWork() {
        assertEquals(sharedGame, deserialize("BHtj3zwNpd0UtN_H4rQS"))
    }

    @Test
    fun linkWithoutScoreIsReadAsNewGame() {
        val newGame = deserialize("I-4_l-blkgFwAAAq")

        assertEquals(GameMode.PROBLEMS, newGame.gameMode)
        assertEquals(1, newGame.modeLimit)
        assertEquals(1, newGame.additionRangeStart)
        assertEquals(2, newGame.additionRangeEnd)
        assertEquals(42L, newGame.seed)
        assertEquals(NO_SCORE, newGame.activeScore)
    }

    @Test
    fun newGameLinkIsWrittenWithoutScore() {
        val newGame = GameRecord(
            gameMode = GameMode.PROBLEMS,
            modeLimit = 1,
            additionRangeStart = 1,
            additionRangeEnd = 2,
            seed = 42L,
            activeScore = NO_SCORE
        )

        assertEquals("I-4_l-blkgFwAAAq", serialize(newGame))
    }

    @Test
    fun newGameLinkIsCreatedFromSettings() {
        val settings = SettingsState(
            gameMode = GameMode.PROBLEMS,
            limit = 1f,
            isAdditionEnabled = true,
            additionRange = 1f to 2f,
            isSubtractionEnabled = false,
            subtractionRange = 5f to 9f
        )

        val link = createLink(settings.toNewGameRecord(seed = 42L))

        assertEquals("https://mental-math.codeberg.page/app?s=I-4_l-blkgFwAAAq", link)
    }

    @Test
    fun invalidLinksAreRejected() {
        val invalidLinks = listOf(
            "",
            "not-a-link",
            serialize(sharedGame.copy(modeLimit = 7)),
            serialize(sharedGame.copy(additionRangeStart = 8, additionRangeEnd = 2)),
            serialize(
                sharedGame.copy(
                    additionRangeStart = 0,
                    subtractionRangeStart = 0,
                    multiplicationRangeStart = 0,
                    divisionRangeStart = 0
                )
            )
        )

        invalidLinks.forEach { link ->
            assertFalse("Link should be rejected: $link", isValidQueryParameter(link))
        }
    }
}
