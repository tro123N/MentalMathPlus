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

package com.helddertierwelt.mentalmath.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import com.helddertierwelt.mentalmath.data.repository.SettingsRepository
import com.helddertierwelt.mentalmath.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val gameRecordRepository: GameRecordRepository
) : ViewModel() {

    private val _settingsState = mutableStateOf(SettingsState())
    val settingsState: State<SettingsState> = _settingsState

    val isLoaded = mutableStateOf(false)
    val isMenuOpen = mutableStateOf(false)
    val isScoreOpen = mutableStateOf(false)

    private val _bestActiveGames = MutableStateFlow<List<GameRecord>>(emptyList())
    val bestActiveGames = _bestActiveGames.asStateFlow()
    private val _bestTotalGames = MutableStateFlow<List<GameRecord>>(emptyList())
    val bestTotalGames = _bestTotalGames.asStateFlow()

    val isScoreSheetEnabled = mutableStateOf(false)

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { loadedState ->
                _settingsState.value = loadedState
                isLoaded.value = true
            }
        }
    }

    fun updateLimit(value: Float) {
        _settingsState.value = _settingsState.value.copy(limit = value)
        saveState()
    }

    fun updatePlusRange(plusRange: ClosedFloatingPointRange<Float>) {
        _settingsState.value =
            _settingsState.value.copy(additionRange = plusRange.start to plusRange.endInclusive)
        saveState()
    }

    fun updateMinusRange(minusRange: ClosedFloatingPointRange<Float>) {
        _settingsState.value =
            _settingsState.value.copy(subtractionRange = minusRange.start to minusRange.endInclusive)
        saveState()
    }

    fun updateMultiplyRange(multiplyRange: ClosedFloatingPointRange<Float>) {
        _settingsState.value =
            _settingsState.value.copy(multiplicationRange = multiplyRange.start to multiplyRange.endInclusive)
        saveState()
    }

    fun updateDivideRange(divideRange: ClosedFloatingPointRange<Float>) {
        _settingsState.value =
            _settingsState.value.copy(divisionRange = divideRange.start to divideRange.endInclusive)
        saveState()
    }

    fun toggleMode() {
        _settingsState.value =
            _settingsState.value.copy(isModeEnabled = !_settingsState.value.isModeEnabled)
        saveState()
    }

    fun toggleAddition() {
        _settingsState.value =
            _settingsState.value.copy(isAdditionEnabled = !_settingsState.value.isAdditionEnabled)
        saveState()
    }

    fun toggleSubtraction() {
        _settingsState.value =
            _settingsState.value.copy(isSubtractionEnabled = !_settingsState.value.isSubtractionEnabled)
        saveState()
    }

    fun toggleMultiplication() {
        _settingsState.value =
            _settingsState.value.copy(isMultiplicationEnabled = !_settingsState.value.isMultiplicationEnabled)
        saveState()
    }

    fun toggleDivide() {
        _settingsState.value =
            _settingsState.value.copy(isDivisionEnabled = !_settingsState.value.isDivisionEnabled)
        saveState()
    }


    fun toggleMenuOpen() {
        if (isScoreOpen.value) {
            isScoreOpen.value = false
        }
        isMenuOpen.value = !isMenuOpen.value
    }

    fun toggleScoreOpen() {
        if (isMenuOpen.value) {
            isMenuOpen.value = false
        }
        isScoreOpen.value = !isScoreOpen.value
    }

    fun toggleDefaultScoreOption() {
        _settingsState.value =
            _settingsState.value.copy(isActiveScoreDefault = !_settingsState.value.isActiveScoreDefault)
        saveState()
    }

    fun disableScoreSheet() {
        isScoreSheetEnabled.value = false
    }

    fun enableScoreSheet() {
        isScoreSheetEnabled.value = true
    }

    fun setThemeMode(mode: ThemeMode) {
        _settingsState.value = _settingsState.value.copy(themeMode = mode)
        saveState()
    }
    fun resetSettings() {
        _settingsState.value = SettingsState()
        saveState()
    }

    private fun saveState() {
        viewModelScope.launch {
            repository.saveSettings(_settingsState.value)
        }
    }

    fun loadBestGames() {
        val additionRangeStart = if (settingsState.value.isAdditionEnabled) settingsState.value.additionRange.first.toInt() else 0
        val additionRangeEnd = if (settingsState.value.isAdditionEnabled) settingsState.value.additionRange.second.toInt() else 0
        val subtractionRangeStart = if (settingsState.value.isSubtractionEnabled) settingsState.value.subtractionRange.first.toInt() else 0
        val subtractionRangeEnd = if (settingsState.value.isSubtractionEnabled) settingsState.value.subtractionRange.second.toInt() else 0
        val multiplicationRangeStart = if (settingsState.value.isMultiplicationEnabled) settingsState.value.multiplicationRange.first.toInt() else 0
        val multiplicationRangeEnd = if (settingsState.value.isMultiplicationEnabled) settingsState.value.multiplicationRange.second.toInt() else 0
        val divisionRangeStart = if (settingsState.value.isDivisionEnabled) settingsState.value.divisionRange.first.toInt() else 0
        val divisionRangeEnd = if (settingsState.value.isDivisionEnabled) settingsState.value.divisionRange.second.toInt() else 0
        viewModelScope.launch(Dispatchers.IO) {
            val resultBestTotalGames = gameRecordRepository.findBestTotalGameRecords(
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
            val resultBestActiveGames = gameRecordRepository.findBestActiveGameRecords(
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
            _bestTotalGames.value = resultBestTotalGames
            _bestActiveGames.value = resultBestActiveGames
        }
    }

    fun applySettingsString(settingsString: String) {
        val parts = settingsString.split(";")
        val isModeEnabled = parts[0] == "1"
        val limit = parts[1].toFloat()
        val additionRange = Pair(parts[2].toFloat(), parts[3].toFloat())
        val subtractionRange = Pair(parts[4].toFloat(), parts[5].toFloat())
        val multiplicationRange = Pair(parts[6].toFloat(), parts[7].toFloat())
        val divisionRange = Pair(parts[8].toFloat(), parts[9].toFloat())
        _settingsState.value = _settingsState.value.copy(
            isModeEnabled = isModeEnabled,
            limit = limit,
            additionRange = additionRange,
            subtractionRange = subtractionRange,
            multiplicationRange = multiplicationRange,
            divisionRange = divisionRange
        )
    }

    fun isValidSettingsString(settingsString: String): Boolean {
        val parts = settingsString.split(";")
        if (parts.size != 11) return false
        if (parts[0] != "0" && parts[0] != "1") return false

        // limit
        val limit = parts[1].toIntOrNull() ?: return false
        if (limit !in 1..5) return false

        // addition
        val addStart = parts[2].toIntOrNull() ?: return false
        val addEnd = parts[3].toIntOrNull() ?: return false
        if (addStart !in 0..9 || addEnd !in 0..9 || addStart > addEnd) return false

        // subtraction
        val subStart = parts[4].toIntOrNull() ?: return false
        val subEnd = parts[5].toIntOrNull() ?: return false
        if (subStart !in 0..9 || subEnd !in 0..9 || subStart > subEnd) return false

        // multiplication
        val mulStart = parts[6].toIntOrNull() ?: return false
        val mulEnd = parts[7].toIntOrNull() ?: return false
        if (mulStart !in 0..9 || mulEnd !in 0..9 || mulStart > mulEnd) return false

        // division
        val divStart = parts[8].toIntOrNull() ?: return false
        val divEnd = parts[9].toIntOrNull() ?: return false
        if (divStart !in 0..9 || divEnd !in 0..9 || divStart > divEnd) return false

        // seed
        if (parts[10].toLongOrNull() == null) return false
        return true
    }
}
