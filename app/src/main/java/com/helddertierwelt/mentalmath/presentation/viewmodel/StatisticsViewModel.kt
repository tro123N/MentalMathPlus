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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.entity.GameScore
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val gameRecordRepository: GameRecordRepository
) : ViewModel() {

    private val _showLoadedGameRecord = MutableStateFlow(false)
    val showLoadedGameRecord = _showLoadedGameRecord.asStateFlow()

    private val _showLinkGamePopUp = MutableStateFlow(false)
    val showLinkGamePopUp = _showLinkGamePopUp.asStateFlow()

    private val _scoreFromLink = MutableStateFlow(0.00f)
    val scoreFromLink = _scoreFromLink.asStateFlow()

    private val _navigatedFromHistory = MutableStateFlow(false)
    val navigatedFromHistory = _navigatedFromHistory.asStateFlow()

    private val _seedFromLink = MutableStateFlow(0L)
    val seedFromLink = _seedFromLink.asStateFlow()

    private val _bestGameScores = MutableStateFlow<List<GameScore>>(emptyList())
    val bestGameScore = _bestGameScores.asStateFlow()

    val loadedGameRecord: StateFlow<GameRecord> = gameRecordRepository.loadedGameRecord
    val lastGameRecord: StateFlow<GameRecord> = gameRecordRepository.lastGameRecord

    fun setShowLoadedGameRecord(value: Boolean) {
        _showLoadedGameRecord.value = value
    }

    fun setShowLinkGamePopUp(value: Boolean) {
        _showLinkGamePopUp.value = value
    }

    fun setScoreFromLink(value: Float) {
        _scoreFromLink.value = value
    }

    fun setNavigatedFromHistory(value: Boolean) {
        _navigatedFromHistory.value = value
    }

    fun setSeedFromLink(value: Long) {
        _seedFromLink.value = value
    }

    fun setLoadedGameRecord(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            gameRecordRepository.setLoadedGameRecord(id)
        }
    }

    fun resetStatistics() {
        setNavigatedFromHistory(false)
        setShowLinkGamePopUp(false)
        setShowLoadedGameRecord(false)
        setScoreFromLink(0.00f)
        setSeedFromLink(0L)
    }

    fun prepareGameFromLink(gameRecord: GameRecord) {
        setNavigatedFromHistory(false)
        setShowLinkGamePopUp(false)
        setScoreFromLink(gameRecord.activeScore)
        setSeedFromLink(gameRecord.seed)
    }

    fun loadBestGameScores() {
        val gameRecord = if (_showLoadedGameRecord.value) loadedGameRecord.value else lastGameRecord.value
        viewModelScope.launch(Dispatchers.IO) {
            val resultBestGameScores = gameRecordRepository.findBestGameScores(
                gameRecord.isModeEnabled,
                gameRecord.modeLimit,
                gameRecord.additionRangeStart,
                gameRecord.additionRangeEnd,
                gameRecord.subtractionRangeStart,
                gameRecord.subtractionRangeEnd,
                gameRecord.multiplicationRangeStart,
                gameRecord.multiplicationRangeEnd,
                gameRecord.divisionRangeStart,
                gameRecord.divisionRangeEnd
            )
            _bestGameScores.value = resultBestGameScores
        }
    }
}