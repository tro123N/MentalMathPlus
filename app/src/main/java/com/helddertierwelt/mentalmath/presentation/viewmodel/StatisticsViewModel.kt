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

import androidx.lifecycle.ViewModel
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val gameRecordRepository: GameRecordRepository
) : ViewModel() {
    val lastGameRecord: StateFlow<GameRecord> = gameRecordRepository.lastGameRecord

    private val _showLoadedGameRecord = MutableStateFlow(false)
    val showLoadedGameRecord: StateFlow<Boolean> = _showLoadedGameRecord.asStateFlow()
    fun setShowLoadedGameRecord(value: Boolean) {
        _showLoadedGameRecord.value = value
    }

    val loadedGameRecord: StateFlow<GameRecord> = gameRecordRepository.loadedGameRecord
    fun setLoadedGameRecord(gameRecord: GameRecord) {
        gameRecordRepository.setLoadedGameRecord(gameRecord)

    }
}
