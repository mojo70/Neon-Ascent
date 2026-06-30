package com.neon.ascent.feature.health.data.uplink

import android.util.Log
import com.neon.ascent.feature.health.data.HealthConnectManager
import com.neon.ascent.feature.health.domain.uplink.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    private val _syncStatus = MutableStateFlow(UplinkSyncStatus(provider, _status.value))
    override val syncStatus: StateFlow<UplinkSyncStatus> = _syncStatus.asStateFlow()

    private val _liveStream = MutableStateFlow<LiveBiometrics?>(null)
    override fun getLiveStream(): StateFlow<LiveBiometrics?> = _liveStream.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    private fun updateStatus(newStatus: UplinkStatus) {
        _status.value = newStatus
        _syncStatus.update { it.copy(currentStatus = newStatus) }
    }

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
                    updateStatus(UplinkStatus.Connected)
                } else {
                    updateStatus(UplinkStatus.PermissionRequired)
                }
            }
        }
    }

    override suspend fun fetchDeepMetrics(): DeepBiometrics {
        Log.d("HealthConnectUplink", "Fetching deep metrics from Health Connect")
        _syncStatus.update { it.copy(lastSyncAttempt = System.currentTimeMillis()) }
        
        return try {
            fetchDeepMetricsWithRetry()
        } catch (e: Exception) {
            handleSyncError(e)
            DeepBiometrics()
        }
    }

    private suspend fun fetchDeepMetricsWithRetry(maxAttempts: Int = 3, initialDelay: Long = 1000): DeepBiometrics {
        var currentDelay = initialDelay
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                if (!healthConnectManager.isAvailableAndHasPermissions()) {
                    Log.w("HealthConnectUplink", "Sync failed: No permissions")
                    updateStatus(UplinkStatus.PermissionRequired)
                    throw Exception("No Permission")
                }

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
                
                val now = System.currentTimeMillis()
                updateStatus(UplinkStatus.Connected)
                _syncStatus.update { 
                    it.copy(
                        lastSuccessfulSync = now,
                        lastError = null
                    )
                }
                Log.i("HealthConnectUplink", "Successfully synced deep metrics from Health Connect")

                return DeepBiometrics(
                    stepsToday = steps,
                    caloriesToday = calories,
                    sleepScore = sleepScore,
                    bodyBattery = null, // Health Connect doesn't have body battery natively
                    stressLevel = null,
                    vo2Max = snapshot.distance.sumOf { it.distance.inMeters }.takeIf { it > 0 }?.let { 45.0 }, // Mock VO2Max logic
                    lastSyncTimestamp = now
                )
            } catch (e: Exception) {
                lastException = e
                if (e.message == "No Permission") {
                    throw e
                }
            }

            if (attempt < (maxAttempts - 1)) {
                Log.w("HealthConnectUplink", "Sync attempt ${attempt + 1} failed, retrying in $currentDelay ms...")
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        throw lastException ?: Exception("Unknown sync failure")
    }

    private fun handleSyncError(e: Exception) {
        Log.e("HealthConnectUplink", "Deep metrics sync failed after retries", e)
        val errorMsg = e.message ?: "Unknown sync error"
        if (_status.value !is UplinkStatus.PermissionRequired) {
            updateStatus(UplinkStatus.Error(errorMsg))
        }
        _syncStatus.update { it.copy(lastError = errorMsg) }
    }

    override suspend fun authenticate() {
        Log.d("HealthConnectUplink", "Authenticating Health Connect")
        updateStatus(UplinkStatus.Authenticating)
        if (healthConnectManager.isAvailableAndHasPermissions()) {
            updateStatus(UplinkStatus.Connected)
        } else {
            updateStatus(UplinkStatus.PermissionRequired)
            _syncStatus.update { it.copy(lastError = "Permissions not granted") }
        }
    }

    override suspend fun disconnect() {
        Log.i("HealthConnectUplink", "Disconnecting Health Connect")
        updateStatus(UplinkStatus.Disconnected)
    }
}
