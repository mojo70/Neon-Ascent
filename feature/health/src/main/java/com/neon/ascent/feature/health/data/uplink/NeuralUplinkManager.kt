package com.neon.ascent.feature.health.data.uplink

import android.content.Context
import com.neon.ascent.core.data.local.dao.InsightDao
import com.neon.ascent.core.data.local.entity.BiometricEventEntity
import com.neon.ascent.core.data.processor.InsightProjectionProcessor
import com.neon.ascent.core.domain.notifications.BriefService
import com.neon.ascent.feature.health.data.workers.HealthSyncWorker
import com.neon.ascent.feature.health.domain.uplink.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val briefService: BriefService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _activeUplinks = MutableStateFlow<List<NeuralUplink>>(emptyList())
    val activeUplinks = _activeUplinks.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uplinkSyncStatuses: StateFlow<List<UplinkSyncStatus>> = _activeUplinks.flatMapLatest { uplinks ->
        if (uplinks.isEmpty()) flowOf(emptyList())
        else combine(uplinks.map { it.syncStatus }) { it.toList() }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

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

    /**
     * Start observing all registered uplinks and merging their data.
     */
    fun startUplinkSync() {
        // Merge Live streams
        // We prioritize Garmin for HR, but Health Connect usually has the most up-to-date steps
        combine(
            garminUplink.getLiveStream(),
            healthConnectUplink.getLiveStream()
        ) { garmin, hc ->
            if ((garmin == null) && (hc == null)) return@combine null
            
            LiveBiometrics(
                heartRate = garmin?.heartRate ?: hc?.heartRate,
                stepsToday = hc?.stepsToday ?: garmin?.stepsToday,
                caloriesToday = hc?.caloriesToday ?: garmin?.caloriesToday,
                heartRateVariability = garmin?.heartRateVariability ?: hc?.heartRateVariability,
                timestamp = System.currentTimeMillis()
            )
        }.onEach { merged ->
            _combinedLiveMetrics.value = merged
            if (merged != null) {
                ingestLiveMetrics(merged)
            }
        }.launchIn(scope)
    }

    private fun ingestLiveMetrics(metrics: LiveBiometrics) {
        scope.launch {
            val now = Instant.now()
            metrics.heartRate?.let {
                insightDao.insertBiometricEvent(
                    BiometricEventEntity(
                        timestamp = now,
                        source = "LIVE_STREAM",
                        type = "HEART_RATE",
                        value = it.toDouble()
                    )
                )
            }
            metrics.heartRateVariability?.let {
                insightDao.insertBiometricEvent(
                    BiometricEventEntity(
                        timestamp = now,
                        source = "LIVE_STREAM",
                        type = "HRV",
                        value = it.toDouble()
                    )
                )
            }
            // Trigger processor after ingestion
            insightProcessor.processProjections()
        }
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

        // Merge logic: Garmin provides Body Battery/Stress, HC provides Steps/VO2Max
        val merged = DeepBiometrics(
            stepsToday = hcDeep.stepsToday ?: garminDeep?.stepsToday,
            caloriesToday = hcDeep.caloriesToday ?: garminDeep?.caloriesToday,
            sleepScore = garminDeep?.sleepScore ?: hcDeep.sleepScore,
            bodyBattery = garminDeep?.bodyBattery,
            stressLevel = garminDeep?.stressLevel,
            recoveryTimeMinutes = garminDeep?.recoveryTimeMinutes,
            trainingReadiness = garminDeep?.trainingReadiness,
            vo2Max = hcDeep.vo2Max ?: garminDeep?.vo2Max,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        
        _combinedDeepMetrics.value = merged
        ingestDeepMetrics(merged)
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
        val message = "SYNC_FAILURE // $provider: ${error.message ?: "Unknown Error"}"
        briefService.showNeuralBrief(
            title = "UPLINK_ALERT",
            content = message,
            actions = listOf<BriefService.BriefAction>(
                BriefService.BriefAction("RE-LINK", BriefService.ACTION_OPEN_DECK, "")
            )
        )
        // Fallback to last known metrics if needed (already handled by returning null/empty from runWithRetry)
    }

    private fun ingestDeepMetrics(metrics: DeepBiometrics) {
        scope.launch {
            val now = Instant.now()
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
            // Trigger processor after ingestion
            insightProcessor.processProjections()
        }
    }
}
