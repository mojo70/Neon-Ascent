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
import com.neon.ascent.util.StilljackSoundType
import com.neon.ascent.core.data.repository.RitesRepository
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class StilljackUiState(
    val totalSeconds: Int = 600, // Default 10M
    val remainingSeconds: Int = 600,
    val isRunning: Boolean = false,
    val noiseEnabled: Boolean = true,
    val startSound: StilljackSoundType = StilljackSoundType.GONG,
    val midSound: StilljackSoundType = StilljackSoundType.GONG,
    val endSound: StilljackSoundType = StilljackSoundType.GONG
)

@HiltViewModel
class StilljackViewModel @Inject constructor(
    private val ambientPlayer: AmbientAudioPlayer,
    private val ritesRepository: RitesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StilljackUiState())
    val uiState: StateFlow<StilljackUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartInstant: Instant? = null

    fun setDuration(minutes: Int) {
        val seconds = minutes * 60
        _uiState.update { it.copy(totalSeconds = seconds, remainingSeconds = seconds) }
    }

    fun toggleNoise() {
        _uiState.update { it.copy(noiseEnabled = !it.noiseEnabled) }
    }

    fun cycleStartSound() {
        _uiState.update {
            val next = when (it.startSound) {
                StilljackSoundType.OFF -> StilljackSoundType.GONG
                StilljackSoundType.GONG -> StilljackSoundType.BELL
                StilljackSoundType.BELL -> StilljackSoundType.OFF
            }
            it.copy(startSound = next)
        }
    }

    fun cycleMidSound() {
        _uiState.update {
            val next = when (it.midSound) {
                StilljackSoundType.OFF -> StilljackSoundType.GONG
                StilljackSoundType.GONG -> StilljackSoundType.BELL
                StilljackSoundType.BELL -> StilljackSoundType.OFF
            }
            it.copy(midSound = next)
        }
    }

    fun cycleEndSound() {
        _uiState.update {
            val next = when (it.endSound) {
                StilljackSoundType.OFF -> StilljackSoundType.GONG
                StilljackSoundType.GONG -> StilljackSoundType.BELL
                StilljackSoundType.BELL -> StilljackSoundType.OFF
            }
            it.copy(endSound = next)
        }
    }

    fun startStilljack() {
        if (_uiState.value.isRunning) return

        sessionStartInstant = Instant.now()
        _uiState.update { it.copy(isRunning = true) }
        
        if (_uiState.value.noiseEnabled) {
            ambientPlayer.startWhiteNoise()
        }

        if (_uiState.value.startSound != StilljackSoundType.OFF) {
            ambientPlayer.playSound(_uiState.value.startSound)
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1.seconds)
                _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }

                // Mid-point check
                if (_uiState.value.midSound != StilljackSoundType.OFF &&
                    _uiState.value.remainingSeconds == _uiState.value.totalSeconds / 2) {
                    ambientPlayer.playSound(_uiState.value.midSound)
                }
            }
            
            if (_uiState.value.endSound != StilljackSoundType.OFF) {
                ambientPlayer.playSound(_uiState.value.endSound)
            }
            
            ambientPlayer.stopWhiteNoise()
            val durMin = (_uiState.value.totalSeconds / 60).coerceAtLeast(1)
            ritesRepository.recordSession(
                kind = "SIT",
                durationMin = durMin,
                source = "STILLJACK",
                startedAt = sessionStartInstant ?: Instant.now(),
                endedAt = Instant.now()
            )
            _uiState.update { it.copy(isRunning = false, remainingSeconds = it.totalSeconds) }
        }
    }

    fun stopStilljack() {
        val completedSec = _uiState.value.totalSeconds - _uiState.value.remainingSeconds
        if (completedSec >= 60) {
            val durMin = completedSec / 60
            viewModelScope.launch {
                ritesRepository.recordSession(
                    kind = "SIT",
                    durationMin = durMin,
                    source = "STILLJACK",
                    startedAt = sessionStartInstant ?: Instant.now(),
                    endedAt = Instant.now()
                )
            }
        }
        timerJob?.cancel()
        ambientPlayer.stopWhiteNoise()
        _uiState.update { it.copy(isRunning = false, remainingSeconds = it.totalSeconds) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
