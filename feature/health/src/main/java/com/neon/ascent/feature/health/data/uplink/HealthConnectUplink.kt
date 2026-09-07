package com.neon.ascent.feature.health.data.uplink

import android.util.Log
import com.neon.ascent.feature.health.data.HealthConnectManager
import com.neon.ascent.feature.health.domain.uplink.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
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
        var lastException: Throwable? = null

        repeat(maxAttempts) { attempt ->
            try {
                if (!healthConnectManager.isAvailableAndHasPermissions()) {
                    Log.w("HealthConnectUplink", "Sync failed: No permissions")
                    updateStatus(UplinkStatus.PermissionRequired)
                    throw Exception("No Permission")
                }

                val now = Instant.now()
                val zone = ZoneId.systemDefault()
                val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
                val sleepWindowStart = LocalDate.now(zone).minusDays(1).atTime(18, 0).atZone(zone).toInstant()
                
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

                // RHR
                val rhr = healthConnectManager.latestRestingHr(startOfDay, now)
                
                // Winner Sleep Session in window [18:00 local yesterday -> now]
                val sleepSessions = healthConnectManager.sleepSessions(sleepWindowStart, now)
                val winnerSession = healthConnectManager.pickCoreNight(sleepSessions, zone)

                val tibMinutes = winnerSession?.let {
                    Duration.between(it.startTime, it.endTime).toMinutes()
                }

                val stageMap = winnerSession?.let {
                    healthConnectManager.stageMinutes(it)
                } ?: emptyMap()

                val asleepMinutes = if (winnerSession != null) {
                    val stagedAsleep = (stageMap["DEEP"] ?: 0) + (stageMap["LIGHT"] ?: 0) + (stageMap["REM"] ?: 0) + (stageMap["SLEEPING"] ?: 0)
                    if (stagedAsleep > 0) {
                        stagedAsleep.toLong()
                    } else {
                        val awakeSum = (stageMap["AWAKE"] ?: 0) + (stageMap["AWAKE_IN_BED"] ?: 0) + (stageMap["OUT_OF_BED"] ?: 0)
                        ((tibMinutes ?: 0L) - awakeSum).coerceAtLeast(0L)
                    }
                } else null

                // Source package tag
                val sourceTag = winnerSession?.let { session ->
                    val pkg = session.metadata.dataOrigin.packageName.lowercase()
                    when {
                        pkg.contains("garmin") -> "HC_GARMIN_WRITE"
                        pkg.contains("fitness") || pkg.contains("google") -> "HC_FIT"
                        pkg.contains("samsung") || pkg.contains("shealth") -> "HC_SAMSUNG"
                        pkg.contains("pixel") -> "HC_PIXEL"
                        else -> "HC"
                    }
                } ?: "HC"

                // Bounded HR and HRV queries for winner session
                val sessionHrSamples = if (winnerSession != null) {
                    healthConnectManager.heartRateSamples(winnerSession.startTime, winnerSession.endTime)
                } else emptyList()

                val eveningHrSamples = if (winnerSession != null) {
                    healthConnectManager.heartRateSamples(winnerSession.startTime.minusSeconds(5400), winnerSession.startTime)
                } else emptyList()

                val sessionHrvSamples = if (winnerSession != null) {
                    healthConnectManager.hrvRmssdSamples(winnerSession.startTime, winnerSession.endTime)
                } else emptyList()

                // Night HRV mean (from session window, fallback to latest today if no session HRV samples)
                val nightHrv = if (sessionHrvSamples.isNotEmpty()) {
                    sessionHrvSamples.map { it.second }.average()
                } else {
                    healthConnectManager.latestHrvRmssd(startOfDay, now)
                }

                // Parsed stages for UI (DEEP, LIGHT, REM, AWAKE)
                val uiSleepStages = winnerSession?.let {
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
                    sleepScore = null, // HC does not provide scores. minutes/4.8 is banned.
                    bodyBattery = null,
                    stressLevel = null,
                    vo2Max = null,
                    restingHeartRate = rhr,
                    hrvRmssd = nightHrv,
                    sleepDurationMinutes = asleepMinutes ?: tibMinutes,
                    sleepStages = uiSleepStages,
                    sessionStartTime = winnerSession?.startTime,
                    sessionEndTime = winnerSession?.endTime,
                    asleepMinutes = asleepMinutes,
                    tibMinutes = tibMinutes,
                    sessionHrSamples = sessionHrSamples,
                    eveningHrSamples = eveningHrSamples,
                    sessionHrvSamples = sessionHrvSamples,
                    sleepSourceTag = sourceTag,
                    lastSyncTimestamp = syncTime
                )
            } catch (e: SecurityException) {
                Log.w("HealthConnectUplink", "HealthConnect SecurityException: ${e.message}")
                updateStatus(UplinkStatus.PermissionRequired)
                return DeepBiometrics()
            } catch (e: Throwable) {
                lastException = e
                if (e.message == "No Permission") {
                    return DeepBiometrics()
                }
            }

            if (attempt < (maxAttempts - 1)) {
                Log.w("HealthConnectUplink", "Sync attempt ${attempt + 1} failed, retrying in $currentDelay ms...")
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return DeepBiometrics()
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
