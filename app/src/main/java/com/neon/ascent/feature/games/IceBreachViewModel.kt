package com.neon.ascent.feature.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.core.domain.character.repository.CharacterRepository
import com.neon.ascent.data.repository.SayingsRepository
import com.neon.ascent.util.SynthAudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
import kotlin.random.Random

sealed class IceBreachUiState {
    object Initializing : IceBreachUiState()
    data class Phase1(val targetFreq: Float, val currentFreq: Float) : IceBreachUiState()
    data class Phase2(
        val hexGrid: List<String> = emptyList(),
        val targetSequences: List<List<String>> = emptyList(),
        val selectedIndices: List<Int> = emptyList(),
        val bufferSize: Int = 4,
        val remainingTime: Int = 15,
        val activeIndex: Int? = null,
        val isRowSelection: Boolean = true
    ) : IceBreachUiState() {
        val grid: List<String> get() = hexGrid
    }
    data class Phase3(val phrase: String, val options: List<String>) : IceBreachUiState()
    data class Success(val xp: Int, val eddies: Int) : IceBreachUiState()
    data class Failed(val reason: String) : IceBreachUiState()
}

@HiltViewModel
class IceBreachViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val sayingsRepository: SayingsRepository,
    private val audioPlayer: SynthAudioPlayer
) : ViewModel() {

    val userCharacter: StateFlow<UserCharacter?> = characterRepository.getUserCharacter()
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
            val char = userCharacter.value ?: characterRepository.getUserCharacter().first()
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
            if (abs(current.currentFreq - current.targetFreq) < 0.35f) {
                startPhase2()
            } else {
                audioPlayer.playPhaseFail()
                _uiState.value = IceBreachUiState.Failed("FREQUENCY_MISMATCH: SIGNAL_LOST")
            }
        } else {
            startPhase2()
        }
    }

    private fun startPhase2() {
        audioPlayer.playPhaseSuccess()
        val hexChars = listOf("7A", "BD", "E9", "1C", "55", "FF")
        val grid = List(16) { hexChars.random() }
        val targetSequence1 = List(2) { hexChars.random() }
        val targetSequence2 = List(3) { hexChars.random() }

        _uiState.value = IceBreachUiState.Phase2(
            hexGrid = grid,
            targetSequences = listOf(targetSequence1, targetSequence2),
            selectedIndices = emptyList(),
            bufferSize = 4,
            remainingTime = 15,
            activeIndex = null,
            isRowSelection = true
        )
        startPhase2Timer()
    }

    private fun startPhase2Timer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value
                if (current is IceBreachUiState.Phase2) {
                    if (current.remainingTime <= 1) {
                        audioPlayer.playPhaseFail()
                        _uiState.value = IceBreachUiState.Failed("BUFFER_OVERFLOW: TIME_EXPIRED")
                        break
                    } else {
                        _uiState.value = current.copy(remainingTime = current.remainingTime - 1)
                    }
                } else {
                    break
                }
            }
        }
    }

    fun selectNode(index: Int) {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase2) {
            audioPlayer.playKeyClick()
            val newSelected = current.selectedIndices + index
            val newIsRowSelection = !current.isRowSelection

            val selectedCodes = newSelected.map { current.grid[it] }
            val allCompleted = current.targetSequences.all { target ->
                isSequenceCompleted(selectedCodes, target)
            }

            if (allCompleted) {
                timerJob?.cancel()
                startPhase3()
            } else if (newSelected.size >= current.bufferSize) {
                timerJob?.cancel()
                val anyCompleted = current.targetSequences.any { target ->
                    isSequenceCompleted(selectedCodes, target)
                }
                if (anyCompleted) {
                    startPhase3()
                } else {
                    audioPlayer.playPhaseFail()
                    _uiState.value = IceBreachUiState.Failed("BUFFER_FULL: SEQUENCE_UNSATISFIED")
                }
            } else {
                _uiState.value = current.copy(
                    selectedIndices = newSelected,
                    activeIndex = index,
                    isRowSelection = newIsRowSelection
                )
            }
        }
    }

    fun resetPhase2() {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase2) {
            _uiState.value = current.copy(
                selectedIndices = emptyList(),
                activeIndex = null,
                isRowSelection = true
            )
        }
    }

    private fun isSequenceCompleted(selected: List<String>, target: List<String>): Boolean {
        if (target.isEmpty()) return true
        if (selected.size < target.size) return false
        for (i in 0..selected.size - target.size) {
            if (selected.subList(i, i + target.size) == target) return true
        }
        return false
    }

    private fun startPhase3() {
        viewModelScope.launch {
            audioPlayer.playPhaseSuccess()
            val sayings = sayingsRepository.getAllSayings().first()
            val selectedSaying = sayings.randomOrNull()?.text ?: "In Night City, identity is the only analog code."

            val words = selectedSaying.split(" ").filter { it.length > 3 }
            val targetPhrase = if (words.isNotEmpty()) words.random().lowercase() else "cyberpunk"
            
            val distractors = listOf("singularity", "algorithm", "bandwidth", "overwrite", "protocol", "quantum")
                .filter { it != targetPhrase }
                .shuffled()
                .take(3)
            
            val options = (distractors + targetPhrase).shuffled()

            _uiState.value = IceBreachUiState.Phase3(phrase = targetPhrase, options = options)
        }
    }

    fun submitPhase3(option: String) {
        val current = _uiState.value
        if (current is IceBreachUiState.Phase3) {
            if (option.lowercase() == current.phrase.lowercase()) {
                audioPlayer.playPhaseSuccess()
                completeBreach()
            } else {
                audioPlayer.playPhaseFail()
                _uiState.value = IceBreachUiState.Failed("SEMANTIC_ERROR: INCORRECT_KEY")
            }
        }
    }

    private fun completeBreach() {
        viewModelScope.launch {
            val char = userCharacter.value ?: characterRepository.getUserCharacter().first()
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
                    characterRepository.saveCharacter(updatedChar)
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
                    characterRepository.saveCharacter(updatedChar)
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
