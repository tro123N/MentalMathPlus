package com.helddertierwelt.mentalmath.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val gameRecordRepository: GameRecordRepository
) : ViewModel() {
    val lastGameRecord: StateFlow<GameRecord> = gameRecordRepository.lastGameRecord
}
