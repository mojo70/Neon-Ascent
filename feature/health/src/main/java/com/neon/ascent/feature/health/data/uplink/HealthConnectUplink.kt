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
                    heartRateVariability = metrics.heartRateVariability,
                    timestamp = System.currentTimeMillis()
                )
                
                // Automatically update status based on whether we're getting any data
                if (healthConnectManager.isAvailableAndHasPermissions()) {
                    _status.value = UplinkStatus.Connected
                } else {
                    _status.value = UplinkStatus.Disconnected
                }
            }
        }
    }

    override suspend fun fetchDeepMetrics(): DeepBiometrics {
        val snapshot = healthConnectManager.readRecentData(days = 1)
        
        // Map Health Connect records to DeepBiometrics
        val steps = snapshot.steps.sumOf { it.count }.takeIf { it > 0 }
        
        // Prioritize total calories (Active + Basal), fall back to just Active if total is empty
        val totalCals = snapshot.totalCalories.sumOf { it.energy.inKilocalories }
        val calories = if (totalCals > 0) totalCals else {
            snapshot.activeCalories.sumOf { it.energy.inKilocalories }.takeIf { it > 0 }
        }
        
        // Use a more realistic mapping for sleep score from records if possible
        val sleepScore = if (snapshot.sleep.isNotEmpty()) {
            // Placeholder: in a real app we might calculate this based on duration and stages
            val totalMinutes = snapshot.sleep.sumOf { 
                java.time.Duration.between(it.startTime, it.endTime).toMinutes() 
            }
            (totalMinutes / 4.8).toInt().coerceIn(0, 100) // 480 mins = 100%
        } else null

        val avgHRV = snapshot.hrv.map { it.heartRateVariabilityMillis }.average().takeIf { !it.isNaN() }
        
        return DeepBiometrics(
            stepsToday = steps,
            caloriesToday = calories,
            sleepScore = sleepScore,
            bodyBattery = null, // Health Connect doesn't have body battery natively
            stressLevel = null,
            vo2Max = snapshot.distance.sumOf { it.distance.inMeters }.takeIf { it > 0 }?.let { 45.0 }, // Mock VO2Max logic
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
