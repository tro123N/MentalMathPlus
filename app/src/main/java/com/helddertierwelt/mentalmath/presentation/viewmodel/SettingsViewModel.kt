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

package com.helddertierwelt.mentalmath.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.entity.GameScore
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import com.helddertierwelt.mentalmath.data.repository.SettingsRepository
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackController
import com.helddertierwelt.mentalmath.domain.feedback.FeedbackType
import com.helddertierwelt.mentalmath.domain.settings.SettingsGroup
import com.helddertierwelt.mentalmath.domain.settings.SettingsLinkUtil
import com.helddertierwelt.mentalmath.domain.settings.SettingsState
import com.helddertierwelt.mentalmath.domain.settings.enums.FormMode
import com.helddertierwelt.mentalmath.domain.settings.enums.GameMode
import com.helddertierwelt.mentalmath.domain.settings.enums.InputDirectionMode
import com.helddertierwelt.mentalmath.domain.settings.enums.InputMode
import com.helddertierwelt.mentalmath.domain.settings.enums.NotificationMode
import com.helddertierwelt.mentalmath.domain.settings.enums.SoundMode
import com.helddertierwelt.mentalmath.domain.settings.enums.ThemeMode
import com.helddertierwelt.mentalmath.domain.settings.enums.VibrationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val gameRecordRepository: GameRecordRepository,
    private val feedbackController: FeedbackController,
    ) : ViewModel() {

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState = _settingsState.asStateFlow()

    private val _isMenuOpen = MutableStateFlow(false)
    val isMenuOpen = _isMenuOpen.asStateFlow()

    private val _isScoreOpen = MutableStateFlow(false)
    val isScoreOpen = _isScoreOpen.asStateFlow()

    sealed interface PopUp {
        data object ResetApp : PopUp
        data object NotificationPermission : PopUp
    }

    private val _popUp = MutableStateFlow<PopUp?>(null)
    val popUp = _popUp.asStateFlow()

    private val _hasGamesWithScore = MutableStateFlow(false)
    val hasGamesWithScore = _hasGamesWithScore.asStateFlow()

    private val _bestGameScores = MutableStateFlow<List<GameScore>>(emptyList())
    val bestGameScores = _bestGameScores.asStateFlow()

    private val _isStartButtonClicked = MutableStateFlow(false)
    val isStartButtonClicked = _isStartButtonClicked.asStateFlow()

    private val _isRandomGameButtonClicked = MutableStateFlow(false)
    val isRandomGameButtonClicked = _isRandomGameButtonClicked.asStateFlow()

    private val _isScoreClicked = MutableStateFlow(false)
    val isScoreClicked = _isScoreClicked.asStateFlow()

    private val _isSliderActive = MutableStateFlow(false)
    val isSliderActive = _isSliderActive.asStateFlow()

    private val _expandedGroup = MutableStateFlow<SettingsGroup?>(null)
    val expandedGroup = _expandedGroup.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { loadedState ->
                _settingsState.value = loadedState
                _isLoaded.value = true
            }
        }
    }

    fun setGameMode(gameMode : GameMode) {
        playFeedback(FeedbackType.TAP)
        _settingsState.value = _settingsState.value.copy(gameMode = gameMode)
        saveState()
    }

    fun updateLimit(value: Float) {
        playFeedback(FeedbackType.SLIDER)
        _settingsState.value = _settingsState.value.copy(limit = value)
        saveState()
    }

    fun updatePlusRange(plusRange: ClosedFloatingPointRange<Float>) {
        playFeedback(FeedbackType.SLIDER)
        _settingsState.value =
            _settingsState.value.copy(additionRange = plusRange.start to plusRange.endInclusive)
        saveState()
    }

    fun updateMinusRange(minusRange: ClosedFloatingPointRange<Float>) {
        playFeedback(FeedbackType.SLIDER)
        _settingsState.value =
            _settingsState.value.copy(subtractionRange = minusRange.start to minusRange.endInclusive)
        saveState()
    }

    fun updateMultiplyRange(multiplyRange: ClosedFloatingPointRange<Float>) {
        playFeedback(FeedbackType.SLIDER)
        _settingsState.value =
            _settingsState.value.copy(multiplicationRange = multiplyRange.start to multiplyRange.endInclusive)
        saveState()
    }

    fun updateDivideRange(divideRange: ClosedFloatingPointRange<Float>) {
        playFeedback(FeedbackType.SLIDER)
        _settingsState.value =
            _settingsState.value.copy(divisionRange = divideRange.start to divideRange.endInclusive)
        saveState()
    }

    fun toggleAddition() {
        val newValue = !_settingsState.value.isAdditionEnabled
        _settingsState.value = _settingsState.value.copy(
            isAdditionEnabled = newValue
        )
        playFeedback(
            if (newValue) FeedbackType.ENABLE else FeedbackType.DISABLE
        )
        saveState()
    }

    fun toggleSubtraction() {
        val newValue = !_settingsState.value.isSubtractionEnabled
        _settingsState.value = _settingsState.value.copy(
            isSubtractionEnabled = newValue
        )
        playFeedback(
            if (newValue) FeedbackType.ENABLE else FeedbackType.DISABLE
        )
        saveState()
    }

    fun toggleMultiplication() {
        val newValue = !_settingsState.value.isMultiplicationEnabled
        _settingsState.value = _settingsState.value.copy(
            isMultiplicationEnabled = newValue
        )
        playFeedback(
            if (newValue) FeedbackType.ENABLE else FeedbackType.DISABLE
        )
        saveState()
    }

    fun toggleDivide() {
        val newValue = !_settingsState.value.isDivisionEnabled
        _settingsState.value = _settingsState.value.copy(
            isDivisionEnabled = newValue
        )
        playFeedback(
            if (newValue) FeedbackType.ENABLE else FeedbackType.DISABLE
        )
        saveState()
    }

    fun isAnyOperationEnabled(): Boolean {
        return settingsState.value.isAdditionEnabled || settingsState.value.isSubtractionEnabled || settingsState.value.isMultiplicationEnabled || settingsState.value.isDivisionEnabled
    }

    fun toggleMenuOpen() {
        if (_isScoreOpen.value) {
            _isScoreOpen.value = false
        }
        _isMenuOpen.value = !_isMenuOpen.value
    }

    fun setThemeMode(mode: ThemeMode) {
        playFeedback(FeedbackType.TAP)
        _settingsState.value = _settingsState.value.copy(themeMode = mode)
        saveState()
    }

    fun setNotificationMode(mode: NotificationMode) {
        _settingsState.value = _settingsState.value.copy(notificationMode = mode)
        saveState()
    }

    fun setSoundMode(mode: SoundMode) {
        feedbackController.setSoundMode(mode)
        _settingsState.value = _settingsState.value.copy(soundMode = mode)
        saveState()
        playFeedback(FeedbackType.TAP)
    }

    fun setVibrationMode(mode: VibrationMode) {
        feedbackController.setVibrationMode(mode)
        _settingsState.value = _settingsState.value.copy(vibrationMode = mode)
        saveState()
        playFeedback(FeedbackType.TAP)
    }

    fun showPopUp(popUp: PopUp) {
        _popUp.value = popUp
    }

    fun closePopUp() {
        _popUp.value = null
    }

    fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun setIsStartButtonClicked(value: Boolean) {
        _isStartButtonClicked.value = value
    }

    fun setIsRandomGameButtonClicked(value: Boolean) {
        _isRandomGameButtonClicked.value = value
    }

    fun setIsScoreClicked(value: Boolean) {
        _isScoreClicked.value = value
    }

    fun setIsSliderActive(value: Boolean) {
        _isSliderActive.value = value
    }

    fun toggleScoreOpen() {
        if (_isMenuOpen.value) {
            _isMenuOpen.value = false
        }
        _isScoreOpen.value = !_isScoreOpen.value
    }

    fun setInputMode(mode: InputMode) {
        playFeedback(FeedbackType.TAP)
        _settingsState.value = _settingsState.value.copy(inputMode = mode)
        saveState()
    }

    fun setInputDirectionMode(mode: InputDirectionMode) {
        playFeedback(FeedbackType.TAP)
        _settingsState.value = _settingsState.value.copy(inputDirectionMode = mode)
        saveState()
    }

    fun setFormMode(mode: FormMode) {
        playFeedback(FeedbackType.TAP)
        _settingsState.value = _settingsState.value.copy(formMode = mode)
        saveState()
    }

    fun toggleGroup(group: SettingsGroup) {
        playFeedback(FeedbackType.TAP)
        _expandedGroup.value =
            if (_expandedGroup.value == group) {
                null
            } else {
                group
            }
    }

    fun resetSettings() {
        _settingsState.value = SettingsState()
        _isLoaded.value = false
        _isMenuOpen.value = false
        _expandedGroup.value = null
        _isScoreOpen.value = false
        _popUp.value = null
        _hasGamesWithScore.value = false
        _isScoreClicked.value = false
        _isStartButtonClicked.value = false
        _isRandomGameButtonClicked.value = false
        _isSliderActive.value = false
        saveState()
    }

    fun checkForGamesWithScore() {
        val game = settingsState.value.toNewGameRecord(seed = 0)

        viewModelScope.launch(Dispatchers.IO) {
            val hasGamesWithScore = gameRecordRepository.hasGamesWithScore(
                game.gameMode,
                game.modeLimit,
                game.additionRangeStart,
                game.additionRangeEnd,
                game.subtractionRangeStart,
                game.subtractionRangeEnd,
                game.multiplicationRangeStart,
                game.multiplicationRangeEnd,
                game.divisionRangeStart,
                game.divisionRangeEnd
            )
            _hasGamesWithScore.value = hasGamesWithScore
        }
    }

    fun loadBestGameScores() {
        val game = settingsState.value.toNewGameRecord(seed = 0)

        viewModelScope.launch(Dispatchers.IO) {
            val resultBestGames = gameRecordRepository.findBestGameScores(
                game.gameMode,
                game.modeLimit,
                game.additionRangeStart,
                game.additionRangeEnd,
                game.subtractionRangeStart,
                game.subtractionRangeEnd,
                game.multiplicationRangeStart,
                game.multiplicationRangeEnd,
                game.divisionRangeStart,
                game.divisionRangeEnd
            )
            _bestGameScores.value = resultBestGames
        }
    }

    suspend fun wasAlreadyPlayed(gameRecord: GameRecord): Boolean {
        return gameRecordRepository.wasAlreadyPlayed(gameRecord)
    }

    fun generateSeedLink(seed: Long): String {
        return SettingsLinkUtil.createLink(settingsState.value.toNewGameRecord(seed))
    }

    fun playFeedback(feedbackType: FeedbackType) {
        feedbackController.play(feedbackType)
    }

    private fun saveState() {
        viewModelScope.launch {
            settingsRepository.saveSettings(_settingsState.value)
        }
    }
}