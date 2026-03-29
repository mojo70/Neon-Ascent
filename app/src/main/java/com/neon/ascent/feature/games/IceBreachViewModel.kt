package com.neon.ascent.feature.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.util.SynthAudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class IceBreachViewModel @Inject constructor(
    private val userCharacterDao: UserCharacterDao,
    private val sayingsDao: SayingsDao,
    private val audioPlayer: SynthAudioPlayer
) : ViewModel() {

    val userCharacter: StateFlow<UserCharacter?> = userCharacterDao.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow<IceBreachUiState>(IceBreachUiState.Initializing)
    val uiState: StateFlow<IceBreachUiState> = _uiState.asStateFlow()

    init {
        startBreach()
    }

    private fun startBreach() {
        viewModelScope.launch {
            audioPlayer.playBreachStart()
            val char = userCharacter.value ?: userCharacterDao.getUserCharacter().first()
            val iceLevel = char?.iceLevel ?: 1
            
            // Phase 1: Frequency matching
            val targetFreq = Random.nextFloat() * 4.9f + 0.1f
            _uiState.value = IceBreachUiState.Phase1(targetFreq = targetFreq, currentFreq = 1.0f)
        }
    }

    fun updateFrequency(freq: Float) {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase1) {
            _uiState.value = current.copy(currentFreq = freq)
        }
    }

    fun submitPhase1() {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase1) {
            if (Math.abs(current.currentFreq - current.targetFreq) < 0.3f) {
                audioPlayer.playPhaseSuccess()
                startPhase2()
            } else {
                audioPlayer.playPhaseFail()
                _uiState.value = IceBreachUiState.Failed("FREQUENCY_MISMATCH: SIGNAL_LOST")
            }
        }
    }

    private fun startPhase2() {
        val hexChars = "0123456789ABCDEF"
        val grid = List(16) { hexChars.random().toString() + hexChars.random().toString() }
        val iceLevel = userCharacter.value?.iceLevel ?: 1
        val targetCount = (2 + iceLevel / 3).coerceAtMost(8)
        val targetIndices = (0 until 16).shuffled().take(targetCount).toSet()
        
        _uiState.value = IceBreachUiState.Phase2(
            grid = grid,
            targetIndices = targetIndices,
            selectedIndices = emptySet()
        )
    }

    fun toggleNode(index: Int) {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase2) {
            audioPlayer.playKeyClick()
            val newSelected = if (current.selectedIndices.contains(index)) {
                current.selectedIndices - index
            } else {
                current.selectedIndices + index
            }
            _uiState.value = current.copy(selectedIndices = newSelected)
        }
    }

    fun submitPhase2() {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase2) {
            if (current.selectedIndices == current.targetIndices) {
                audioPlayer.playPhaseSuccess()
                startPhase3()
            } else {
                audioPlayer.playPhaseFail()
                _uiState.value = IceBreachUiState.Failed("NODE_SEQUENCE_ERROR: TRACE_DETECTED")
            }
        }
    }

    private fun startPhase3() {
        viewModelScope.launch {
            val categories = listOf("Street Wisdom", "Truth & Illusion")
            val category = categories.random()
            val sayings = sayingsDao.getSayingsByCategory(category)
            val phrase = if (sayings.isNotEmpty()) {
                val words = sayings.random().text.split(" ").take(4)
                words.joinToString(" ")
            } else {
                "NEON ASCENT CORE PROTOCOL"
            }
            _uiState.value = IceBreachUiState.Phase3(phrase = phrase)
        }
    }

    fun submitPhase3(input: String) {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase3) {
            if (input.equals(current.phrase, ignoreCase = true)) {
                audioPlayer.playSuccess()
                completeBreach()
            }
        }
    }

    private fun completeBreach() {
        viewModelScope.launch {
            val char = userCharacter.value ?: return@launch
            val iceLevel = char.iceLevel
            
            val rewards = if (iceLevel < 6) {
                val xp = 50 + iceLevel * 10
                Pair(xp, 0)
            } else {
                val xp = 75 + iceLevel * 15
                val eddies = (iceLevel * 15).coerceAtMost(300)
                Pair(xp, eddies)
            }
            
            var finalEddies = rewards.second
            if (!char.hasBreachedBefore) {
                finalEddies += 100
                userCharacterDao.setHasBreached()
            }
            
            userCharacterDao.updateExperience(char.experience + rewards.first)
            userCharacterDao.updateEddies(char.eddies + finalEddies)
            userCharacterDao.updateIceLevel(iceLevel + 1)
            
            _uiState.value = IceBreachUiState.Success(xp = rewards.first, eddies = finalEddies)
        }
    }

    fun triggerQuickHack() {
        viewModelScope.launch {
            val char = userCharacter.value ?: return@launch
            if (char.eddies >= 20) {
                audioPlayer.playGlitch()
                userCharacterDao.updateEddies(char.eddies - 20)
                completeBreach()
            }
        }
    }
}
