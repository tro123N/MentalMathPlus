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
import com.helddertierwelt.mentalmath.data.repository.SettingsRepository
import com.helddertierwelt.mentalmath.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    private val _settingsState = mutableStateOf(SettingsState())
    val settingsState: State<SettingsState> = _settingsState

    val isLoaded = mutableStateOf(false)
    val isMenuOpen = mutableStateOf(false)
    val isScoreOpen = mutableStateOf(false)
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

    fun togglePlus() {
        _settingsState.value =
            _settingsState.value.copy(isAdditionEnabled = !_settingsState.value.isAdditionEnabled)
        saveState()
    }

    fun toggleMinus() {
        _settingsState.value =
            _settingsState.value.copy(isSubtractionEnabled = !_settingsState.value.isSubtractionEnabled)
        saveState()
    }

    fun toggleMultiply() {
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
}
