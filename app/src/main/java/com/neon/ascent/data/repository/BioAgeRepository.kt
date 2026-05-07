package com.neon.ascent.data.repository

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.neon.ascent.model.BioAgeResult
import com.neon.ascent.model.Driver
import com.neon.ascent.model.ModelConfig
import com.neon.ascent.model.ShapData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BioAgeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ortEnv = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null
    private var shapData: ShapData? = null
    private var modelConfig: ModelConfig? = null

    private val json = Json { ignoreUnknownKeys = true }

    private var isInitialized = false

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        loadModel()
        loadShapData()
        loadModelConfig()
        isInitialized = true
    }

    private fun loadModel() {
        try {
            val modelBytes = context.assets.open("bioage_xgboost_final.onnx").readBytes()
            ortSession = ortEnv.createSession(modelBytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadShapData() {
        try {
            val jsonText = context.assets.open("shap_explanations.json").bufferedReader().use { it.readText() }
            shapData = json.decodeFromString<ShapData>(jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelConfig() {
        try {
            // Attempt to load from assets, fallback to hardcoded if not found
            val jsonText = try {
                context.assets.open("model_config.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }

            modelConfig = if (jsonText != null) {
                json.decodeFromString<ModelConfig>(jsonText)
            } else {
                createDefaultConfig()
            }
        } catch (e: Exception) {
            modelConfig = createDefaultConfig()
            e.printStackTrace()
        }
    }

    private fun createDefaultConfig() = ModelConfig(
        features = listOf(
            "LBXSAL", "LBXSCR", "LBXGLU", "LBXHSCRP", "LBXWBCSI", "LBXLYPCT", 
            "LBXMCVSI", "LBXRDW", "LBXSAPSI", "LBXGH", "LBXTC", "LBXTR", 
            "BMXBMI", "RIAGENDR", "LBDLYMNO", "LBXRBCSI"
        ),
        medianValues = mapOf(
            "LBXSAL" to 4.1f, "LBXSCR" to 0.85f, "LBXGLU" to 95f, "LBXHSCRP" to 1.5f,
            "LBXWBCSI" to 6.5f, "LBXLYPCT" to 30f, "LBXMCVSI" to 90f, "LBXRDW" to 13f,
            "LBXSAPSI" to 70f, "LBXGH" to 5.4f, "LBXTC" to 190f, "LBXTR" to 120f,
            "BMXBMI" to 26f, "RIAGENDR" to 1f, "LBDLYMNO" to 2.0f, "LBXRBCSI" to 4.8f
        )
    )

    fun predictBiologicalAge(biomarkers: Map<String, Float>): BioAgeResult {
        val session = ortSession ?: return BioAgeResult(0f, "ORT_SESSION_NOT_INITIALIZED", emptyList())
        val config = modelConfig ?: return BioAgeResult(0f, "MODEL_CONFIG_NOT_LOADED", emptyList())
        
        val features = config.features
        val medianValues = config.medianValues

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
        
        val drivers = generateShapDrivers(biomarkers, config)
        val explanation = buildNaturalExplanation(predictedAge, drivers)

        return BioAgeResult(
            biologicalAge = predictedAge,
            explanation = explanation,
            keyDrivers = drivers
        )
    }

    private fun generateShapDrivers(biomarkers: Map<String, Float>, config: ModelConfig): List<Driver> {
        val drivers = mutableListOf<Driver>()
        val globalImp = shapData?.global_importance ?: emptyMap()
        val features = config.features
        val medianValues = config.medianValues

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

    fun getFeatures(): List<String> = modelConfig?.features ?: emptyList()
}
