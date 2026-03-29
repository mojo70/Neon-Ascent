package com.neon.ascent.feature.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.util.SynthAudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var timerJob: Job? = null

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
        timerJob?.cancel()
        val hexChars = "0123456789ABCDEF"
        val grid = MutableList(16) { hexChars.random().toString() + hexChars.random().toString() }
        val iceLevel = userCharacter.value?.iceLevel ?: 1
        
        // Logic-based pattern generation
        val seqLength = (2 + iceLevel / 4).coerceAtMost(4)
        val targetIndices = mutableListOf<Int>()
        
        var currentIdx = Random.nextInt(4) 
        var isSearchingRow = true 
        val usedIndices = mutableSetOf<Int>()

        repeat(seqLength) {
            targetIndices.add(currentIdx)
            usedIndices.add(currentIdx)
            
            val row = currentIdx / 4
            val col = currentIdx % 4
            
            val nextPossible = if (isSearchingRow) {
                (0 until 4).map { it * 4 + col }.filter { it !in usedIndices }
            } else {
                (row * 4 until (row + 1) * 4).filter { it !in usedIndices }
            }
            
            if (nextPossible.isNotEmpty()) {
                currentIdx = nextPossible.random()
                isSearchingRow = !isSearchingRow
            } else {
                val remaining = (0 until 16).filter { it !in usedIndices }
                if (remaining.isNotEmpty()) currentIdx = remaining.random()
            }
        }
        
        val targetSequence = targetIndices.map { grid[it] }

        val baseBuffer = (seqLength + 2).coerceAtMost(8) 
        val bonusBuffer = if (iceLevel < 5) 1 else 0 

        // Initial countdown time based on difficulty
        val baseTime = (20 - (iceLevel * 0.5)).coerceAtLeast(10.0).toInt()

        _uiState.value = IceBreachUiState.Phase2(
            grid = grid,
            targetSequence = targetSequence,
            selectedIndices = emptyList(),
            bufferSize = baseBuffer + bonusBuffer,
            isRowSelection = true,
            activeIndex = null,
            remainingTime = baseTime,
            isTimerStarted = false
        )
    }

    fun selectNode(index: Int) {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase2) {
            if (current.selectedIndices.contains(index)) return
            
            audioPlayer.playKeyClick()
            
            // Start timer on first move
            if (!current.isTimerStarted) {
                startTimer()
            }

            val newSelected = current.selectedIndices + index
            val selectedCodes = newSelected.map { current.grid[it] }
            
            if (isSequenceMatched(selectedCodes, current.targetSequence)) {
                timerJob?.cancel()
                audioPlayer.playPhaseSuccess()
                startPhase3()
            } else if (newSelected.size >= current.bufferSize) {
                timerJob?.cancel()
                audioPlayer.playPhaseFail()
                _uiState.value = IceBreachUiState.Failed("BUFFER_OVERFLOW: UPLOAD_FAILED")
            } else {
                _uiState.value = current.copy(
                    selectedIndices = newSelected,
                    isRowSelection = !current.isRowSelection,
                    activeIndex = index,
                    isTimerStarted = true
                )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value
                if (current is IceBreachUiState.Phase2) {
                    val newTime = current.remainingTime - 1
                    if (newTime <= 0) {
                        audioPlayer.playPhaseFail()
                        _uiState.value = IceBreachUiState.Failed("TRACE_DETECTED: CONNECTION_TERMINATED")
                        break
                    } else {
                        _uiState.value = current.copy(remainingTime = newTime)
                    }
                } else {
                    break
                }
            }
        }
    }

    fun resetPhase2() {
        startPhase2()
    }

    private fun isSequenceMatched(selected: List<String>, target: List<String>): Boolean {
        if (target.isEmpty()) return true
        for (i in 0..selected.size - target.size) {
            if (selected.subList(i, i + target.size) == target) return true
        }
        return false
    }

    fun toggleNode(index: Int) { selectNode(index) }
    fun submitPhase2() { resetPhase2() }

    private fun startPhase3() {
        viewModelScope.launch {
            val categories = listOf("Street Wisdom", "Truth & Illusion")
            val category = categories.random()
            val allSayingsInCategory = sayingsDao.getSayingsByCategory(category)
            
            if (allSayingsInCategory.size >= 4) {
                val shuffled = allSayingsInCategory.shuffled()
                val targetSaying = shuffled[0]
                val options = shuffled.take(4).map { it.text }.shuffled()
                _uiState.value = IceBreachUiState.Phase3(phrase = targetSaying.text, options = options)
            } else {
                val defaultPhrase = "NEON ASCENT CORE PROTOCOL"
                val defaultOptions = listOf(defaultPhrase, "SYSTEM OVERRIDE INITIATED", "ENCRYPTION KEY EXPIRED", "SIGNAL TERMINATED").shuffled()
                _uiState.value = IceBreachUiState.Phase3(phrase = defaultPhrase, options = defaultOptions)
            }
        }
    }

    fun submitPhase3(input: String) {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase3) {
            // Trim to avoid whitespace issues and use exact comparison
            if (input.trim() == current.phrase.trim()) {
                audioPlayer.playSuccess()
                completeBreach()
            } else {
                audioPlayer.playPhaseFail()
                _uiState.value = IceBreachUiState.Failed("SEMANTIC_ERROR: INCORRECT_KEY")
            }
        }
    }

    private fun completeBreach() {
        viewModelScope.launch {
            // Ensure character data is loaded
            val char = userCharacter.value ?: userCharacterDao.getUserCharacter().first()
            if (char == null) return@launch

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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
