package com.neon.ascent.feature.codex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.datastore.HealthPreferencesDataStore
import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.dao.BodySampleDao
import com.neon.ascent.core.data.local.entity.BodySampleEntity
import com.neon.ascent.core.data.local.entity.DailyVitalRollupEntity
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.data.local.dao.InsightDao
import com.neon.ascent.core.domain.codex.models.BiomarkerKeys
import com.neon.ascent.core.domain.codex.models.BiomarkerSample
import com.neon.ascent.core.domain.codex.models.BiomarkerStatus
import com.neon.ascent.core.domain.repository.BiomarkerRepository
import com.neon.ascent.core.domain.workout.models.Exercise
import com.neon.ascent.core.domain.workout.models.ExerciseAccomplishments
import com.neon.ascent.core.domain.workout.models.RecoveryScore
import com.neon.ascent.core.domain.workout.models.SetType
import com.neon.ascent.core.domain.workout.models.UserWorkoutProfile
import com.neon.ascent.core.domain.workout.models.WorkoutLog
import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.rules.CyberCrappRules
import com.neon.ascent.core.domain.backup.models.BackupScope
import com.neon.ascent.core.domain.repository.FullDataBackupRepository
import com.neon.ascent.core.domain.workout.models.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class CodexWing {
    OPS_LOG, VITALS, SERUM, CHRONICLE
}

enum class CodexPeriod(val label: String, val days: Int?) {
    SEVEN_DAYS("7D", 7),
    THIRTY_DAYS("30D", 30),
    NINETY_DAYS("90D", 90),
    YTD("YTD", null),
    ALL("ALL", null)
}

data class CodexUiState(
    val activeWing: CodexWing = CodexWing.OPS_LOG,
    val selectedPeriod: CodexPeriod = CodexPeriod.THIRTY_DAYS,
    val sessionCount: Int = 0,
    val weekCount: Int = 0,
    val monthCount: Int = 0,
    val yearCount: Int = 0,
    val totalVolume: Long = 0,
    val streak: Int = 0,
    val sessionSummaries: List<SessionSummary> = emptyList(),
    val prs: List<PrDisplayData> = emptyList(),
    val muscleFrequency: Map<String, Int> = emptyMap(),
    val hitRate: Float = 0f,
    val isLoading: Boolean = false,
    
    // Vitals State
    val vitalsType: VitalsType = VitalsType.HRV,
    val vitalsData: List<VitalsPoint> = emptyList(),
    val bpDualPoints: List<DualVitalsPoint> = emptyList(),
    val bodyEntries: List<BodyEntryItem> = emptyList(),
    val selectedBpPosition: String = "SITTING", // "SITTING" or "STANDING"
    val bodyPartMeasurements: List<BodyPartMeasurementSection> = emptyList(),
    val hasNutritionPermission: Boolean = true,
    val availableBfMethods: List<String> = emptyList(),
    val selectedBfMethod: String? = null,
    val fuelHistory: List<com.neon.ascent.core.domain.workout.models.FuelSnapshot> = emptyList(),
    val latestInsight: String? = null,
    val recoveryScore: RecoveryScore? = null,

    // Serum State
    val latestBiomarkers: List<BiomarkerStatus> = emptyList(),
    val selectedMarkerKey: String? = null,
    val selectedMarkerHistory: List<BiomarkerSample> = emptyList(),

    // Dossier State
    val selectedExerciseId: String? = null,
    val dossier: ExerciseDossier? = null,
    val exerciseSearchQuery: String = "",
    val availableExercises: List<Exercise> = emptyList(),
    val periodExerciseIds: Set<String> = emptySet(),
    val userProfile: UserWorkoutProfile? = null
)

enum class VitalsType(val label: String, val rollupMetric: String) {
    SANCTUM("SANCTUM", "SANCTUM"),
    STEPS("STEPS", "STEPS"),
    RHR("RHR", "RHR"),
    HRV("HRV", "HRV_RMSSD"),
    HRV_NIGHT("HRV_NIGHT", "HRV_NIGHT"),
    HR_LOAD("HR_LOAD", "HR_LOAD_MIN"),
    SLEEP_MIN("SLEEP", "SLEEP_MIN"),
    KCAL_TOTAL("KCAL_TOTAL", "KCAL_TOTAL"),
    KCAL_EATEN("KCAL_EATEN", "KCAL_EATEN"),
    WEIGHT("WEIGHT", "WEIGHT"),
    BF_PCT("BF_PCT", "BF_PCT"),
    BODY_MEASUREMENTS("BODY_MEASUREMENTS", "TAPE_WAIST_NAVEL"),
    BLOOD_PRESSURE("BLOOD_PRESSURE", "BP_SYS_SIT")
}

data class BodyPartMeasurementSection(
    val siteKey: String,
    val displayName: String,
    val data: List<VitalsPoint>
)

data class DualVitalsPoint(
    val date: LocalDate,
    val primaryValue: Double,
    val secondaryValue: Double
)

data class BodyEntryItem(
    val id: String,
    val date: LocalDate,
    val timeStr: String,
    val primaryValue: Double,
    val secondaryValue: Double? = null,
    val displayValue: String,
    val subtitle: String? = null
)

data class VitalsPoint(
    val date: LocalDate,
    val value: Double
)

data class SessionSummary(
    val date: LocalDate,
    val isDeload: Boolean,
    val volume: Long = 0,
    val protocol: com.neon.ascent.core.domain.workout.models.WorkoutProtocol? = null,
    val primaryAugmentName: String? = null,
    val dayType: com.neon.ascent.core.domain.workout.models.ProtocolDayType? = null
)

data class ExerciseDossier(
    val exercise: Exercise,
    val accomplishments: ExerciseAccomplishments?,
    val sessions: List<DossierSession>,
    val weightSeries: List<StepPoint>,
    val widowmakerSeries: List<StepPoint> = emptyList(),
    val repRange: com.neon.ascent.core.domain.workout.rules.RepRange,
    val nextIncrementCopy: String? = null,
    val isStalled: Boolean = false
)

data class DossierSession(
    val date: LocalDate,
    val weight: Float,
    val displaySummary: String,
    val rir: Int?,
    val isWidowmaker: Boolean = false,
    val isDeload: Boolean = false
)

data class StepPoint(
    val date: LocalDate,
    val value: Float,
    val reps: Int,
    val isDeload: Boolean = false
)

data class PrDisplayData(
    val exerciseId: String,
    val exerciseName: String,
    val heaviestWeight: Float,
    val heaviestWeightReps: Int,
    val heaviestWeightDate: Instant?,
    val bestClusterReps: Int,
    val bestClusterWeight: Float,
    val bestClusterDate: Instant?
)

@HiltViewModel
class CodexViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val biomarkerRepository: BiomarkerRepository,
    private val rollupDao: DailyVitalRollupDao,
    private val bodySampleDao: BodySampleDao,
    private val dataStore: HealthPreferencesDataStore,
    private val insightDao: InsightDao,
    private val healthManager: HealthManager,
    private val fullDataBackupRepository: FullDataBackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodexUiState())
    val uiState: StateFlow<CodexUiState> = _uiState.asStateFlow()

    private val _exportJsonEvent = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val exportJsonEvent = _exportJsonEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                dataStore.codexPeriod,
                dataStore.codexWing,
                dataStore.codexLastExerciseId
            ) { periodName, wingName, lastExerciseId ->
                Triple(periodName, wingName, lastExerciseId)
            }.collect { (periodName, wingName, lastExerciseId) ->
                val period = runCatching { CodexPeriod.valueOf(periodName) }.getOrDefault(CodexPeriod.THIRTY_DAYS)
                val wing = runCatching { CodexWing.valueOf(wingName) }.getOrDefault(CodexWing.OPS_LOG)
                
                _uiState.update { it.copy(
                    selectedPeriod = period,
                    activeWing = wing,
                    selectedExerciseId = lastExerciseId
                ) }

                loadCodexData(period)
                loadVitalsData(period, _uiState.value.vitalsType)
                loadFuelHistory(period)
                if (lastExerciseId != null) {
                    loadDossier(lastExerciseId)
                }
            }
        }
        loadPrs()
        loadExercises()
        loadUserProfile()
        loadLatestInsight()
        loadRecoveryScore()
        loadSerumData()
    }

    private fun loadSerumData() {
        viewModelScope.launch {
            biomarkerRepository.getLatestPerMarker().collect { latestSamples ->
                val statuses = latestSamples.map { latest ->
                    biomarkerRepository.getSamplesForMarker(latest.markerKey).first().let { history ->
                        val previous = history.getOrNull(1)
                        BiomarkerStatus(
                            latest = latest,
                            previous = previous,
                            delta = if (previous != null) latest.value - previous.value else null,
                            history = history
                        )
                    }
                }
                _uiState.update { it.copy(latestBiomarkers = statuses) }
            }
        }
    }

    fun selectWing(wing: CodexWing) {
        viewModelScope.launch {
            dataStore.setCodexWing(wing.name)
        }
    }

    fun selectPeriod(period: CodexPeriod) {
        viewModelScope.launch {
            dataStore.setCodexPeriod(period.name)
        }
    }

    fun selectMarker(markerKey: String?) {
        _uiState.update { it.copy(selectedMarkerKey = markerKey) }
        if (markerKey != null) {
            viewModelScope.launch {
                biomarkerRepository.getSamplesForMarker(markerKey).collect { history ->
                    _uiState.update { it.copy(selectedMarkerHistory = history) }
                }
            }
        }
    }

    fun exportLogs(scope: BackupScope = BackupScope()) {
        viewModelScope.launch {
            val json = fullDataBackupRepository.exportBackupJson(scope)
            _exportJsonEvent.emit(json)
        }
    }

    fun addBiomarkerSample(
        key: String,
        name: String,
        value: Double,
        unit: String,
        date: Instant,
        notes: String?
    ) {
        viewModelScope.launch {
            val sample = BiomarkerSample(
                id = java.util.UUID.randomUUID().toString(),
                markerKey = key,
                displayName = name,
                value = value,
                unit = unit,
                drawnAt = date,
                source = "MANUAL",
                notes = notes
            )
            biomarkerRepository.saveSample(sample)
        }
    }

    fun deleteBiomarkerSample(id: String) {
        viewModelScope.launch {
            biomarkerRepository.deleteSample(id)
        }
    }

    fun selectExercise(exerciseId: String?) {
        viewModelScope.launch {
            dataStore.setCodexLastExerciseId(exerciseId)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(exerciseSearchQuery = query) }
    }

    fun selectVitalsType(type: VitalsType) {
        _uiState.update { it.copy(vitalsType = type) }
        loadVitalsData(_uiState.value.selectedPeriod, type)
    }

    fun selectBpPosition(position: String) {
        _uiState.update { it.copy(selectedBpPosition = position) }
        loadVitalsData(_uiState.value.selectedPeriod, _uiState.value.vitalsType)
    }

    fun addBodySample(
        metric: String,
        value: Double,
        unit: String,
        date: LocalDate,
        time: LocalTime = LocalTime.now(),
        method: String? = null,
        position: String? = null,
        note: String? = null
    ) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val loggedAt = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
            val sample = BodySampleEntity(
                localDate = date.toString(),
                loggedAt = loggedAt,
                metric = metric,
                value = value,
                unit = unit,
                method = method,
                position = position,
                source = "NEON",
                note = note
            )
            bodySampleDao.upsertSample(sample)
            recalculateRollupForDate(date.toString())
            loadVitalsData(_uiState.value.selectedPeriod, _uiState.value.vitalsType)
        }
    }

    fun addBloodPressureSample(
        systolicMmHg: Double,
        diastolicMmHg: Double,
        position: String,
        date: LocalDate,
        time: LocalTime = LocalTime.now(),
        note: String? = null
    ) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val loggedAt = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
            val sysSample = BodySampleEntity(
                localDate = date.toString(),
                loggedAt = loggedAt,
                metric = "BP_SYS",
                value = systolicMmHg,
                unit = "MMHG",
                position = position,
                source = "NEON",
                note = note
            )
            val diaSample = BodySampleEntity(
                localDate = date.toString(),
                loggedAt = loggedAt,
                metric = "BP_DIA",
                value = diastolicMmHg,
                unit = "MMHG",
                position = position,
                source = "NEON",
                note = note
            )
            bodySampleDao.upsertSamples(listOf(sysSample, diaSample))
            recalculateRollupForDate(date.toString())
            loadVitalsData(_uiState.value.selectedPeriod, _uiState.value.vitalsType)
        }
    }

    fun deleteBodySample(sampleId: String) {
        viewModelScope.launch {
            val sample = bodySampleDao.getSamplesBetween(0, Long.MAX_VALUE).find { it.id == sampleId }
            bodySampleDao.deleteSample(sampleId)
            if (sample != null) {
                recalculateRollupForDate(sample.localDate)
            }
            loadVitalsData(_uiState.value.selectedPeriod, _uiState.value.vitalsType)
        }
    }

    private suspend fun recalculateRollupForDate(localDate: String) {
        val samples = bodySampleDao.getSamplesForDayList(localDate)
        val nowMillis = System.currentTimeMillis()

        val weightSample = samples.filter { it.metric == "WEIGHT" }.maxByOrNull { it.loggedAt }
        if (weightSample != null) {
            rollupDao.upsert(
                DailyVitalRollupEntity(
                    localDate = localDate,
                    metric = "WEIGHT",
                    value = weightSample.value,
                    source = weightSample.source,
                    quality = "OK",
                    updatedAt = nowMillis
                )
            )
        }

        val bfSamples = samples.filter { it.metric == "BF_PCT" }
        if (bfSamples.isNotEmpty()) {
            val latestOverall = bfSamples.maxByOrNull { it.loggedAt }
            if (latestOverall != null) {
                rollupDao.upsert(
                    DailyVitalRollupEntity(
                        localDate = localDate,
                        metric = "BF_PCT",
                        value = latestOverall.value,
                        source = latestOverall.source,
                        quality = "OK",
                        updatedAt = nowMillis
                    )
                )
            }
            bfSamples.groupBy { it.method }.forEach { (method, methodGroup) ->
                if (!method.isNullOrBlank() && methodGroup.isNotEmpty()) {
                    val latestMethod = methodGroup.maxByOrNull { it.loggedAt }
                    if (latestMethod != null) {
                        rollupDao.upsert(
                            DailyVitalRollupEntity(
                                localDate = localDate,
                                metric = "BF_PCT_${method}",
                                value = latestMethod.value,
                                source = latestMethod.source,
                                quality = "OK",
                                updatedAt = nowMillis
                            )
                        )
                    }
                }
            }
        }

        val bpSysSamples = samples.filter { it.metric == "BP_SYS" }
        val bpDiaSamples = samples.filter { it.metric == "BP_DIA" }
        listOf("SITTING", "STANDING", "LYING").forEach { posture ->
            val postfix = when (posture) {
                "SITTING" -> "_SIT"
                "STANDING" -> "_STAND"
                else -> "_LYING"
            }
            val sys = bpSysSamples.filter { it.position == posture }.maxByOrNull { it.loggedAt }
            val dia = bpDiaSamples.filter { it.position == posture }.maxByOrNull { it.loggedAt }
            if (sys != null) {
                rollupDao.upsert(
                    DailyVitalRollupEntity(
                        localDate = localDate,
                        metric = "BP_SYS${postfix}",
                        value = sys.value,
                        source = sys.source,
                        quality = "OK",
                        updatedAt = nowMillis
                    )
                )
            }
            if (dia != null) {
                rollupDao.upsert(
                    DailyVitalRollupEntity(
                        localDate = localDate,
                        metric = "BP_DIA${postfix}",
                        value = dia.value,
                        source = dia.source,
                        quality = "OK",
                        updatedAt = nowMillis
                    )
                )
            }
        }
    }

    private fun loadFuelHistory(period: CodexPeriod) {
        val (start, end) = getRangeForPeriod(period)
        viewModelScope.launch {
            workoutRepository.getFuelHistory(start, end).collect { history ->
                if (history.isEmpty() && (period == CodexPeriod.THIRTY_DAYS || period == CodexPeriod.ALL)) {
                    backfillBaseline()
                }
                _uiState.update { it.copy(fuelHistory = history) }
            }
        }
    }

    private fun backfillBaseline() {
        viewModelScope.launch {
            val profile = workoutRepository.getUserProfile("default_user").first() ?: return@launch
            val macros = com.neon.ascent.core.domain.workout.rules.MacroCalculator.calculateMacros(profile)
            workoutRepository.saveFuelSnapshot(
                com.neon.ascent.core.domain.workout.models.FuelSnapshot(
                    weightKg = profile.weightKg,
                    tdee = macros.calories,
                    protein = macros.protein,
                    carb = macros.carbs,
                    fat = macros.fat,
                    activityFactor = profile.activityFactor,
                    somatotype = profile.somatotype,
                    timestamp = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS)
                )
            )
        }
    }

    private fun loadRecoveryScore() {
        viewModelScope.launch {
            workoutRepository.getRecoveryScore().collect { score ->
                _uiState.update { it.copy(recoveryScore = score) }
            }
        }
    }

    private fun loadLatestInsight() {
        viewModelScope.launch {
            insightDao.getLatestInsight().collect { insight ->
                _uiState.update { it.copy(latestInsight = insight?.content) }
            }
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(hasNutritionPermission = healthManager.hasNutritionPermission()) }
        }
    }

    suspend fun getPermissionsToRequest(): Set<String> {
        return healthManager.getPermissionsToRequest()
    }

    fun getPermissionRationale(): Map<String, String> {
        return healthManager.getPermissionRationale()
    }

    fun selectBfMethod(method: String) {
        _uiState.update { it.copy(selectedBfMethod = method) }
        loadVitalsData(_uiState.value.selectedPeriod, _uiState.value.vitalsType)
    }

    private fun loadVitalsData(period: CodexPeriod, type: VitalsType) {
        val (start, end) = getRangeForPeriod(period)
        val zone = ZoneId.systemDefault()
        val startDate = start.atZone(zone).toLocalDate().toString()
        val endDate = end.atZone(zone).toLocalDate().toString()

        viewModelScope.launch {
            val hasNutr = healthManager.hasNutritionPermission()
            _uiState.update { it.copy(hasNutritionPermission = hasNutr) }

            if (type == VitalsType.BF_PCT) {
                // Query all BF samples to find available methods and the last-used method
                val bfSamples = bodySampleDao.getSamplesForMetricRange(
                    metric = "BF_PCT",
                    fromDate = startDate,
                    toDate = endDate
                )
                val methods = bfSamples.mapNotNull { it.method }.filter { it.isNotBlank() }.distinct()
                val activeMethod = _uiState.value.selectedBfMethod?.takeIf { it in methods }
                    ?: bfSamples.maxByOrNull { it.loggedAt }?.method
                    ?: methods.firstOrNull()

                _uiState.update { 
                    it.copy(
                        availableBfMethods = methods,
                        selectedBfMethod = activeMethod
                    ) 
                }

                val samplesFlow = bodySampleDao.getSamplesForMetricRangeFlow("BF_PCT", startDate, endDate)
                val metricQuery = if (activeMethod != null) "BF_PCT_${activeMethod}" else "BF_PCT"
                val rollupFlow = rollupDao.getRange(metricQuery, startDate, endDate)

                combine(rollupFlow, samplesFlow) { rollups, samples ->
                    val targetRollups = if (rollups.isEmpty() && activeMethod != null) {
                        rollupDao.getRangeList("BF_PCT", startDate, endDate)
                    } else {
                        rollups
                    }
                    val points = targetRollups.map { VitalsPoint(LocalDate.parse(it.localDate), it.value) }
                    val filteredSamples = if (activeMethod != null) {
                        samples.filter { it.method == activeMethod }
                    } else {
                        samples
                    }
                    val entries = filteredSamples.map { s ->
                        val date = LocalDate.parse(s.localDate)
                        val timeInstant = Instant.ofEpochMilli(s.loggedAt)
                        val timeStr = timeInstant.atZone(zone).toLocalTime().format(
                            DateTimeFormatter.ofPattern("h:mm a"))
                        BodyEntryItem(
                            id = s.id,
                            date = date,
                            timeStr = timeStr,
                            primaryValue = s.value,
                            displayValue = String.format(Locale.US, "%.1f%%", s.value),
                            subtitle = listOfNotNull(s.method?.let { "Method: $it" }, s.conditionTag, s.source.takeIf { it != "NEON" }).joinToString(" • ").ifBlank { null }
                        )
                    }.sortedByDescending { it.date }
                    Pair(points, entries)
                }.collect { (points, entries) ->
                    _uiState.update {
                        it.copy(
                            vitalsData = points,
                            bodyEntries = entries,
                            bpDualPoints = emptyList(),
                            bodyPartMeasurements = emptyList()
                        )
                    }
                }
            } else if (type == VitalsType.BODY_MEASUREMENTS) {
                val knownSiteOrder = listOf(
                    "WAIST_NAVEL" to "WAIST (NAVEL)",
                    "WAIST_NARROW" to "WAIST (NARROW)",
                    "CHEST" to "CHEST",
                    "NECK" to "NECK",
                    "HIPS" to "HIPS",
                    "BICEP" to "BICEP",
                    "THIGH" to "THIGH",
                    "CALF" to "CALF",
                    "FOREARM" to "FOREARM",
                    "SHOULDERS" to "SHOULDERS"
                )

                rollupDao.getTapeRollups(startDate, endDate).collect { rollups ->
                    val grouped = rollups.groupBy { it.metric.removePrefix("TAPE_") }
                    val sections = mutableListOf<BodyPartMeasurementSection>()

                    // First add known sites in order if they exist
                    for ((siteKey, displayName) in knownSiteOrder) {
                        val siteRollups = grouped[siteKey]
                        if (!siteRollups.isNullOrEmpty()) {
                            val pts = siteRollups.map { VitalsPoint(LocalDate.parse(it.localDate), it.value) }
                            sections.add(BodyPartMeasurementSection(siteKey, displayName, pts))
                        }
                    }

                    // Any remaining tape metrics
                    for ((siteKey, siteRollups) in grouped) {
                        if (knownSiteOrder.none { it.first == siteKey } && siteRollups.isNotEmpty()) {
                            val pts = siteRollups.map { VitalsPoint(LocalDate.parse(it.localDate), it.value) }
                            sections.add(BodyPartMeasurementSection(siteKey, siteKey.replace('_', ' '), pts))
                        }
                    }

                    val firstPoints = sections.firstOrNull()?.data ?: emptyList()
                    _uiState.update {
                        it.copy(
                            bodyPartMeasurements = sections,
                            vitalsData = firstPoints,
                            bpDualPoints = emptyList(),
                            bodyEntries = emptyList()
                        )
                    }
                }
            } else if (type == VitalsType.BLOOD_PRESSURE) {
                val position = _uiState.value.selectedBpPosition
                val postfix = if (position == "STANDING") "_STAND" else "_SIT"
                val sysFlow = rollupDao.getRange("BP_SYS$postfix", startDate, endDate)
                val diaFlow = rollupDao.getRange("BP_DIA$postfix", startDate, endDate)
                val sysSamplesFlow = bodySampleDao.getSamplesForMetricRangeFlow("BP_SYS", startDate, endDate)
                val diaSamplesFlow = bodySampleDao.getSamplesForMetricRangeFlow("BP_DIA", startDate, endDate)

                combine(sysFlow, diaFlow, sysSamplesFlow, diaSamplesFlow) { sysList, diaList, sysSamples, diaSamples ->
                    val sysMap = sysList.associate { it.localDate to it.value }
                    val diaMap = diaList.associate { it.localDate to it.value }
                    val allDates = (sysMap.keys + diaMap.keys).sorted()
                    val dualPoints = allDates.mapNotNull { d ->
                        val s = sysMap[d]
                        val dia = diaMap[d]
                        if (s != null && dia != null) {
                            DualVitalsPoint(LocalDate.parse(d), s, dia)
                        } else null
                    }

                    // Build entry history items
                    val filteredSys = sysSamples.filter { it.position == position || (position == "SITTING" && it.position == null) }
                    val entries = filteredSys.map { sys ->
                        val matchingDia = diaSamples.find { it.localDate == sys.localDate && Math.abs(it.loggedAt - sys.loggedAt) < 60000 }
                        val diaVal = matchingDia?.value ?: 0.0
                        val date = LocalDate.parse(sys.localDate)
                        val timeInstant = Instant.ofEpochMilli(sys.loggedAt)
                        val timeStr = timeInstant.atZone(zone).toLocalTime().format(
                            DateTimeFormatter.ofPattern("h:mm a"))
                        BodyEntryItem(
                            id = sys.id,
                            date = date,
                            timeStr = timeStr,
                            primaryValue = sys.value,
                            secondaryValue = diaVal,
                            displayValue = "${sys.value.toInt()}/${diaVal.toInt()} mmHg",
                            subtitle = sys.conditionTag ?: sys.source
                        )
                    }.sortedByDescending { it.date }

                    Pair(dualPoints, entries)
                }.collect { (dualPoints, entries) ->
                    _uiState.update {
                        it.copy(
                            bpDualPoints = dualPoints,
                            bodyEntries = entries,
                            vitalsData = emptyList(),
                            bodyPartMeasurements = emptyList()
                        )
                    }
                }
            } else if (type == VitalsType.WEIGHT) {
                val samplesFlow = bodySampleDao.getSamplesForMetricRangeFlow("WEIGHT", startDate, endDate)
                val rollupFlow = rollupDao.getRange("WEIGHT", startDate, endDate)

                combine(rollupFlow, samplesFlow) { rollups, samples ->
                    val points = rollups.map { VitalsPoint(LocalDate.parse(it.localDate), it.value) }
                    val entries = samples.map { s ->
                        val date = LocalDate.parse(s.localDate)
                        val timeInstant = Instant.ofEpochMilli(s.loggedAt)
                        val timeStr = timeInstant.atZone(zone).toLocalTime().format(
                            DateTimeFormatter.ofPattern("h:mm a"))
                        val isImp = _uiState.value.userProfile?.unitSystem == UnitSystem.IMPERIAL
                        val disp = if (isImp) {
                            String.format(Locale.US, "%.1f lbs", s.value * 2.20462262)
                        } else {
                            String.format(Locale.US, "%.1f kg", s.value)
                        }
                        BodyEntryItem(
                            id = s.id,
                            date = date,
                            timeStr = timeStr,
                            primaryValue = s.value,
                            displayValue = disp,
                            subtitle = listOfNotNull(s.conditionTag, s.source.takeIf { it != "NEON" }).joinToString(" • ").ifBlank { null }
                        )
                    }.sortedByDescending { it.date }
                    Pair(points, entries)
                }.collect { (points, entries) ->
                    _uiState.update {
                        it.copy(
                            vitalsData = points,
                            bodyEntries = entries,
                            bpDualPoints = emptyList(),
                            bodyPartMeasurements = emptyList()
                        )
                    }
                }
            } else {
                rollupDao.getRange(type.rollupMetric, startDate, endDate).collect { list ->
                    val points = list
                        .filter { 
                            when (type) {
                                VitalsType.KCAL_EATEN, VitalsType.SANCTUM, VitalsType.HR_LOAD -> it.value > 0.0
                                else -> true
                            }
                        }
                        .map { VitalsPoint(LocalDate.parse(it.localDate), it.value) }
                    _uiState.update { 
                        it.copy(
                            vitalsData = points, 
                            bodyPartMeasurements = emptyList(),
                            bpDualPoints = emptyList(),
                            bodyEntries = emptyList()
                        ) 
                    }
                }
            }
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            workoutRepository.getUserProfile("default_user").collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            workoutRepository.getExerciseDefinitions().collect { exercises ->
                _uiState.update { it.copy(availableExercises = exercises) }
            }
        }
    }

    private fun loadCodexData(period: CodexPeriod) {
        val (start, end) = getRangeForPeriod(period)
        val zone = ZoneId.systemDefault()

        viewModelScope.launch {
            workoutRepository.getSessionsBetween(start, end).collect { sessions ->
                val totalVol = sessions.sumOf { (_, logs) ->
                    logs.sumOf { (_, sets) -> 
                        sets.sumOf { (it.weight * it.reps).toLong() }
                    }
                }
                
                val summaries = sessions.map { (session, logs) ->
                    val vol = logs.sumOf { (_, sets) -> 
                        sets.sumOf { (it.weight * it.reps).toLong() }
                    }
                    val augmentName = logs.firstOrNull { it.first.augmentId == session.primaryAugmentId }?.first?.augmentName
                        ?: logs.firstOrNull { it.first.augmentName != null }?.first?.augmentName
                    
                    SessionSummary(
                        date = session.date.atZone(zone).toLocalDate(), 
                        isDeload = session.isDeload, 
                        volume = vol,
                        protocol = session.protocol,
                        primaryAugmentName = augmentName,
                        dayType = session.protocolDayType
                    )
                }.sortedByDescending { it.date }

                val exerciseIds = sessions.flatMap { it.second }.map { it.first.exerciseId }.toSet()
                
                _uiState.update { it.copy(
                    sessionCount = sessions.size,
                    totalVolume = totalVol,
                    sessionSummaries = summaries,
                    periodExerciseIds = exerciseIds,
                    streak = calculateStreak(sessions.map { it.first.date })
                ) }
                calculateHitRate(summaries, period)
            }
        }

        val nineDaysAgo = Instant.now().minus(9, java.time.temporal.ChronoUnit.DAYS)
        viewModelScope.launch {
            workoutRepository.getMuscleGroupsHitBetween(nineDaysAgo, end).collect { groups ->
                val freq = groups.groupingBy { it }.eachCount()
                _uiState.update { it.copy(muscleFrequency = freq) }
            }
        }

        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant()
        val startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(zone).toInstant()
        val startOfYear = today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(zone).toInstant()

        viewModelScope.launch {
            combine(
                workoutRepository.countSessionsBetween(startOfWeek, end),
                workoutRepository.countSessionsBetween(startOfMonth, end),
                workoutRepository.countSessionsBetween(startOfYear, end)
            ) { week, month, year ->
                Triple(week, month, year)
            }.collect { (week, month, year) ->
                _uiState.update { it.copy(weekCount = week, monthCount = month, yearCount = year) }
            }
        }
    }

    private fun loadPrs() {
        viewModelScope.launch {
            combine(
                workoutRepository.getAllAccomplishments(),
                workoutRepository.getExerciseDefinitions()
            ) { allPrs, exercises ->
                val exerciseMap = exercises.associateBy { it.id }
                allPrs
                    .filter { it.heaviestWeightDate != null || it.bestClusterDate != null }
                    .sortedByDescending { 
                        maxOf(it.heaviestWeightDate ?: Instant.EPOCH, it.bestClusterDate ?: Instant.EPOCH) 
                    }
                    .take(12)
                    .map { pr ->
                        PrDisplayData(
                            exerciseId = pr.exerciseId,
                            exerciseName = exerciseMap[pr.exerciseId]?.name ?: "Unknown Exercise",
                            heaviestWeight = pr.heaviestWeight,
                            heaviestWeightReps = pr.heaviestWeightReps,
                            heaviestWeightDate = pr.heaviestWeightDate,
                            bestClusterReps = pr.bestClusterReps,
                            bestClusterWeight = pr.bestClusterWeight,
                            bestClusterDate = pr.bestClusterDate
                        )
                    }
            }.collect { displayPrs ->
                _uiState.update { it.copy(prs = displayPrs) }
            }
        }
    }

    private fun calculateStreak(dates: List<Instant>): Int {
        if (dates.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val sortedDates = dates.map { it.atZone(zone).toLocalDate() }
            .distinct()
            .sortedDescending()
        
        var currentStreak = 0
        val today = LocalDate.now()
        
        if (sortedDates.first() != today && sortedDates.first() != today.minusDays(1)) {
            return 0
        }

        var checkDate = sortedDates.first()
        for (date in sortedDates) {
            if (date == checkDate) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }
        return currentStreak
    }

    private fun calculateHitRate(summaries: List<SessionSummary>, period: CodexPeriod) {
        val profile = _uiState.value.userProfile ?: return
        if (profile.scheduledDays.isEmpty()) return

        val (start, end) = getRangeForPeriod(period)
        val zone = ZoneId.systemDefault()
        
        var expectedCount = 0
        var check = start.atZone(zone).toLocalDate()
        val endLocalDate = end.atZone(zone).toLocalDate()
        
        val scheduledDaysSet = profile.scheduledDays.map { it.dayOfWeek }.toSet()
        
        while (check.isBefore(endLocalDate) || check.isEqual(endLocalDate)) {
            if (scheduledDaysSet.contains(check.dayOfWeek.value)) {
                expectedCount++
            }
            check = check.plusDays(1)
        }

        if (expectedCount > 0) {
            val hitRate = (summaries.size.toFloat() / expectedCount).coerceIn(0f, 1f)
            _uiState.update { it.copy(hitRate = hitRate) }
        }
    }

    private fun loadDossier(exerciseId: String) {
        viewModelScope.launch {
            val range = getRangeForPeriod(CodexPeriod.ALL)
            val logsFlow = workoutRepository.getLogsForExerciseBetween(exerciseId, range.first, range.second)
            val accFlow = workoutRepository.getAccomplishments(exerciseId)
            val progressionFlow = workoutRepository.getProgressionState(exerciseId)
            val exercise = _uiState.value.availableExercises.find { it.id == exerciseId } ?: return@launch

            combine(logsFlow, accFlow, progressionFlow) { logs, accomplishments, progression ->
                processDossier(exercise, logs, accomplishments, progression)
            }.collect { dossier ->
                _uiState.update { it.copy(dossier = dossier) }
            }
        }
    }

    private fun processDossier(
        exercise: Exercise,
        logs: List<Pair<WorkoutLog, List<SetLog>>>,
        accomplishments: ExerciseAccomplishments?,
        progression: com.neon.ascent.core.domain.workout.models.ProgressionState?
    ): ExerciseDossier {
        val zone = ZoneId.systemDefault()
        val sessions = mutableListOf<DossierSession>()
        val mainWeightSeries = mutableListOf<StepPoint>()
        val widowmakerSeries = mutableListOf<StepPoint>()
        
        val isSquatQuad = exercise.movementType == com.neon.ascent.core.domain.workout.models.MovementType.QUAD_DOMINANT

        logs.reversed().forEach { (log, sets) ->
            val date = sets.firstOrNull()?.timestamp?.atZone(zone)?.toLocalDate() ?: return@forEach
            val completedSets = sets.filter { it.isCompleted }
            if (completedSets.isEmpty()) return@forEach

            val clusterSets = completedSets.filter { it.clusterMiniSetIndex != null }
            val widowmakerSet = completedSets.find { it.type == SetType.WIDOWMAKER }
            val heavySet = completedSets.find { it.type == SetType.NORMAL || it.type == SetType.FAILURE || it.type == SetType.POWER }

            val isProbablyDeload = clusterSets.isEmpty() && completedSets.count { it.type == SetType.NORMAL } >= 3

            if (clusterSets.isNotEmpty()) {
                val weight = clusterSets.first().weight
                val totalReps = clusterSets.sumOf { it.reps }
                val rir = clusterSets.mapNotNull { it.rir }.maxOrNull()
                sessions.add(DossierSession(date, weight, "$totalReps reps (Cluster)", rir, isDeload = isProbablyDeload))
                mainWeightSeries.add(StepPoint(date, weight, totalReps, isDeload = isProbablyDeload))
            } else if (isSquatQuad) {
                if (heavySet != null) {
                    mainWeightSeries.add(StepPoint(date, heavySet.weight, heavySet.reps, isDeload = isProbablyDeload))
                }
                if (widowmakerSet != null) {
                    widowmakerSeries.add(StepPoint(date, widowmakerSet.weight, widowmakerSet.reps, isDeload = isProbablyDeload))
                    sessions.add(DossierSession(date, widowmakerSet.weight, "${widowmakerSet.reps} reps (Widowmaker)", widowmakerSet.rir, isWidowmaker = true, isDeload = isProbablyDeload))
                }
                if (heavySet != null) {
                    sessions.add(DossierSession(date, heavySet.weight, "${heavySet.reps} reps (Heavy)", heavySet.rir, isDeload = isProbablyDeload))
                }
            } else {
                val topSet = completedSets.maxBy { it.weight }
                sessions.add(DossierSession(date, topSet.weight, "${topSet.reps} reps", topSet.rir, isDeload = isProbablyDeload))
                mainWeightSeries.add(StepPoint(date, topSet.weight, topSet.reps, isDeload = isProbablyDeload))
            }
        }

        val repRange = CyberCrappRules.getRepRange(exercise.movementType)
        
        val profile = _uiState.value.userProfile
        var incrementCopy: String? = null
        if (mainWeightSeries.isNotEmpty() && profile?.autoWeightIncrement == true) {
            val last = mainWeightSeries.last()
            if (last.reps >= repRange.max) {
                val isCompound = exercise.movementType.name.contains("COMPOUND") || 
                                 exercise.movementType == com.neon.ascent.core.domain.workout.models.MovementType.QUAD_DOMINANT || 
                                 exercise.movementType == com.neon.ascent.core.domain.workout.models.MovementType.DEADLIFT
                val increment = if (isCompound) profile.weightIncrementCompound else profile.weightIncrementIsolation
                incrementCopy = "Hit ${repRange.max} @ ${last.value.toInt()} -> next load ${last.value.toInt() + increment.toInt()}, min ${repRange.min}"
            }
        }

        return ExerciseDossier(
            exercise = exercise,
            accomplishments = accomplishments,
            sessions = sessions.sortedByDescending { it.date }.take(8),
            weightSeries = mainWeightSeries,
            widowmakerSeries = widowmakerSeries,
            repRange = repRange,
            nextIncrementCopy = incrementCopy,
            isStalled = (progression?.consecutiveMisses ?: 0) >= 2
        )
    }

    private fun getRangeForPeriod(period: CodexPeriod): Pair<Instant, Instant> {
        val now = Instant.now()
        val localNow = LocalDate.now()
        val zone = ZoneId.systemDefault()
        
        val start = when (period) {
            CodexPeriod.SEVEN_DAYS -> localNow.minusDays(7).atStartOfDay(zone).toInstant()
            CodexPeriod.THIRTY_DAYS -> localNow.minusDays(30).atStartOfDay(zone).toInstant()
            CodexPeriod.NINETY_DAYS -> localNow.minusDays(90).atStartOfDay(zone).toInstant()
            CodexPeriod.YTD -> localNow.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(zone).toInstant()
            CodexPeriod.ALL -> Instant.EPOCH
        }
        
        return start to now
    }
}
