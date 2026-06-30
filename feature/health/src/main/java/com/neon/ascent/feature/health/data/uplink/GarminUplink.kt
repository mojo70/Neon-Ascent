package com.neon.ascent.feature.health.data.uplink

import android.util.Log
import com.neon.ascent.core.data.local.UplinkSecurityManager
import com.neon.ascent.feature.health.data.remote.GarminAuthManager
import com.neon.ascent.feature.health.data.remote.GarminCloudApi
import com.neon.ascent.feature.health.domain.uplink.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarminUplink @Inject constructor(
    private val securityManager: UplinkSecurityManager,
    private val bleManager: BleManager,
    private val garminCloudApi: GarminCloudApi,
    private val authManager: GarminAuthManager
) : NeuralUplink {

    override val provider: UplinkProvider = UplinkProvider.GARMIN

    private val _status = MutableStateFlow<UplinkStatus>(if (authManager.hasValidSession()) UplinkStatus.Connected else UplinkStatus.Disconnected)
    override val status: StateFlow<UplinkStatus> = _status.asStateFlow()

    private val _syncStatus = MutableStateFlow(UplinkSyncStatus(provider, _status.value))
    override val syncStatus: StateFlow<UplinkSyncStatus> = _syncStatus.asStateFlow()

    private val _liveStream = MutableStateFlow<LiveBiometrics?>(null)
    override fun getLiveStream(): StateFlow<LiveBiometrics?> = _liveStream.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            bleManager.heartRate.collect { hr ->
                if (hr != null) {
                    _liveStream.value = LiveBiometrics(
                        heartRate = hr,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private fun updateStatus(newStatus: UplinkStatus) {
        _status.value = newStatus
        _syncStatus.update { it.copy(currentStatus = newStatus) }
    }

    override suspend fun fetchDeepMetrics(): DeepBiometrics {
        Log.d("GarminUplink", "Starting deep metrics sync for Garmin")
        _syncStatus.update { it.copy(lastSyncAttempt = System.currentTimeMillis()) }
        
        if (!authManager.hasValidSession()) {
            Log.w("GarminUplink", "Sync failed: No valid session")
            updateStatus(UplinkStatus.NeedsReAuth)
            _syncStatus.update { it.copy(lastError = "Session Expired") }
            return DeepBiometrics()
        }

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
                updateStatus(UplinkStatus.Syncing(0.1f))
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                // 1. Get User Profile for displayName
                val settings = garminCloudApi.getUserSettings()
                val displayName = settings.userData.displayName

                updateStatus(UplinkStatus.Syncing(0.4f))
                // 2. Fetch Body Battery
                val bbResponse = garminCloudApi.getBodyBattery(today)
                val currentBB = bbResponse.firstOrNull()?.stats?.lastOrNull()?.bodyBatteryValue

                updateStatus(UplinkStatus.Syncing(0.7f))
                // 3. Fetch Sleep Data
                val sleepResponse = garminCloudApi.getSleepData(displayName, today)
                val sleepScore = sleepResponse.sleepScores?.overallScore

                updateStatus(UplinkStatus.Syncing(0.9f))
                // 4. Fetch Stress
                val stressResponse = garminCloudApi.getStress(today)
                val avgStress = stressResponse.avgStressLevel

                updateStatus(UplinkStatus.Connected)
                val now = System.currentTimeMillis()
                _syncStatus.update { 
                    it.copy(
                        lastSuccessfulSync = now,
                        lastError = null
                    )
                }
                Log.i("GarminUplink", "Successfully synced deep metrics. BB: $currentBB, Sleep: $sleepScore")
                
                return DeepBiometrics(
                    bodyBattery = currentBB,
                    sleepScore = sleepScore,
                    stressLevel = avgStress,
                    lastSyncTimestamp = now
                )
            } catch (e: HttpException) {
                if ((e.code() == 401) || (e.code() == 403)) {
                    Log.e("GarminUplink", "Session expired during sync", e)
                    updateStatus(UplinkStatus.NeedsReAuth)
                    throw e
                }
                lastException = e
            } catch (e: Exception) {
                lastException = e
            }

            if (attempt < maxAttempts - 1) {
                Log.w("GarminUplink", "Sync attempt ${attempt + 1} failed, retrying in $currentDelay ms...")
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        throw lastException ?: Exception("Unknown sync failure")
    }

    private fun handleSyncError(e: Exception) {
        Log.e("GarminUplink", "Deep metrics sync failed after retries", e)
        val errorMsg = e.message ?: "Unknown sync error"
        if (_status.value !is UplinkStatus.NeedsReAuth) {
             updateStatus(UplinkStatus.Error(errorMsg))
        }
        _syncStatus.update { it.copy(lastError = errorMsg) }
    }

    override suspend fun authenticate() {
        Log.d("GarminUplink", "Authenticating Garmin uplink")
        // Auth is handled via WebView in the UI layer (GarminLoginScreen)
        // This method can be used to re-validate or refresh if needed.
        if (authManager.hasValidSession()) {
            updateStatus(UplinkStatus.Connected)
            startBLESync()
        } else {
            updateStatus(UplinkStatus.Disconnected)
        }
    }

    override suspend fun disconnect() {
        Log.i("GarminUplink", "Disconnecting Garmin uplink")
        // Clear tokens from secure storage
        updateStatus(UplinkStatus.Disconnected)
        bleManager.disconnect()
    }
    
    /**
     * Start high-frequency BLE broadcast scanning for real-time HR.
     */
    fun startBLESync() {
        bleManager.startScan()
    }
}
