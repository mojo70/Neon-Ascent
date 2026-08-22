package com.neon.ascent.feature.biohacking.ui.stilljack

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
import com.neon.ascent.util.AmbientAudioPlayer
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class StilljackUiState(
    val totalSeconds: Int = 600, // Default 10M
    val remainingSeconds: Int = 600,
    val isRunning: Boolean = false,
    val noiseEnabled: Boolean = true,
    val startGongEnabled: Boolean = true,
    val midGongEnabled: Boolean = true,
    val endGongEnabled: Boolean = true
)

@HiltViewModel
class StilljackViewModel @Inject constructor(
    private val ambientPlayer: AmbientAudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(StilljackUiState())
    val uiState: StateFlow<StilljackUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun setDuration(minutes: Int) {
        val seconds = minutes * 60
        _uiState.update { it.copy(totalSeconds = seconds, remainingSeconds = seconds) }
    }

    fun toggleNoise() {
        _uiState.update { it.copy(noiseEnabled = !it.noiseEnabled) }
    }

    fun toggleStartGong() {
        _uiState.update { it.copy(startGongEnabled = !it.startGongEnabled) }
    }

    fun toggleMidGong() {
        _uiState.update { it.copy(midGongEnabled = !it.midGongEnabled) }
    }

    fun toggleEndGong() {
        _uiState.update { it.copy(endGongEnabled = !it.endGongEnabled) }
    }

    fun startStilljack() {
        if (_uiState.value.isRunning) return

        _uiState.update { it.copy(isRunning = true) }
        
        if (_uiState.value.noiseEnabled) {
            ambientPlayer.startWhiteNoise()
        }

        if (_uiState.value.startGongEnabled) {
            ambientPlayer.playGong()
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1.seconds)
                _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }

                // Mid-point check
                if (_uiState.value.midGongEnabled && 
                    _uiState.value.remainingSeconds == _uiState.value.totalSeconds / 2) {
                    ambientPlayer.playGong()
                }
            }
            
            if (_uiState.value.endGongEnabled) {
                ambientPlayer.playGong()
            }
            
            ambientPlayer.stopWhiteNoise()
            _uiState.update { it.copy(isRunning = false, remainingSeconds = it.totalSeconds) }
        }
    }

    fun stopStilljack() {
        timerJob?.cancel()
        ambientPlayer.stopWhiteNoise()
        _uiState.update { it.copy(isRunning = false, remainingSeconds = it.totalSeconds) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
