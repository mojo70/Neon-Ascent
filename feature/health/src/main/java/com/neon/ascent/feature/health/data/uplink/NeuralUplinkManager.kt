package com.neon.ascent.feature.health.data.uplink

import android.content.Context
import android.util.Log
import com.neon.ascent.core.data.local.dao.InsightDao
import com.neon.ascent.core.data.local.entity.BiometricEventEntity
import com.neon.ascent.core.data.processor.InsightProjectionProcessor
import com.neon.ascent.core.domain.health.models.VitalsSnapshot
import com.neon.ascent.feature.health.data.workers.HealthSyncWorker
import com.neon.ascent.feature.health.domain.uplink.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeuralUplinkManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val garminUplink: GarminUplink,
    private val healthConnectUplink: HealthConnectUplink,
    private val insightDao: InsightDao,
    private val insightProcessor: InsightProjectionProcessor,
    private val rollupWriter: VitalsRollupWriter
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _activeUplinks = MutableStateFlow<List<NeuralUplink>>(emptyList())
    val activeUplinks = _activeUplinks.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uplinkSyncStatuses: StateFlow<List<UplinkSyncStatus>> = _activeUplinks.flatMapLatest { uplinks ->
        if (uplinks.isEmpty()) flowOf(emptyList())
        else combine(uplinks.map { it.syncStatus }) { it.toList() }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _combinedVitalsSnapshot = MutableStateFlow<VitalsSnapshot?>(null)
    val combinedVitalsSnapshot = _combinedVitalsSnapshot.asStateFlow()

    // Keep for backward compatibility until all consumers move to Snapshot
    private val _combinedLiveMetrics = MutableStateFlow<LiveBiometrics?>(null)
    val combinedLiveMetrics = _combinedLiveMetrics.asStateFlow()

    private val _combinedDeepMetrics = MutableStateFlow<DeepBiometrics?>(null)
    val combinedDeepMetrics = _combinedDeepMetrics.asStateFlow()

    init {
        registerUplink(garminUplink)
        registerUplink(healthConnectUplink)
        
        startUplinkSync()
        
        // Auto-start BLE sync for providers that support it
        garminUplink.startBLESync()

        // Schedule periodic background sync
        HealthSyncWorker.schedulePeriodicSync(context)
    }

    fun registerUplink(uplink: NeuralUplink) {
        if (_activeUplinks.value.none { it.provider == uplink.provider }) {
            _activeUplinks.value += uplink
        }
    }

    private var syncJob: Job? = null

    /**
     * Start observing all registered uplinks and merging their data.
     */
    fun startUplinkSync() {
        syncJob?.cancel()
        
        // Merge Live and Deep streams into a single Snapshot
        syncJob = combine(
            garminUplink.getLiveStream(),
            healthConnectUplink.getLiveStream(),
            _combinedDeepMetrics
        ) { garminLive, hcLive, deep ->
            if (garminLive == null && hcLive == null && deep == null) return@combine null
            
            val now = System.currentTimeMillis()
            
            // Live HR logic: Garmin BLE wins only if < 30s old
            val liveHr = if (garminLive?.heartRate != null && (now - garminLive.timestamp) < 30000) {
                garminLive.heartRate
            } else {
                hcLive?.heartRate
            }

            // Source footer logic
            val sourceFooter = when {
                garminLive != null && hcLive != null -> "HC+GARMIN_HR"
                garminLive != null -> "GARMIN"
                else -> "HC"
            }

            VitalsSnapshot(
                steps = deep?.stepsToday ?: hcLive?.stepsToday ?: garminLive?.stepsToday,
                calories = deep?.caloriesToday ?: hcLive?.caloriesToday ?: garminLive?.caloriesToday,
                distance = deep?.vo2Max, // Placeholder if needed
                sleepDurationMinutes = deep?.sleepDurationMinutes,
                sleepScore = deep?.sleepScore,
                sleepStages = deep?.sleepStages ?: emptyMap(),
                hrvRmssd = deep?.hrvRmssd ?: garminLive?.heartRateVariability ?: hcLive?.heartRateVariability,
                restingHeartRate = deep?.restingHeartRate,
                liveHeartRate = liveHr,
                bodyBattery = deep?.bodyBattery,
                stressLevel = deep?.stressLevel,
                sourceFooter = sourceFooter,
                timestamp = now
            )
        }.onEach { snapshot ->
            _combinedVitalsSnapshot.value = snapshot
            // Backward compatibility
            snapshot?.let {
                _combinedLiveMetrics.value = LiveBiometrics(
                    heartRate = it.liveHeartRate,
                    stepsToday = it.steps,
                    caloriesToday = it.calories,
                    heartRateVariability = it.hrvRmssd,
                    timestamp = it.timestamp
                )
            }
        }.launchIn(scope)
    }

    suspend fun fetchAllDeepMetrics(): DeepBiometrics {
        val garminDeep = runWithRetry(garminUplink.provider.name) {
            if (garminUplink.status.value == UplinkStatus.Connected) {
                garminUplink.fetchDeepMetrics()
            } else null
        }
        
        val hcDeep = runWithRetry(healthConnectUplink.provider.name) {
            healthConnectUplink.fetchDeepMetrics()
        } ?: DeepBiometrics()

        // Merge logic based on VitalsCards.md:
        // Steps/Kcal/Dist: HC aggregate > Garmin only if HC empty
        // Sleep Duration/Stages: Garmin (if >0) > HC longest
        // Sleep Score: Garmin overallScore only
        // HRV: Latest HC RMSSD from last sleep window (or Garmin if we add it)
        // RHR: HC Aggregate/Latest
        
        val merged = DeepBiometrics(
            stepsToday = hcDeep.stepsToday ?: garminDeep?.stepsToday,
            caloriesToday = hcDeep.caloriesToday ?: garminDeep?.caloriesToday,
            sleepScore = garminDeep?.sleepScore, // Garmin only
            bodyBattery = garminDeep?.bodyBattery,
            stressLevel = garminDeep?.stressLevel,
            recoveryTimeMinutes = garminDeep?.recoveryTimeMinutes,
            trainingReadiness = garminDeep?.trainingReadiness,
            vo2Max = hcDeep.vo2Max ?: garminDeep?.vo2Max,
            restingHeartRate = hcDeep.restingHeartRate, // HC Aggregate/Latest
            hrvRmssd = hcDeep.hrvRmssd ?: garminDeep?.hrvRmssd,
            sleepDurationMinutes = if ((garminDeep?.sleepDurationMinutes ?: 0L) > 0) garminDeep?.sleepDurationMinutes else hcDeep.sleepDurationMinutes,
            sleepStages = garminDeep?.sleepStages ?: emptyMap(),
            lastSyncTimestamp = System.currentTimeMillis()
        )
        
        _combinedDeepMetrics.value = merged
        ingestDeepMetrics(merged)
        rollupWriter.writeTodayRollup(merged)
        
        // Force immediate snapshot update
        startUplinkSync() 
        
        return merged
    }

    private suspend fun <T> runWithRetry(
        label: String,
        maxAttempts: Int = 3,
        initialDelay: Long = 1000,
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelay
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) {
                    handleCentralError(label, e)
                    return null
                }
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return null
    }

    private fun handleCentralError(provider: String, error: Exception) {
        // Stop using Neural Brief for routine sync failures. Log only.
        Log.e("NeuralUplinkManager", "// SYNC_FAILURE // $provider: ${error.message ?: "Unknown Error"}")
    }

    private fun ingestDeepMetrics(metrics: DeepBiometrics) {
        scope.launch {
            val now = Instant.now()
            // Deep ingest (sleep score, body battery) writes events at most once per deep sync
            metrics.sleepScore?.let {
                insightDao.insertBiometricEvent(
                    BiometricEventEntity(
                        timestamp = now,
                        source = "DEEP_SYNC",
                        type = "SLEEP_SCORE",
                        value = it.toDouble()
                    )
                )
            }
            metrics.bodyBattery?.let {
                insightDao.insertBiometricEvent(
                    BiometricEventEntity(
                        timestamp = now,
                        source = "DEEP_SYNC",
                        type = "BODY_BATTERY",
                        value = it.toDouble()
                    )
                )
            }
            metrics.restingHeartRate?.let {
                insightDao.insertBiometricEvent(
                    BiometricEventEntity(
                        timestamp = now,
                        source = "DEEP_SYNC",
                        type = "RHR",
                        value = it.toDouble()
                    )
                )
            }
            // Trigger processor after DEEP ingestion only
            insightProcessor.processProjections()
        }
    }

    // Live HR/HRV ingestion STOPPED as per Prompt 2 requirements.
}
