package com.neon.ascent.feature.health.data.uplink

import com.neon.ascent.feature.health.domain.uplink.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeuralUplinkManager @Inject constructor(
    private val garminUplink: GarminUplink,
    private val healthConnectUplink: HealthConnectUplink
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _activeUplinks = MutableStateFlow<List<NeuralUplink>>(emptyList())
    val activeUplinks = _activeUplinks.asStateFlow()

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
            if (garmin == null && hc == null) return@combine null
            
            LiveBiometrics(
                heartRate = garmin?.heartRate ?: hc?.heartRate,
                stepsToday = hc?.stepsToday ?: garmin?.stepsToday,
                caloriesToday = hc?.caloriesToday ?: garmin?.caloriesToday,
                heartRateVariability = garmin?.heartRateVariability ?: hc?.heartRateVariability,
                timestamp = System.currentTimeMillis()
            )
        }.onEach { merged ->
            _combinedLiveMetrics.value = merged
        }.launchIn(scope)
    }

    suspend fun fetchAllDeepMetrics(): DeepBiometrics {
        val garminDeep = if (garminUplink.status.value == UplinkStatus.Connected) {
            garminUplink.fetchDeepMetrics()
        } else null
        
        val hcDeep = healthConnectUplink.fetchDeepMetrics()

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
        return merged
    }
}
