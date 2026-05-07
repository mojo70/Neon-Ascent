package com.neon.ascent.feature.biohacking

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.repository.BioAgeRepository
import com.neon.ascent.data.repository.HealthRepository
import com.neon.ascent.data.repository.UserPreferencesRepository
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val aiProvider: AiProvider,
    private val bioAgeRepository: BioAgeRepository,
    val modelDownloadManager: ModelDownloadManager,
    @ApplicationContext private val context: Context
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

    val cachedBioAge: StateFlow<Float?> = userPreferencesRepository.lastBioAge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
