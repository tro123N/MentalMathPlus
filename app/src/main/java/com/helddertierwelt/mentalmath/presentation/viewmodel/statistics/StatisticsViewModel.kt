package com.helddertierwelt.mentalmath.presentation.viewmodel.statistics

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

    private val _showLoadedGameRecord = MutableStateFlow(false)
    val showLoadedGameRecord: StateFlow<Boolean> = _showLoadedGameRecord.asStateFlow()

    private val _showLinkGamePopUp = MutableStateFlow(false)
    val showLinkGamePopUp: StateFlow<Boolean> = _showLinkGamePopUp.asStateFlow()

    private val _scoreFromLink = MutableStateFlow(0.00f)
    val scoreFromLink: StateFlow<Float> = _scoreFromLink.asStateFlow()

    private val _navigatedFromHistory = MutableStateFlow(false)
    val navigatedFromHistory: StateFlow<Boolean> = _navigatedFromHistory.asStateFlow()

    private val _seedFromLink = MutableStateFlow(0L)
    val seedFromLink: StateFlow<Long> = _seedFromLink.asStateFlow()

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

    fun setLoadedGameRecord(gameRecord: GameRecord) {
        gameRecordRepository.setLoadedGameRecord(gameRecord)
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
}