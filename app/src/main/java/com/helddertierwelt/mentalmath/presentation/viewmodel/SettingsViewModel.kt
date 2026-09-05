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
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.entity.GameScore
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import com.helddertierwelt.mentalmath.data.repository.SettingsRepository
import com.helddertierwelt.mentalmath.domain.settings.NotificationMode
import com.helddertierwelt.mentalmath.domain.settings.SettingsState
import com.helddertierwelt.mentalmath.domain.settings.ThemeMode
import com.helddertierwelt.mentalmath.domain.sound.SoundController
import com.helddertierwelt.mentalmath.domain.sound.SoundType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val gameRecordRepository: GameRecordRepository,
    private val soundController: SoundController,
    ) : ViewModel() {

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState = _settingsState.asStateFlow()

    private val _isMenuOpen = MutableStateFlow(false)
    val isMenuOpen = _isMenuOpen.asStateFlow()

    private val _isScoreOpen = MutableStateFlow(false)
    val isScoreOpen = _isScoreOpen.asStateFlow()

    private val _showResetPopUp = MutableStateFlow(false)
    val showResetPopUp = _showResetPopUp.asStateFlow()

    private val _showNotificationPermissionPopUp = MutableStateFlow(false)
    val showNotificationPermissionPopUp = _showNotificationPermissionPopUp.asStateFlow()

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

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { loadedState ->
                _settingsState.value = loadedState
                _isLoaded.value = true
            }
        }
    }

    fun toggleMode() {
        playSound(SoundType.ENABLE)
        _settingsState.value =
            _settingsState.value.copy(isModeEnabled = !_settingsState.value.isModeEnabled)
        saveState()
    }

    fun updateLimit(value: Float) {
        playSound(SoundType.SLIDER)
        _settingsState.value = _settingsState.value.copy(limit = value)
        saveState()
    }

    fun updatePlusRange(plusRange: ClosedFloatingPointRange<Float>) {
        playSound(SoundType.SLIDER)
        _settingsState.value =
            _settingsState.value.copy(additionRange = plusRange.start to plusRange.endInclusive)
        saveState()
    }

    fun updateMinusRange(minusRange: ClosedFloatingPointRange<Float>) {
        playSound(SoundType.SLIDER)
        _settingsState.value =
            _settingsState.value.copy(subtractionRange = minusRange.start to minusRange.endInclusive)
        saveState()
    }

    fun updateMultiplyRange(multiplyRange: ClosedFloatingPointRange<Float>) {
        playSound(SoundType.SLIDER)
        _settingsState.value =
            _settingsState.value.copy(multiplicationRange = multiplyRange.start to multiplyRange.endInclusive)
        saveState()
    }

    fun updateDivideRange(divideRange: ClosedFloatingPointRange<Float>) {
        playSound(SoundType.SLIDER)
        _settingsState.value =
            _settingsState.value.copy(divisionRange = divideRange.start to divideRange.endInclusive)
        saveState()
    }

    fun toggleAddition() {
        val newValue = !_settingsState.value.isAdditionEnabled
        _settingsState.value = _settingsState.value.copy(
            isAdditionEnabled = newValue
        )
        playSound(
            if (newValue) SoundType.ENABLE else SoundType.DISABLE
        )
        saveState()
    }

    fun toggleSubtraction() {
        val newValue = !_settingsState.value.isSubtractionEnabled
        _settingsState.value = _settingsState.value.copy(
            isSubtractionEnabled = newValue
        )
        playSound(
            if (newValue) SoundType.ENABLE else SoundType.DISABLE
        )
        saveState()
    }

    fun toggleMultiplication() {
        val newValue = !_settingsState.value.isMultiplicationEnabled
        _settingsState.value = _settingsState.value.copy(
            isMultiplicationEnabled = newValue
        )
        playSound(
            if (newValue) SoundType.ENABLE else SoundType.DISABLE
        )
        saveState()
    }

    fun toggleDivide() {
        val newValue = !_settingsState.value.isDivisionEnabled
        _settingsState.value = _settingsState.value.copy(
            isDivisionEnabled = newValue
        )
        playSound(
            if (newValue) SoundType.ENABLE else SoundType.DISABLE
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
        playSound(SoundType.ENABLE)
        _settingsState.value = _settingsState.value.copy(themeMode = mode)
        saveState()
    }

    fun setNotificationMode(mode: NotificationMode) {
        _settingsState.value = _settingsState.value.copy(notificationMode = mode)
        saveState()
    }

    fun setShowResetPopUp(value: Boolean) {
        _showResetPopUp.value = value
    }

    fun setShowNotificationPermissionPopUp(value: Boolean) {
        _showNotificationPermissionPopUp.value = value
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

    fun toggleInputLayout() {
        playSound(SoundType.ENABLE)
        _settingsState.value =
            _settingsState.value.copy(isNumpadDefault = !_settingsState.value.isNumpadDefault)
        saveState()
    }

    fun toggleInputMethod(): Boolean {
        playSound(SoundType.ENABLE)
        val newValue = !_settingsState.value.isRightToLeftDefault
        _settingsState.value = _settingsState.value.copy(isRightToLeftDefault = newValue)
        saveState()
        return newValue
    }

    fun toggleForm(): Boolean {
        playSound(SoundType.ENABLE)
        val newValue = !_settingsState.value.isVerticalFormDefault
        _settingsState.value = _settingsState.value.copy(isVerticalFormDefault = newValue)
        saveState()
        return newValue
    }

    fun toggleSound() {
        _settingsState.value =
            _settingsState.value.copy(isSoundEnabled = !_settingsState.value.isSoundEnabled)
        saveState()
        playSound(SoundType.ENABLE)
    }

    fun resetSettings() {
        _settingsState.value = SettingsState()
        _isLoaded.value = false
        _isMenuOpen.value = false
        _isScoreOpen.value = false
        _showResetPopUp.value = false
        _hasGamesWithScore.value = false
        _isScoreClicked.value = false
        _isStartButtonClicked.value = false
        _isRandomGameButtonClicked.value = false
        _isSliderActive.value = false
        _showNotificationPermissionPopUp.value = false
        saveState()
    }

    fun checkForGamesWithScore() {
        val additionRangeStart = if (settingsState.value.isAdditionEnabled) settingsState.value.additionRange.first.toInt() else 0
        val additionRangeEnd = if (settingsState.value.isAdditionEnabled) settingsState.value.additionRange.second.toInt() else 0
        val subtractionRangeStart = if (settingsState.value.isSubtractionEnabled) settingsState.value.subtractionRange.first.toInt() else 0
        val subtractionRangeEnd = if (settingsState.value.isSubtractionEnabled) settingsState.value.subtractionRange.second.toInt() else 0
        val multiplicationRangeStart = if (settingsState.value.isMultiplicationEnabled) settingsState.value.multiplicationRange.first.toInt() else 0
        val multiplicationRangeEnd = if (settingsState.value.isMultiplicationEnabled) settingsState.value.multiplicationRange.second.toInt() else 0
        val divisionRangeStart = if (settingsState.value.isDivisionEnabled) settingsState.value.divisionRange.first.toInt() else 0
        val divisionRangeEnd = if (settingsState.value.isDivisionEnabled) settingsState.value.divisionRange.second.toInt() else 0

        viewModelScope.launch(Dispatchers.IO) {
            val hasGamesWithScore = gameRecordRepository.hasGamesWithScore(
                settingsState.value.isModeEnabled,
                settingsState.value.limit.toInt(),
                additionRangeStart,
                additionRangeEnd,
                subtractionRangeStart,
                subtractionRangeEnd,
                multiplicationRangeStart,
                multiplicationRangeEnd,
                divisionRangeStart,
                divisionRangeEnd
            )
            _hasGamesWithScore.value = hasGamesWithScore
        }
    }

    fun loadBestGameScores() {
        val additionRangeStart = if (settingsState.value.isAdditionEnabled) settingsState.value.additionRange.first.toInt() else 0
        val additionRangeEnd = if (settingsState.value.isAdditionEnabled) settingsState.value.additionRange.second.toInt() else 0
        val subtractionRangeStart = if (settingsState.value.isSubtractionEnabled) settingsState.value.subtractionRange.first.toInt() else 0
        val subtractionRangeEnd = if (settingsState.value.isSubtractionEnabled) settingsState.value.subtractionRange.second.toInt() else 0
        val multiplicationRangeStart = if (settingsState.value.isMultiplicationEnabled) settingsState.value.multiplicationRange.first.toInt() else 0
        val multiplicationRangeEnd = if (settingsState.value.isMultiplicationEnabled) settingsState.value.multiplicationRange.second.toInt() else 0
        val divisionRangeStart = if (settingsState.value.isDivisionEnabled) settingsState.value.divisionRange.first.toInt() else 0
        val divisionRangeEnd = if (settingsState.value.isDivisionEnabled) settingsState.value.divisionRange.second.toInt() else 0
        viewModelScope.launch(Dispatchers.IO) {
            val resultBestGames = gameRecordRepository.findBestGameScores(
                settingsState.value.isModeEnabled,
                settingsState.value.limit.toInt(),
                additionRangeStart,
                additionRangeEnd,
                subtractionRangeStart,
                subtractionRangeEnd,
                multiplicationRangeStart,
                multiplicationRangeEnd,
                divisionRangeStart,
                divisionRangeEnd
            )
            _bestGameScores.value = resultBestGames
        }
    }

    suspend fun checkIfGameWasAlreadyPlayed(gameRecord: GameRecord): Boolean {
        return gameRecordRepository.wasAlreadyPlayed(gameRecord)
    }

    fun applySettingsFromLink(gameRecordFromLink: GameRecord) {
        val isModeEnabled = gameRecordFromLink.isModeEnabled
        val limit = gameRecordFromLink.modeLimit.toFloat()

        val additionRange = Pair(gameRecordFromLink.additionRangeStart.toFloat(), gameRecordFromLink.additionRangeEnd.toFloat())
        val subtractionRange = Pair(gameRecordFromLink.subtractionRangeStart.toFloat(), gameRecordFromLink.subtractionRangeEnd.toFloat())
        val multiplicationRange = Pair(gameRecordFromLink.multiplicationRangeStart.toFloat(), gameRecordFromLink.multiplicationRangeEnd.toFloat())
        val divisionRange = Pair(gameRecordFromLink.divisionRangeStart.toFloat(), gameRecordFromLink.divisionRangeEnd.toFloat())

        val isAdditionEnabled = additionRange.first != 0f && additionRange.second != 0f
        val isSubtractionEnabled = subtractionRange.first != 0f && subtractionRange.second != 0f
        val isMultiplicationEnabled = multiplicationRange.first != 0f && multiplicationRange.second != 0f
        val isDivisionEnabled = divisionRange.first != 0f && divisionRange.second != 0f

        _settingsState.value = _settingsState.value.copy(
            isModeEnabled = isModeEnabled,
            limit = limit,
            additionRange = if (isAdditionEnabled) additionRange else _settingsState.value.additionRange,
            subtractionRange = if (isSubtractionEnabled) subtractionRange else _settingsState.value.subtractionRange,
            multiplicationRange = if (isMultiplicationEnabled) multiplicationRange else _settingsState.value.multiplicationRange,
            divisionRange = if (isDivisionEnabled) divisionRange else _settingsState.value.divisionRange,
            isAdditionEnabled = isAdditionEnabled,
            isSubtractionEnabled = isSubtractionEnabled,
            isMultiplicationEnabled = isMultiplicationEnabled,
            isDivisionEnabled = isDivisionEnabled
        )
        saveState()
    }

    fun generateSeedLink(seed: Long): String {
        val state = _settingsState.value
        val settingsStringByteArray = listOf(
            if (state.isModeEnabled) "1" else "0",
            state.limit.toInt().toString(),
            if (state.isAdditionEnabled) state.additionRange.first.toInt().toString() else "0",
            if (state.isAdditionEnabled) state.additionRange.second.toInt().toString() else "0",
            if (state.isSubtractionEnabled) state.subtractionRange.first.toInt().toString() else "0",
            if (state.isSubtractionEnabled) state.subtractionRange.second.toInt().toString() else "0",
            if (state.isMultiplicationEnabled) state.multiplicationRange.first.toInt().toString() else "0",
            if (state.isMultiplicationEnabled) state.multiplicationRange.second.toInt().toString() else "0",
            if (state.isDivisionEnabled) state.divisionRange.first.toInt().toString() else "0",
            if (state.isDivisionEnabled) state.divisionRange.second.toInt().toString() else "0",
            "%019d".format(Locale.ENGLISH, seed)
        ).joinToString("").toBigInteger().toByteArray()

        val encoded = Base64.encodeToString(settingsStringByteArray, Base64.URL_SAFE or Base64.NO_WRAP)
        return "https://mental-math.codeberg.page/app?s=${encoded}"
    }

    fun playSound(sound: SoundType) {
        soundController.play(sound)
    }

    private fun saveState() {
        viewModelScope.launch {
            repository.saveSettings(_settingsState.value)
        }
    }
}