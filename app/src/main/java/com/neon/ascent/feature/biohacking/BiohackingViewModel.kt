package com.neon.ascent.feature.biohacking

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.repository.*
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.HealthTrend
import com.neon.ascent.model.TerminalEvent
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.entity.NeuralMemory
import com.neon.ascent.core.data.datastore.HealthPreferencesDataStore
import com.neon.ascent.feature.health.data.uplink.NeuralUplinkManager
import com.neon.ascent.feature.health.domain.uplink.UplinkSyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.*

@HiltViewModel
class BiohackingViewModel @Inject constructor(
    private val biohackingDao: BiohackingDao,
    private val userCharacterDao: UserCharacterDao,
    private val healthRepository: HealthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthPrefs: HealthPreferencesDataStore,
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val aiProvider: AiProvider,
    private val bioAgeRepository: BioAgeRepository,
    private val neuralMemoryDao: NeuralMemoryDao,
    private val uplinkManager: NeuralUplinkManager,
    val modelDownloadManager: ModelDownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiohackingData())
    val uiState: StateFlow<BiohackingData> = _uiState.asStateFlow()

    private val _character = MutableStateFlow<UserCharacter?>(null)
    val character: StateFlow<UserCharacter?> = _character.asStateFlow()

    private val _logs = MutableStateFlow<List<BioProtocolLog>>(emptyList())
    val logs: StateFlow<List<BioProtocolLog>> = _logs.asStateFlow()

    private val _trends = MutableStateFlow<List<HealthTrend>>(emptyList())
    val trends: StateFlow<List<HealthTrend>> = _trends.asStateFlow()

    private val _isNeuralCoreThinking = MutableStateFlow(false)
    val isNeuralCoreThinking: StateFlow<Boolean> = _isNeuralCoreThinking.asStateFlow()

    private val _latestReport = MutableStateFlow<String?>(null)
    val latestReport: StateFlow<String?> = _latestReport.asStateFlow()

    val neuralInsights: StateFlow<List<NeuralMemory>> = neuralMemoryDao.getMemoriesByWing("INSIGHTS")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAiType: StateFlow<AiType> = aiProvider.activeAiType

    val liveMonitoringEnabled: StateFlow<Boolean> = healthPrefs.liveMonitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val uplinkSyncStatuses: StateFlow<List<UplinkSyncStatus>> = uplinkManager.uplinkSyncStatuses

    val measurementUnit: StateFlow<String> = userPreferencesRepository.measurementUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Metric")

    val cachedBioAge: StateFlow<Float?> = userPreferencesRepository.lastBioAge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val terminalFeed: StateFlow<List<TerminalEvent>> = combine(
        taskRepository.getDailyTasks(),
        goalRepository.getActiveGoals(),
        biohackingDao.getProtocolLogs(0)
    ) { tasks, goals, logs ->
        val today = LocalDate.now()
        val startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = mutableListOf<TerminalEvent>()

        tasks.forEach { task ->
            if (task.completedDates.contains(today)) {
                events.add(TerminalEvent(task.id, task.title, "TASK", "COMPLETED", task.updatedAt))
            } else {
                val status = if (task.createdAt >= startOfToday) "ADDED" else "PENDING"
                events.add(TerminalEvent(task.id, task.title, "TASK", status, task.updatedAt))
            }
        }

        goals.forEach { goal ->
            val status = if (goal.createdAt >= startOfToday) "ADDED" else "ACTIVE"
            events.add(TerminalEvent(goal.id, goal.title, "MISSION", status, goal.updatedAt))
        }

        logs.filter { it.timestamp >= startOfToday }.forEach { log ->
            events.add(TerminalEvent(log.id.toString(), "PROTOCOL_LOG // ${log.protocolId}", "PROTOCOL", "LOGGED", log.timestamp))
        }

        events.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTimeRange = MutableStateFlow(7)
    val selectedTimeRange: StateFlow<Int> = _selectedTimeRange.asStateFlow()

    init {
        PDFBoxResourceLoader.init(context)
        viewModelScope.launch {
            bioAgeRepository.initialize()
        }
        viewModelScope.launch {
            biohackingDao.getBiohackingData(0).collectLatest { data ->
                _uiState.value = data ?: BiohackingData()
            }
        }
        viewModelScope.launch {
            userCharacterDao.getUserCharacter().collectLatest { char ->
                _character.value = char?.let {
                    UserCharacter(
                        id = it.id,
                        name = it.name,
                        netrunnerName = it.netrunnerName,
                        sex = it.sex,
                        dob = it.dob,
                        units = it.units,
                        heightFeet = it.heightFeet,
                        heightInches = it.heightInches,
                        heightCm = it.heightCm,
                        weight = it.weight,
                        somatotype = it.somatotype,
                        mbti = it.mbti,
                        alignment = it.alignment,
                        archetype = it.archetype,
                        strength = it.strength,
                        endurance = it.endurance,
                        agility = it.agility,
                        perception = it.perception,
                        intelligence = it.intelligence,
                        charisma = it.charisma,
                        luck = it.luck,
                        level = it.level,
                        neuralLoad = it.neuralLoad,
                        experience = it.experience,
                        isCreationComplete = it.isCreationComplete,
                        avatarPath = it.avatarPath,
                        eddies = it.eddies,
                        isSystemDatabaseUnlocked = it.isSystemDatabaseUnlocked
                    )
                }
            }
        }
        viewModelScope.launch {
            biohackingDao.getProtocolLogs(0).collectLatest {
                _logs.value = it
            }
        }
        viewModelScope.launch {
            _selectedTimeRange.collectLatest { days ->
                fetchTrends(days)
            }
        }
    }

    fun setTimeRange(days: Int) {
        _selectedTimeRange.value = days
    }

    private fun fetchTrends(days: Int) {
        viewModelScope.launch {
            if (healthRepository.hasAllPermissions()) {
                val steps = healthRepository.getSteps(days)
                val hrv = healthRepository.getHrv(days)
                val sleep = healthRepository.getSleepDuration(days)
                
                // Fetch recent completed tasks for correlation
                val dailyTasks = taskRepository.getDailyTasks().first()
                val startDate = LocalDate.now().minusDays(days.toLong())
                val recentlyCompletedTasks = dailyTasks.filter { task ->
                    task.completedDates.any { it.isAfter(startDate) || it.isEqual(startDate) }
                }

                val trendsList = mutableListOf<HealthTrend>()

                if (steps.isNotEmpty()) {
                    val stepsData = steps.map { it.second.toFloat() }
                    val avgSteps = stepsData.average().toInt()
                    val insight = if (recentlyCompletedTasks.any { it.title.contains("Walk", true) || it.title.contains("Run", true) }) {
                        "Kinetic missions successfully boosted step count. Average: $avgSteps"
                    } else {
                        "Consistent kinetic output maintains neural plasticity. Average: $avgSteps"
                    }
                    trendsList.add(HealthTrend(
                        label = "STEPS",
                        currentValue = steps.last().second.toString(),
                        dataPoints = stepsData,
                        unit = "STEPS",
                        insight = insight
                    ))
                }

                if (hrv.isNotEmpty()) {
                    val hrvData = hrv.map { it.second.toFloat() }
                    val hrvImproved = if (hrvData.size >= 2) hrvData.last() > hrvData.first() else false
                    val insight = when {
                        hrvImproved && recentlyCompletedTasks.any { it.title.contains("Strength", true) || it.title.contains("Gym", true) } -> 
                            "HRV improved after consistent Strength missions."
                        hrvImproved -> "Autonomic resilience is trending upward."
                        else -> "Autonomic resilience is stable; neural load capacity expanded."
                    }
                    trendsList.add(HealthTrend(
                        label = "HRV",
                        currentValue = String.format(Locale.US, "%.0f", hrv.last().second),
                        dataPoints = hrvData,
                        unit = "ms",
                        insight = insight
                    ))
                }

                if (sleep.isNotEmpty()) {
                    val sleepData = sleep.map { it.second.toFloat() }
                    val avgSleep = String.format(Locale.US, "%.1f", sleepData.average())
                    val insight = if (sleepData.last() < 6f) {
                        "Sub-optimal recovery detected ($avgSleep avg). Prioritize neural cooldown."
                    } else {
                        "Deep state recovery essential for synaptic pruning ($avgSleep avg)."
                    }
                    trendsList.add(HealthTrend(
                        label = "SLEEP",
                        currentValue = String.format(Locale.US, "%.1f", sleep.last().second),
                        dataPoints = sleepData,
                        unit = "HRS",
                        insight = insight
                    ))
                }

                // Proxy for Body Battery / Recovery using HRV and Sleep
                if (hrv.isNotEmpty() && sleep.isNotEmpty()) {
                    val recoveryScore = ((hrv.last().second / 100.0) * 50.0 + (sleep.last().second / 8.0) * 50.0).coerceIn(0.0, 100.0)
                    val recoveryData = List(steps.size) { _ ->
                        // Mocking a recovery trend based on hrv/sleep if we don't have historical daily recovery
                        (recoveryScore * (0.8 + 0.4 * Math.random())).toFloat().coerceIn(0f, 100f)
                    }
                    trendsList.add(HealthTrend(
                        label = "RECOVERY",
                        currentValue = String.format(Locale.US, "%.0f", recoveryScore),
                        dataPoints = recoveryData,
                        unit = "%",
                        insight = "Neural battery depleted. Consider dopamine reset."
                    ))
                }

                _trends.value = trendsList
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
            
            val result = aiProvider.generateContent(prompt, forceLocal = data.enableOnDeviceNeuralCore)
            _latestReport.value = result
            _isNeuralCoreThinking.value = false
            
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
                fetchTrends(_selectedTimeRange.value)
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

    fun processLabResults(uri: Uri) {
        viewModelScope.launch {
            _isNeuralCoreThinking.value = true
            try {
                val contentResolver = context.contentResolver
                val type = contentResolver.getType(uri)
                val text = if (type == "application/pdf") {
                    extractTextFromPdf(uri)
                } else {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }

                if (text != null) {
                    val biomarkers = extractBiomarkersWithAi(text)
                    val result = bioAgeRepository.predictBiologicalAge(biomarkers)
                    val bioAge = result.biologicalAge
                    
                    val calendarAge = calculateAge(_character.value?.dob ?: "2000.01.01").toIntOrNull() ?: 0
                    
                    userPreferencesRepository.cacheBioAge(bioAge)
                    
                    updateData { it.copy(
                        calculatedBioAge = bioAge,
                        calendarAgeAtCalculation = calendarAge,
                        extractedBiomarkersJson = Json.encodeToString(biomarkers)
                    ) }
                    
                    _latestReport.value = """
                        BIO_AGE_SCAN_COMPLETE: Calculated Age is ${String.format(Locale.US, "%.1f", bioAge)} (vs Calendar Age $calendarAge). 
                        Delta: ${String.format(Locale.US, "%.1f", bioAge - calendarAge)}
                        
                        ${result.explanation}
                    """.trimIndent()
                }
            } catch (e: Exception) {
                _latestReport.value = "ERROR_PROCESSING_LAB_RESULTS: ${e.message}"
            } finally {
                _isNeuralCoreThinking.value = false
            }
        }
    }

    private fun extractTextFromPdf(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun extractBiomarkersWithAi(text: String): Map<String, Float> {
        val features = bioAgeRepository.getFeatures()
        val prompt = """
            EXTRACT_BIOMARKERS_REQUEST
            TEXT: $text
            
            Identify the following values from the lab results. Return ONLY a valid JSON object with these keys:
            ${features.joinToString(", ")}
            
            Use null if not found. Do not include any other text.
        """.trimIndent()

        val response = aiProvider.generateContent(prompt, forceLocal = false)
        return try {
            val jsonString = response.substringAfter("{").substringBeforeLast("}")
            val fullJson = "{$jsonString}"
            val jsonElement = Json.parseToJsonElement(fullJson)
            val result = mutableMapOf<String, Float>()
            features.forEach { feature ->
                jsonElement.jsonObject[feature]?.jsonPrimitive?.floatOrNull?.let {
                    result[feature] = it
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun toggleLiveMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            healthPrefs.setLiveMonitoringEnabled(enabled)
        }
    }

    private fun calculateAge(dob: String): String {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val birthDate = java.time.LocalDate.parse(dob, formatter)
            val currentDate = java.time.LocalDate.now()
            java.time.Period.between(birthDate, currentDate).years.toString()
        } catch (e: Exception) {
            "0"
        }
    }
}
