package com.neon.ascent.feature.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.util.SynthAudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

sealed class IceBreachUiState {
    object Initializing : IceBreachUiState()
    data class Phase1(val targetFreq: Float, val currentFreq: Float) : IceBreachUiState()
    data class Phase2(
        val grid: List<String>,
        val targetSequences: List<List<String>>,
        val selectedIndices: List<Int>,
        val bufferSize: Int,
        val isRowSelection: Boolean,
        val activeIndex: Int?,
        val remainingTime: Int,
        val isTimerStarted: Boolean
    ) : IceBreachUiState()
    data class Phase3(val phrase: String, val options: List<String>) : IceBreachUiState()
    data class Success(val xp: Int, val eddies: Int) : IceBreachUiState()
    data class Failed(val reason: String) : IceBreachUiState()
}

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
        
        val targetSequences = mutableListOf<List<String>>()
        val lengths = listOf(2, 3, (3 + iceLevel / 5).coerceAtMost(4))
        
        lengths.forEach { len ->
            val path = generateValidPath(grid, len)
            targetSequences.add(path.map { grid[it] })
        }

        val maxLen = lengths.maxOrNull() ?: 2
        val baseBuffer = (maxLen + 2).coerceAtMost(8) 
        val bonusBuffer = if (iceLevel < 5) 1 else 0 

        val baseTime = (25 - (iceLevel * 0.5)).coerceAtLeast(10.0).toInt()

        _uiState.value = IceBreachUiState.Phase2(
            grid = grid,
            targetSequences = targetSequences,
            selectedIndices = emptyList(),
            bufferSize = baseBuffer + bonusBuffer,
            isRowSelection = true,
            activeIndex = null,
            remainingTime = baseTime,
            isTimerStarted = false
        )
    }

    private fun generateValidPath(grid: List<String>, length: Int): List<Int> {
        val path = mutableListOf<Int>()
        var currentIdx = Random.nextInt(4) 
        var isSearchingRow = true 
        val usedIndices = mutableSetOf<Int>()

        repeat(length) {
            path.add(currentIdx)
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
        return path
    }

    fun selectNode(index: Int) {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase2) {
            if (current.selectedIndices.contains(index)) return
            
            audioPlayer.playKeyClick()
            if (!current.isTimerStarted) startTimer()

            val newSelected = current.selectedIndices + index
            val selectedCodes = newSelected.map { current.grid[it] }
            
            val completedCount = current.targetSequences.count { isSequenceMatched(selectedCodes, it) }
            val allCompleted = completedCount == current.targetSequences.size
            
            if (allCompleted) {
                timerJob?.cancel()
                audioPlayer.playPhaseSuccess()
                startPhase3()
            } else if (newSelected.size >= current.bufferSize) {
                timerJob?.cancel()
                if (completedCount > 0) {
                    audioPlayer.playPhaseSuccess()
                    startPhase3()
                } else {
                    audioPlayer.playPhaseFail()
                    _uiState.value = IceBreachUiState.Failed("BUFFER_OVERFLOW: UPLOAD_FAILED")
                }
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
                        val selectedCodes = current.selectedIndices.map { current.grid[it] }
                        val completedAny = current.targetSequences.any { isSequenceMatched(selectedCodes, it) }
                        
                        if (completedAny) {
                            audioPlayer.playPhaseSuccess()
                            startPhase3()
                        } else {
                            audioPlayer.playPhaseFail()
                            _uiState.value = IceBreachUiState.Failed("TRACE_DETECTED: CONNECTION_TERMINATED")
                        }
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
            val char = userCharacter.value ?: userCharacterDao.getUserCharacter().first()
            val iceLevel = char?.iceLevel ?: 1
            
            val xpReward = if (iceLevel < 6) 50 + iceLevel * 10 else 75 + iceLevel * 15
            var eddiesReward = if (iceLevel < 6) 0 else (iceLevel * 15).coerceAtMost(300)
            
            if (char?.hasBreachedBefore == false) {
                eddiesReward += 100
            }

            // Immediately set UI success state
            _uiState.value = IceBreachUiState.Success(xp = xpReward, eddies = eddiesReward)

            // Persist the changes using a single robust update call
            withContext(NonCancellable) {
                char?.let {
                    val updatedChar = it.copy(
                        experience = it.experience + xpReward,
                        eddies = it.eddies + eddiesReward,
                        iceLevel = iceLevel + 1,
                        isSystemDatabaseUnlocked = true, // ROOT THE SYSTEM
                        hasBreachedBefore = true
                    )
                    userCharacterDao.updateUserCharacter(updatedChar)
                }
            }
        }
    }

    fun triggerQuickHack() {
        viewModelScope.launch {
            val char = userCharacter.value ?: return@launch
            if (char.eddies >= 20) {
                audioPlayer.playGlitch()
                // Deduct eddies and set rooted state in one go
                withContext(NonCancellable) {
                    val updatedChar = char.copy(
                        eddies = char.eddies - 20,
                        isSystemDatabaseUnlocked = true,
                        hasBreachedBefore = true
                    )
                    userCharacterDao.updateUserCharacter(updatedChar)
                }
                completeBreach()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
