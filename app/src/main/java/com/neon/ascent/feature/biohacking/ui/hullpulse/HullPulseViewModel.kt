package com.neon.ascent.feature.biohacking.ui.hullpulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

enum class PulsePhase(val label: String) {
    IDLE("READY"),
    CONTRACT("CONTRACT"),
    HOLD("HOLD"),
    RELEASE("RELEASE")
}

data class HullPulseUiState(
    val currentCycle: Int = 0,
    val totalCycles: Int = 10,
    val phase: PulsePhase = PulsePhase.IDLE,
    val phaseTimeRemaining: Int = 0,
    val isRunning: Boolean = false,
    val totalProgress: Float = 0f
)

@HiltViewModel
class HullPulseViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HullPulseUiState())
    val uiState: StateFlow<HullPulseUiState> = _uiState.asStateFlow()

    private var pulseJob: Job? = null

    fun startPulse() {
        if (_uiState.value.isRunning) return
        
        pulseJob = viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, currentCycle = 1, totalProgress = 0f) }
            
            for (cycle in 1.._uiState.value.totalCycles) {
                _uiState.update { it.copy(currentCycle = cycle) }
                
                // 1. Contract/Hold (4s)
                runPhase(PulsePhase.HOLD, 4)
                
                // 2. Release (4s)
                runPhase(PulsePhase.RELEASE, 4)
                
                _uiState.update { it.copy(totalProgress = cycle.toFloat() / _uiState.value.totalCycles) }
            }
            
            _uiState.update { it.copy(isRunning = false, phase = PulsePhase.IDLE, totalProgress = 1f) }
        }
    }

    private suspend fun runPhase(phase: PulsePhase, seconds: Int) {
        _uiState.update { it.copy(phase = phase, phaseTimeRemaining = seconds) }
        for (i in seconds downTo 1) {
            _uiState.update { it.copy(phaseTimeRemaining = i) }
            delay(1.seconds)
        }
    }

    fun stopPulse() {
        pulseJob?.cancel()
        _uiState.update { HullPulseUiState() }
    }

    override fun onCleared() {
        super.onCleared()
        pulseJob?.cancel()
    }
}
