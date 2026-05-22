package com.neon.ascent.feature.health.data.uplink

import com.neon.ascent.feature.health.data.HealthConnectManager
import com.neon.ascent.feature.health.domain.uplink.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectUplink @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) : NeuralUplink {

    override val provider: UplinkProvider = UplinkProvider.HEALTH_CONNECT

    private val _status = MutableStateFlow<UplinkStatus>(UplinkStatus.Disconnected)
    override val status: StateFlow<UplinkStatus> = _status.asStateFlow()

    private val _liveStream = MutableStateFlow<LiveBiometrics?>(null)
    override fun getLiveStream(): StateFlow<LiveBiometrics?> = _liveStream.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            healthConnectManager.liveMetricsFlow().collect { metrics ->
                _liveStream.value = LiveBiometrics(
                    heartRate = metrics.heartRate,
                    stepsToday = metrics.stepsToday,
                    caloriesToday = metrics.caloriesToday,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    override suspend fun fetchDeepMetrics(): DeepBiometrics {
        val snapshot = healthConnectManager.readRecentData(days = 1)
        
        // Map Health Connect records to DeepBiometrics
        return DeepBiometrics(
            vo2Max = 0.0, // HealthConnectManager doesn't fetch vo2Max yet in readRecentData
            stressLevel = null, // Health Connect doesn't have a standard stress record
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    override suspend fun authenticate() {
        _status.value = UplinkStatus.Authenticating
        if (healthConnectManager.isAvailableAndHasPermissions()) {
            _status.value = UplinkStatus.Connected
        } else {
            _status.value = UplinkStatus.Error("Permissions not granted")
        }
    }

    override suspend fun disconnect() {
        _status.value = UplinkStatus.Disconnected
    }
}
