package com.neon.ascent.feature.biohacking

import android.content.Context
import android.net.Uri
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import ai.onnxruntime.*
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.FloatBuffer
import java.util.*

@HiltViewModel
class BiohackingViewModel @Inject constructor(
    private val biohackingDao: BiohackingDao,
    private val userCharacterDao: UserCharacterDao,
    private val healthRepository: HealthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProvider: AiProvider,
    val modelDownloadManager: ModelDownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var ortSession: OrtSession? = null
    private val ortEnv = OrtEnvironment.getEnvironment()
    private var shapData: ShapData? = null

    // MUST match training order EXACTLY (from your Colab)
    private val features = listOf(
        "LBXSAL",      // 0  Albumin
        "LBXSCR",      // 1  Creatinine
        "LBXGLU",      // 2  Glucose
        "LBXHSCRP",    // 3  hs-CRP
        "LBXWBCSI",    // 4  WBC Count
        "LBXLYPCT",    // 5  Lymphocyte %
        "LBXMCVSI",    // 6  MCV
        "LBXRDW",      // 7  RDW
        "LBXSAPSI",    // 8  Alkaline Phosphatase
        "LBXGH",       // 9  HbA1c
        "LBXTC",       // 10 Total Cholesterol
        "LBXTR",       // 11 Triglycerides
        "BMXBMI",      // 12 BMI
        "RIAGENDR",    // 13 Gender (1 = Male, 2 = Female)
        "LBDLYMNO",    // 14 Lymphocyte number
        "LBXRBCSI"     // 15 RBC Count
    )

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
        PDFBoxResourceLoader.init(context)
        loadBioAgeModel()
        loadShapData()
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

    private fun loadBioAgeModel() {
        viewModelScope.launch {
            try {
                val modelBytes = context.assets.open("bioage_xgboost_final.onnx").readBytes()
                ortSession = ortEnv.createSession(modelBytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadShapData() {
        viewModelScope.launch {
            try {
                val json = context.assets.open("shap_explanations.json").bufferedReader().use { it.readText() }
                shapData = Json.decodeFromString<ShapData>(json)
            } catch (e: Exception) {
                // Fallback to rule-based if SHAP fails
                e.printStackTrace()
            }
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
                    val result = predictBiologicalAge(biomarkers)
                    val bioAge = result.biologicalAge
                    
                    val calendarAge = calculateAge(_character.value?.dob ?: "2000.01.01").toIntOrNull() ?: 0
                    
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
        val prompt = """
            EXTRACT_BIOMARKERS_REQUEST
            TEXT: $text
            
            Identify the following values from the lab results. Return ONLY a valid JSON object with these keys:
            ${features.joinToString(", ")}
            
            Use null if not found. Do not include any other text.
        """.trimIndent()

        val response = aiProvider.generateContent(prompt, forceLocal = false) // Cloud might be better for high accuracy extraction
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

    private val medianValues = mapOf(
        "LBXSAL" to 4.1f,
        "LBXSCR" to 0.85f,
        "LBXGLU" to 95f,
        "LBXHSCRP" to 1.5f,
        "LBXWBCSI" to 6.5f,
        "LBXLYPCT" to 30f,
        "LBXMCVSI" to 90f,
        "LBXRDW" to 13f,
        "LBXSAPSI" to 70f,
        "LBXGH" to 5.4f,
        "LBXTC" to 190f,
        "LBXTR" to 120f,
        "BMXBMI" to 26f,
        "RIAGENDR" to 1f,
        "LBDLYMNO" to 2.0f,
        "LBXRBCSI" to 4.8f
    )

    private fun predictBiologicalAge(biomarkers: Map<String, Float>): BioAgeResult {
        val session = ortSession ?: return BioAgeResult(0f, "ORT_SESSION_NOT_INITIALIZED", emptyList())
        
        val inputArray = FloatArray(features.size)
        for (i in features.indices) {
            val featureName = features[i]
            inputArray[i] = biomarkers[featureName] ?: medianValues[featureName] ?: 0f
        }

        val floatBuffer = FloatBuffer.wrap(inputArray)
        val inputName = session.inputNames.firstOrNull() ?: "float_input"
        val inputTensor = OnnxTensor.createTensor(ortEnv, floatBuffer, longArrayOf(1, features.size.toLong()))

        val inputs = mapOf(inputName to inputTensor)
        val outputs = session.run(inputs)
        
        val predictedAge = (outputs?.get(0)?.value as? Array<FloatArray>)?.get(0)?.get(0) ?: 0f
        
        val drivers = generateShapDrivers(biomarkers)
        val explanation = buildNaturalExplanation(predictedAge, drivers)

        return BioAgeResult(
            biologicalAge = predictedAge,
            explanation = explanation,
            keyDrivers = drivers
        )
    }

    private fun generateShapDrivers(biomarkers: Map<String, Float>): List<Driver> {
        val drivers = mutableListOf<Driver>()
        val globalImp = shapData?.global_importance ?: emptyMap()

        // Top impactful features present in input
        features.forEach { feat ->
            val value = biomarkers[feat] ?: return@forEach
            val importance = globalImp[feat] ?: 0f

            if (importance > 0.05f || isCriticalFeature(feat, value)) {
                val direction = when {
                    feat.contains("GLU") && value > 110 -> "+ Metabolic stress"
                    feat.contains("HSCRP") && value > 3.0 -> "+ Inflammation"
                    feat.contains("SAL") && value < 4.0 -> "- Low protein"
                    feat.contains("BMI") && value > 30 -> "+ Obesity effect"
                    feat.contains("GH") && value > 5.7 -> "+ Glycation risk"
                    else -> if (value > (medianValues[feat] ?: 0f)) "Higher" else "Lower"
                }

                drivers.add(
                    Driver(
                        feature = getFriendlyName(feat),
                        value = value,
                        contribution = importance,
                        impactText = direction
                    )
                )
            }
        }

        return drivers.sortedByDescending { it.contribution }.take(4)
    }

    private fun isCriticalFeature(feat: String, value: Float): Boolean {
        return when (feat) {
            "LBXGLU" -> value > 110f
            "LBXHSCRP" -> value > 3f
            "LBXSAL" -> value < 4.0f
            "BMXBMI" -> value > 30f
            "LBXGH" -> value > 5.7f
            else -> false
        }
    }

    private fun buildNaturalExplanation(age: Float, drivers: List<Driver>): String {
        val driverLines = if (drivers.isEmpty()) {
            "All markers look balanced within expected thresholds."
        } else {
            drivers.joinToString("\n") {
                "• ${it.feature}: ${it.impactText} (${it.value})"
            }
        }

        return """
            SYSTEM_ANALYSIS:
            Predicted biological age: ${age.toInt()} years

            $driverLines
        """.trimIndent()
    }

    private fun getFriendlyName(code: String): String = when (code) {
        "LBXSAL" -> "Albumin"
        "LBXSCR" -> "Creatinine"
        "LBXGLU" -> "Glucose"
        "LBXHSCRP" -> "Inflammation (CRP)"
        "LBXWBCSI" -> "WBC Count"
        "LBXLYPCT" -> "Lymphocyte %"
        "LBXMCVSI" -> "MCV"
        "LBXRDW" -> "RDW"
        "LBXSAPSI" -> "Alkaline Phosphatase"
        "LBXGH" -> "HbA1c"
        "LBXTC" -> "Total Cholesterol"
        "LBXTR" -> "Triglycerides"
        "BMXBMI" -> "BMI"
        "RIAGENDR" -> "Gender"
        "LBDLYMNO" -> "Lymphocyte number"
        "LBXRBCSI" -> "RBC Count"
        else -> code
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

data class BioAgeResult(
    val biologicalAge: Float,
    val explanation: String,           // Natural language explanation
    val keyDrivers: List<Driver>       // Top contributors
)

data class Driver(
    val feature: String,
    val value: Float,
    val contribution: Float,           // SHAP value
    val impactText: String             // e.g. "+4.2 years (High glucose)"
)

@kotlinx.serialization.Serializable
data class ShapData(
    val global_importance: Map<String, Float>
)
