package com.neon.ascent.feature.biohacking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.repository.HealthRepository
import com.neon.ascent.data.repository.UserPreferencesRepository
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BiohackingViewModel @Inject constructor(
    private val biohackingDao: BiohackingDao,
    private val userCharacterDao: UserCharacterDao,
    private val healthRepository: HealthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProvider: AiProvider,
    val modelDownloadManager: ModelDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiohackingData())
    val uiState: StateFlow<BiohackingData> = _uiState.asStateFlow()

    private val _character = MutableStateFlow<UserCharacter?>(null)
    val character: StateFlow<UserCharacter?> = _character.asStateFlow()

    private val _logs = MutableStateFlow<List<BioProtocolLog>>(emptyList())
    val logs: StateFlow<List<BioProtocolLog>> = _logs.asStateFlow()

    private val _isNeuralCoreThinking = MutableStateFlow(false)
    val isNeuralCoreThinking: StateFlow<Boolean> = _isNeuralCoreThinking.asStateFlow()

    private val _latestReport = MutableStateFlow<String?>(null)
    val latestReport: StateFlow<String?> = _latestReport.asStateFlow()

    val activeAiType: StateFlow<AiType> = aiProvider.activeAiType

    val measurementUnit: StateFlow<String> = userPreferencesRepository.measurementUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Metric")

    init {
        viewModelScope.launch {
            biohackingDao.getBiohackingData(0).collectLatest { data ->
                // RESET_BEHAVIOR: If data is null (wiped), reset to default state to trigger onboarding
                _uiState.value = data ?: BiohackingData()
            }
        }
        viewModelScope.launch {
            userCharacterDao.getUserCharacter().collectLatest { char ->
                _character.value = char
            }
        }
        viewModelScope.launch {
            biohackingDao.getProtocolLogs(0).collectLatest {
                _logs.value = it
            }
        }
    }

    fun updateData(update: (BiohackingData) -> BiohackingData) {
        val newData = update(_uiState.value)
        _uiState.value = newData
        viewModelScope.launch {
            biohackingDao.insertOrUpdate(newData)
        }
    }

    fun initiateLocalScan(sector: String) {
        _isNeuralCoreThinking.value = true
        viewModelScope.launch {
            val char = _character.value
            val data = _uiState.value
            
            val prompt = """
                SYSTEM_SCAN_REQUEST: Sector $sector
                USER_PROFILE: ${char?.archetype} / ${char?.mbti}
                BIOMETRICS: Energy=${data.energyScore}, Mood=${data.moodScore}, Focus=${data.focusScore}
                OBJECTIVE: Generate a high-impact cyberpunk biohacking protocol for this sector. 
                FORMAT: Concise, technical, neon-noir style. Max 100 words.
            """.trimIndent()
            
            // Biohacking scan uses forceLocal based on user preference to either strictly use local or allow Cloud fallback
            val result = aiProvider.generateContent(prompt, forceLocal = data.enableOnDeviceNeuralCore)
            _latestReport.value = result
            _isNeuralCoreThinking.value = false
            
            // Persist report
            updateData { it.copy(latestReportJson = result, reportTimestamp = System.currentTimeMillis()) }
        }
    }

    fun syncWearable() {
        viewModelScope.launch {
            if (healthRepository.hasAllPermissions()) {
                val steps = healthRepository.getTodaySteps()
                val heartRate = healthRepository.getLatestHeartRate()
                
                updateData { it.copy(
                    isWearableSynced = true, 
                    lastSyncTimestamp = System.currentTimeMillis(),
                    currentSteps = steps,
                    currentHeartRate = heartRate
                ) }
            }
        }
    }

    fun logProtocolEffectiveness(
        energy: Int,
        sleep: Int,
        mood: Int,
        focus: Int,
        sideEffects: String?,
        notes: String?,
        protocolId: String
    ) {
        viewModelScope.launch {
            val log = BioProtocolLog(
                userId = 0,
                energyScore = energy,
                sleepQuality = sleep,
                moodScore = mood,
                focusScore = focus,
                sideEffects = sideEffects,
                notes = notes,
                protocolId = protocolId
            )
            biohackingDao.insertProtocolLog(log)
        }
    }
}
