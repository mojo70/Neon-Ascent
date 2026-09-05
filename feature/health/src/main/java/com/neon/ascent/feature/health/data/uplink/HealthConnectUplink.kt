package com.neon.ascent.feature.health.data.uplink

import android.util.Log
import com.neon.ascent.feature.health.data.HealthConnectManager
import com.neon.ascent.feature.health.domain.uplink.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

                val now = Instant.now()
                val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
                val sleepWindowStart = LocalDate.now().minusDays(1).atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant()
                
                // Use aggregates for totals to avoid double-counting
                val steps = healthConnectManager.aggregateSteps(startOfDay, now).takeIf { it > 0 }
                
                // Prioritize total calories, fallback to active
                var calories = healthConnectManager.aggregateTotalCaloriesKcal(startOfDay, now)
                if (calories <= 0.0) {
                    calories = healthConnectManager.aggregateActiveCaloriesKcal(startOfDay, now)
                }
                val caloriesVal = calories.takeIf { it > 0 }

                // Nutrition calories consumed today
                val caloriesConsumed = healthConnectManager.aggregateNutritionKcal(startOfDay, now)

                // RHR and HRV (latest today)
                val rhr = healthConnectManager.latestRestingHr(startOfDay, now)
                val hrv = healthConnectManager.latestHrvRmssd(startOfDay, now)
                
                // Sleep window: 18:00 local yesterday -> now
                val sleepSessions = healthConnectManager.sleepSessions(sleepWindowStart, now)
                val longestSession = sleepSessions.maxByOrNull { 
                    java.time.Duration.between(it.startTime, it.endTime).toMillis() 
                }
                val longestSleepMinutes = longestSession?.let { 
                    java.time.Duration.between(it.startTime, it.endTime).toMinutes() 
                }
                val sleepStages = longestSession?.let { 
                    healthConnectManager.parseSleepStages(it) 
                } ?: emptyMap()

                updateStatus(UplinkStatus.Connected)
                val syncTime = System.currentTimeMillis()
                _syncStatus.update { 
                    it.copy(
                        lastSuccessfulSync = syncTime,
                        lastError = null
                    )
                }
                Log.i("HealthConnectUplink", "Successfully synced deep metrics from Health Connect")

                return DeepBiometrics(
                    stepsToday = steps,
                    caloriesToday = caloriesVal,
                    caloriesConsumedToday = caloriesConsumed,
                    sleepScore = null, // HC does not provide scores
                    bodyBattery = null,
                    stressLevel = null,
                    vo2Max = null, // Deleted mock VO2
                    restingHeartRate = rhr,
                    hrvRmssd = hrv,
                    sleepDurationMinutes = longestSleepMinutes,
                    sleepStages = sleepStages,
                    lastSyncTimestamp = syncTime
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
        Log.d("HealthConnectUplink", "Refreshing Health Connect status...")
        updateStatus(UplinkStatus.Authenticating)
        
        // Re-check permissions
        val isLinked = healthConnectManager.isAvailableAndHasPermissions()
        
        if (isLinked) {
            updateStatus(UplinkStatus.Connected)
        } else {
            updateStatus(UplinkStatus.PermissionRequired)
            _syncStatus.update { it.copy(lastError = "Permissions incomplete") }
        }
    }

    override suspend fun disconnect() {
        Log.i("HealthConnectUplink", "Disconnecting Health Connect")
        updateStatus(UplinkStatus.Disconnected)
    }
}
