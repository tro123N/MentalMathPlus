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

package com.helddertierwelt.mentalmath.presentation.viewmodel.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import com.helddertierwelt.mentalmath.presentation.component.history.SortMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val gameRecordRepository: GameRecordRepository
) : ViewModel() {

    private val _allGames = MutableStateFlow<List<GameRecord>>(emptyList())
    val allGames = _allGames.asStateFlow()

    private val _isEntryClicked = MutableStateFlow(false)
    val isEntryClicked = _isEntryClicked.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.TIME_DESCENDING)
    val sortMode = _sortMode.asStateFlow()

    fun loadAllGames() {
        viewModelScope.launch(Dispatchers.IO) {
            val allGames = gameRecordRepository.findAllGameRecords()
            _allGames.value = allGames
        }
    }

    fun setSortMode(sortMode: SortMode) {
        _sortMode.value = sortMode
    }

    fun setIsEntryClicked(isEntryClicked: Boolean) {
        _isEntryClicked.value = isEntryClicked
    }

    fun resetHistory() {
        setSortMode(SortMode.TIME_DESCENDING)
        setIsEntryClicked(false)
        loadAllGames()
    }
}