package com.neon.ascent.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.DailyPrayerDao
import com.neon.ascent.data.repository.CharacterRepository
import com.neon.ascent.data.repository.HealthRepository
import com.neon.ascent.data.repository.JournalRepository
import com.neon.ascent.data.repository.SettingsRepository
import com.neon.ascent.data.repository.UserPreferencesRepository
import com.neon.ascent.data.repository.UserStoryRepository
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.feature.goals.domain.usecases.ExportNeuralLogUseCase
import com.neon.ascent.model.DailyPrayer
import com.neon.ascent.model.JournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val biohackingDao: BiohackingDao,
    private val settingsRepository: SettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dailyPrayerDao: DailyPrayerDao,
    private val journalRepository: JournalRepository,
    private val healthRepository: HealthRepository,
    private val exportNeuralLogUseCase: ExportNeuralLogUseCase,
    private val userStoryRepository: UserStoryRepository,
    private val specialRepository: SpecialRepository,
    private val notificationScheduler: SmartPingScheduler,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _prayerToast = MutableStateFlow<String?>(null)
    val prayerToast = _prayerToast.asStateFlow()

    private val _currentDailyPrayer = MutableStateFlow<DailyPrayer?>(null)
    val currentDailyPrayer = _currentDailyPrayer.asStateFlow()

    private val _isHealthConnectGranted = MutableStateFlow(false)
    val isHealthConnectGranted = _isHealthConnectGranted.asStateFlow()

    private val _exportEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val exportEvent = _exportEvent.asSharedFlow()

    init {
        seedPrayersIfEmpty()
        checkHealthConnectStatus()
    }

    fun checkHealthConnectStatus() {
        viewModelScope.launch {
            _isHealthConnectGranted.value = healthRepository.hasAllPermissions()
        }
    }

    fun getHealthPermissions(): Set<String> {
        return healthRepository.permissions
    }

    private fun seedPrayersIfEmpty() {
        viewModelScope.launch {
            val existingPrayers = dailyPrayerDao.getAllPrayers().first()
            if (existingPrayers.isEmpty()) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.PRAYER_SEED)
            } else if (existingPrayers.size < 60) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_2_SEED)
            } else if (existingPrayers.size < 90) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_3_SEED)
            } else if (existingPrayers.size < 120) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_4_SEED)
            } else if (existingPrayers.size < 150) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_5_SEED)
            } else if (existingPrayers.size < 180) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_6_SEED)
            } else if (existingPrayers.size < 210) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_7_SEED)
            } else if (existingPrayers.size < 240) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_8_SEED)
            } else if (existingPrayers.size < 270) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_9_SEED)
            } else if (existingPrayers.size < 300) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_10_SEED)
            } else if (existingPrayers.size < 330) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_11_SEED)
            } else if (existingPrayers.size < 365) {
                dailyPrayerDao.insertPrayers(DailyPrayerSeeds.MONTH_12_SEED)
            }
        }
    }

    fun loadTodayPrayer() {
        viewModelScope.launch {
            val char = userCharacter.value ?: return@launch
            val dayToFetch = (char.prayerStreak % 365) + 1
            _currentDailyPrayer.value = dailyPrayerDao.getPrayerForDay(dayToFetch)
        }
    }

    fun sealDailyPrayer(amen: String, reflection: String = "") {
        viewModelScope.launch {
            val prayer = _currentDailyPrayer.value ?: return@launch
            val now = System.currentTimeMillis()

            if (reflection.isNotBlank()) {
                val entryText = "VERSE: ${prayer.scripture}\n\nREFLECTION: $reflection"
                journalRepository.saveToJournal(
                    JournalEntry(
                        id = UUID.randomUUID().toString(),
                        text = entryText,
                        category = "DIVINE_DOWNLOAD",
                        timestamp = now
                    )
                )
            }

            val char = userCharacter.value ?: return@launch
            val isSameDay = isSameDay(char.lastPrayerDate, now)
            if (isSameDay) {
                _prayerToast.value = "Neural Reflection logged. Streak already maintained for today."
                return@launch
            }

            val isNextDay = isNextDay(char.lastPrayerDate, now)
            val newStreak = if (isNextDay || char.lastPrayerDate == 0L) char.prayerStreak + 1 else 1
            
            var expToAdd = 10
            if (newStreak % 7 == 0) expToAdd += 15

            val updatedChar = char.copy(
                experience = char.experience + expToAdd,
                prayerStreak = newStreak,
                lastPrayerDate = now
            )
            characterRepository.updateCharacter(updatedChar)
            
            _prayerToast.value = "Prayer Pulse Received. Holy Ghost Signal Strengthened."
            settingsRepository.setLastAltarVisit(now)
        }
    }

    fun dismissPrayerToast() {
        _prayerToast.value = null
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val d1 = t1 / (1000 * 60 * 60 * 24)
        val d2 = t2 / (1000 * 60 * 60 * 24)
        return d1 == d2
    }

    private fun isNextDay(t1: Long, t2: Long): Boolean {
        val d1 = t1 / (1000 * 60 * 60 * 24)
        val d2 = t2 / (1000 * 60 * 60 * 24)
        return d2 == d1 + 1
    }

    val isBiometricLockEnabled: StateFlow<Boolean> = settingsRepository.isBiometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isReligionShortcutEnabled: StateFlow<Boolean> = settingsRepository.isReligionShortcutEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isLocalAiOnly: StateFlow<Boolean> = settingsRepository.isLocalAiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasCompletedSinnersPrayer: StateFlow<Boolean> = settingsRepository.hasCompletedSinnersPrayer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastAltarVisit: StateFlow<Long> = settingsRepository.lastAltarVisit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val measurementUnit: StateFlow<String> = userPreferencesRepository.measurementUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Metric")

    val userCharacter: StateFlow<UserCharacter?> = characterRepository.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Notification Settings
    val isNeuralBriefEnabled = settingsRepository.isNeuralBriefEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val quietHoursStart = settingsRepository.quietHoursStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "22:00")

    val quietHoursEnd = settingsRepository.quietHoursEnd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "07:00")

    val briefFrequency = settingsRepository.briefFrequency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DAILY")

    val insightDepth = settingsRepository.insightDepth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DETAILED")

    // Neon Guide & AI
    val guideVerbosity = userPreferencesRepository.guideVerbosity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "STANDARD")
        
    val cloudFallbackEnabled = userPreferencesRepository.cloudFallbackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val expertWeighting = userPreferencesRepository.expertWeighting
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BALANCED")

    // Appearance & Interface
    val isDopamineMenuVisible = userPreferencesRepository.isDopamineMenuVisible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val isSelfMapVisible = userPreferencesRepository.isSelfMapVisible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val neonIntensity = userPreferencesRepository.neonIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.8f)

    // Privacy
    val isShardVaultEnabled = userPreferencesRepository.isShardVaultEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setNeuralBriefEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNeuralBriefEnabled(enabled)
            notificationScheduler.scheduleSmartPings()
        }
    }

    fun setQuietHoursStart(time: String) {
        viewModelScope.launch {
            settingsRepository.setQuietHoursStart(time)
            notificationScheduler.scheduleSmartPings()
        }
    }

    fun setQuietHoursEnd(time: String) {
        viewModelScope.launch {
            settingsRepository.setQuietHoursEnd(time)
            notificationScheduler.scheduleSmartPings()
        }
    }

    fun setBriefFrequency(frequency: String) {
        viewModelScope.launch {
            settingsRepository.setBriefFrequency(frequency)
            notificationScheduler.scheduleSmartPings()
        }
    }

    fun setInsightDepth(depth: String) {
        viewModelScope.launch {
            settingsRepository.setInsightDepth(depth)
        }
    }

    fun setGuideVerbosity(verbosity: String) {
        viewModelScope.launch { userPreferencesRepository.setGuideVerbosity(verbosity) }
    }

    fun setCloudFallbackEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setCloudFallbackEnabled(enabled) }
    }

    fun setExpertWeighting(weighting: String) {
        viewModelScope.launch { userPreferencesRepository.setExpertWeighting(weighting) }
    }

    fun setDopamineMenuVisible(visible: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setDopamineMenuVisible(visible) }
    }

    fun setSelfMapVisible(visible: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setSelfMapVisible(visible) }
    }

    fun setNeonIntensity(intensity: Float) {
        viewModelScope.launch { userPreferencesRepository.setNeonIntensity(intensity) }
    }

    fun setShardVaultEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShardVaultEnabled(enabled) }
    }

    fun debugTriggerTestBrief() {
        viewModelScope.launch {
            notificationScheduler.enqueueDailyNeuralBrief(isTestRequest = true)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricLockEnabled(enabled)
        }
    }

    fun setLocalAiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLocalAiOnly(enabled)
        }
    }

    fun setReligionShortcutEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReligionShortcutEnabled(enabled)
        }
    }

    fun acceptHolyGhost() {
        viewModelScope.launch {
            settingsRepository.setCompletedSinnersPrayer(true)
            val char = characterRepository.getUserCharacter().first()
            if (char != null && (char.holyGhost ?: 0) < 1) {
                characterRepository.updateHolyGhost(1)
            }
        }
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
            // Holy Spirit baptism usually brings you to a higher spiritual level in this game's logic
            characterRepository.updateHolyGhost(3)
        }
    }

    fun initializeWorkoutLibrary() {
        viewModelScope.launch {
            workoutRepository.seedStarterExercises()
        }
    }

    fun setMeasurementUnit(unit: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateMeasurementUnit(unit)
        }
    }

    fun exportNeuralLog() {
        viewModelScope.launch {
            val logContent = exportNeuralLogUseCase()
            _exportEvent.emit(logContent)
        }
    }

    fun resetProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            characterRepository.resetCharacter()
            settingsRepository.setReligionShortcutEnabled(false)
            settingsRepository.setLocalAiOnly(false)
            settingsRepository.setNetrunnerMode(false)
            settingsRepository.setFirstAiCoreEntry(true)
            settingsRepository.setCompletedSinnersPrayer(false)
            settingsRepository.setLastAltarVisit(0L)
            biohackingDao.deleteBiohackingData(0)
            biohackingDao.deleteBioProtocolLogs(0)
            userStoryRepository.resetStory()
            specialRepository.resetSpecialAttributes()
            onComplete()
        }
    }
}
