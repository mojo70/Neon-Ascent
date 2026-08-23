package com.neon.ascent.feature.altar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.data.local.DailyPrayerDao
import com.neon.ascent.data.repository.CharacterRepository
import com.neon.ascent.data.repository.JournalRepository
import com.neon.ascent.data.repository.SettingsRepository
import com.neon.ascent.feature.settings.DailyPrayerSeeds
import com.neon.ascent.model.DailyPrayer
import com.neon.ascent.model.JournalEntry
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
import java.util.UUID
import javax.inject.Inject

data class AltarUiState(
    val currentPrayer: DailyPrayer? = null,
    val isTrueTextMode: Boolean = false,
    val isSealedToday: Boolean = false,
    val prayerStreak: Int = 0,
    val remainSelectedDurationSeconds: Int = 600, // Default 10 min
    val remainElapsedSeconds: Int = 0,
    val isRemainRunning: Boolean = false,
    val isRemainCompletedToday: Boolean = false,
    val toastMessage: String? = null
)

@HiltViewModel
class AltarViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val settingsRepository: SettingsRepository,
    private val dailyPrayerDao: DailyPrayerDao,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AltarUiState())
    val uiState: StateFlow<AltarUiState> = _uiState.asStateFlow()

    val userCharacter: StateFlow<UserCharacter?> = characterRepository.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isTrueTextMode: StateFlow<Boolean> = settingsRepository.isAltarTrueTextMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var timerJob: Job? = null

    init {
        seedPrayersIfEmpty()
        loadTodayPrayer()
        checkBuffStatus()
    }

    private fun seedPrayersIfEmpty() {
        viewModelScope.launch {
            val existingPrayers = dailyPrayerDao.getAllPrayers().first()
            if (existingPrayers.isEmpty()) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.PRAYER_SEED)
            }
        }
    }

    fun loadTodayPrayer() {
        viewModelScope.launch {
            val char = characterRepository.getUserCharacter().first()
            val now = System.currentTimeMillis()
            val dayToFetch = if (char != null) ((char.prayerStreak % 365) + 1) else 1
            val prayer = dailyPrayerDao.getPrayerForDay(dayToFetch) ?: DailyPrayerSeeds.PRAYER_SEED.first()

            val isSealed = if (char != null) isSameDay(char.lastPrayerDate, now) else false
            val lastBuffDate = settingsRepository.lastRemainBuffDate.first()
            val hasBuff = isSameDay(lastBuffDate, now)
            val trueMode = settingsRepository.isAltarTrueTextMode.first()

            _uiState.value = _uiState.value.copy(
                currentPrayer = prayer,
                isTrueTextMode = trueMode,
                isSealedToday = isSealed,
                prayerStreak = char?.prayerStreak ?: 0,
                isRemainCompletedToday = hasBuff
            )
        }
    }

    private fun checkBuffStatus() {
        viewModelScope.launch {
            val lastBuffDate = settingsRepository.lastRemainBuffDate.first()
            val now = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                isRemainCompletedToday = isSameDay(lastBuffDate, now)
            )
        }
    }

    fun toggleTextMode() {
        viewModelScope.launch {
            val newMode = !_uiState.value.isTrueTextMode
            settingsRepository.setAltarTrueTextMode(newMode)
            _uiState.value = _uiState.value.copy(isTrueTextMode = newMode)
        }
    }

    fun setRemainDuration(seconds: Int) {
        if (_uiState.value.isRemainRunning) return
        _uiState.value = _uiState.value.copy(
            remainSelectedDurationSeconds = seconds,
            remainElapsedSeconds = 0
        )
    }

    fun toggleRemainTimer() {
        if (_uiState.value.isRemainRunning) {
            pauseRemainTimer()
        } else {
            startRemainTimer()
        }
    }

    private fun startRemainTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isRemainRunning = true)
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRemainRunning && _uiState.value.remainElapsedSeconds < _uiState.value.remainSelectedDurationSeconds) {
                delay(1000)
                val newElapsed = _uiState.value.remainElapsedSeconds + 1
                _uiState.value = _uiState.value.copy(remainElapsedSeconds = newElapsed)

                if (newElapsed >= _uiState.value.remainSelectedDurationSeconds) {
                    completeRemainSession()
                    break
                }
            }
        }
    }

    private fun pauseRemainTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isRemainRunning = false)
    }

    fun resetRemainTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isRemainRunning = false,
            remainElapsedSeconds = 0
        )
    }

    private fun completeRemainSession() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            settingsRepository.setLastRemainBuffDate(now)

            val char = characterRepository.getUserCharacter().first()
            if (char != null) {
                val updated = char.copy(
                    experience = char.experience + 40
                )
                characterRepository.updateCharacter(updated)
            }

            _uiState.value = _uiState.value.copy(
                isRemainRunning = false,
                isRemainCompletedToday = true,
                toastMessage = "REMAIN PROTOCOL COMPLETE // +40 XP & SPIRIT BUFF ACTIVATED"
            )
        }
    }

    fun sealUplink() {
        viewModelScope.launch {
            val prayer = _uiState.value.currentPrayer ?: return@launch
            val now = System.currentTimeMillis()
            val char = characterRepository.getUserCharacter().first() ?: return@launch

            val isSameDay = isSameDay(char.lastPrayerDate, now)
            if (isSameDay) {
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Uplink already maintained today. Streak preserved."
                )
                return@launch
            }

            // Log to journal
            val scriptureRef = if (prayer.scriptureReference.isNotBlank()) prayer.scriptureReference else prayer.scripture
            val adore = if (_uiState.value.isTrueTextMode && prayer.adoreTrue.isNotBlank()) prayer.adoreTrue else if (prayer.adoreCyber.isNotBlank()) prayer.adoreCyber else prayer.prayer
            val entryText = "ALTAR UPLINK // $scriptureRef\n\nADORE: $adore\n\nSCRIPTURE: ${prayer.scripture}"

            journalRepository.saveToJournal(
                JournalEntry(
                    id = UUID.randomUUID().toString(),
                    text = entryText,
                    category = "ALTAR_UPLINK",
                    timestamp = now
                )
            )

            val isNextDay = isNextDay(char.lastPrayerDate, now)
            val newStreak = if (isNextDay || char.lastPrayerDate == 0L) char.prayerStreak + 1 else 1

            var expToAdd = 15
            if (newStreak % 7 == 0) expToAdd += 25

            val updatedChar = char.copy(
                experience = char.experience + expToAdd,
                prayerStreak = newStreak,
                lastPrayerDate = now
            )
            characterRepository.updateCharacter(updatedChar)
            settingsRepository.setLastAltarVisit(now)

            _uiState.value = _uiState.value.copy(
                isSealedToday = true,
                prayerStreak = newStreak,
                toastMessage = "AMEN // UPLINK SEALED (+${expToAdd} XP)"
            )
        }
    }

    fun dismissToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun completeWaterBaptism() {
        viewModelScope.launch {
            characterRepository.updateWaterBaptism(true)
            val char = characterRepository.getUserCharacter().first()
            if (char != null) {
                val currentLevel = char.holyGhost ?: 1
                characterRepository.updateHolyGhost(currentLevel + 1)
            }
        }
    }

    fun completeHolySpiritBaptism() {
        viewModelScope.launch {
            characterRepository.updateHolySpiritBaptism(true)
            val char = characterRepository.getUserCharacter().first()
            if (char != null) {
                val currentLevel = char.holyGhost ?: 1
                characterRepository.updateHolyGhost(currentLevel + 1)
            }
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        if (t1 == 0L || t2 == 0L) return false
        val d1 = t1 / (1000 * 60 * 60 * 24)
        val d2 = t2 / (1000 * 60 * 60 * 24)
        return d1 == d2
    }

    private fun isNextDay(t1: Long, t2: Long): Boolean {
        if (t1 == 0L) return false
        val d1 = t1 / (1000 * 60 * 60 * 24)
        val d2 = t2 / (1000 * 60 * 60 * 24)
        return d2 == d1 + 1
    }
}
