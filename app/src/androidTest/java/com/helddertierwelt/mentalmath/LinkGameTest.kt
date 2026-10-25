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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.domain.game.GameState
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.presentation.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class LinkGameTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val sharedGame = GameRecord(
        gameMode = GameMode.PROBLEMS,
        modeLimit = 1,
        additionRangeStart = 1,
        additionRangeEnd = 2,
        seed = Random.nextLong(from = 0, until = Long.MAX_VALUE),
        activeScore = 10f
    )

    @Test
    fun openingALinkAsksToStartTheGame() {
        openLink(sharedGame).use {
            waitForText(R.string.app_link_game_popup_title)
        }
    }

    @Test
    fun confirmingTheLinkStartsTheSharedGame() {
        openLink(sharedGame).use { app ->
            confirmLinkPopUp()

            assertTrue(app.gameState().isGameStarted)
            assertEquals(sharedGame.seed, app.gameState().seed)
        }
    }

    @Test
    fun gameContinuesWhenTheActivityIsRecreated() {
        openLink(sharedGame).use { app ->
            confirmLinkPopUp()
            composeRule.waitUntil { app.gameState().activeTime > 500 }
            val stateBefore = app.gameState()

            // happens e.g. when switching dark mode or changing the font size
            app.recreate()

            val stateAfter = app.gameState()
            assertTrue(stateAfter.isGameStarted)
            assertEquals(stateBefore.operand1, stateAfter.operand1)
            assertEquals(stateBefore.operand2, stateAfter.operand2)
            assertEquals(stateBefore.startTimeStamp, stateAfter.startTimeStamp)
            composeRule.waitUntil { app.gameState().activeTime > stateAfter.activeTime }
        }
    }

    private fun openLink(game: GameRecord): ActivityScenario<MainActivity> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SettingsLinkUtil.createLink(game)))
            .setClass(context, MainActivity::class.java)
        return ActivityScenario.launch(intent)
    }

    private fun confirmLinkPopUp() {
        waitForText(R.string.app_link_game_popup_title)
        val confirmButton = hasText(context.getString(R.string.app_link_popup_confirmation_text)) and
                hasAnyAncestor(isDialog())
        composeRule.onNode(confirmButton).performClick()
        composeRule.waitForIdle()
    }

    private fun waitForText(textId: Int) {
        val text = context.getString(textId)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ActivityScenario<MainActivity>.gameState(): GameState {
        lateinit var state: GameState
        onActivity { activity ->
            state = ViewModelProvider(activity)[GameViewModel::class.java].gameState.value
        }
        return state
    }
}
