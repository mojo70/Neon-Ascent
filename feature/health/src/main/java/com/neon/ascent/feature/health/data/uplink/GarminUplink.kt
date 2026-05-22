package com.neon.ascent.feature.health.data.uplink

import com.neon.ascent.core.data.local.UplinkSecurityManager
import com.neon.ascent.feature.health.data.remote.GarminAuthManager
import com.neon.ascent.feature.health.data.remote.GarminCloudApi
import com.neon.ascent.feature.health.domain.uplink.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    override suspend fun fetchDeepMetrics(): DeepBiometrics {
        if (!authManager.hasValidSession()) {
            _status.value = UplinkStatus.Disconnected
            return DeepBiometrics()
        }

        return try {
            _status.value = UplinkStatus.Syncing(0.1f)
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // 1. Get User Profile for displayName
            val settings = garminCloudApi.getUserSettings()
            val displayName = settings.userData.displayName

            _status.value = UplinkStatus.Syncing(0.4f)
            // 2. Fetch Body Battery
            val bbResponse = garminCloudApi.getBodyBattery(today)
            val currentBB = bbResponse.firstOrNull()?.stats?.lastOrNull()?.bodyBatteryValue

            _status.value = UplinkStatus.Syncing(0.7f)
            // 3. Fetch Sleep Data
            val sleepResponse = garminCloudApi.getSleepData(displayName, today)
            val sleepScore = sleepResponse.sleepScores?.overallScore

            _status.value = UplinkStatus.Syncing(0.9f)
            // 4. Fetch Stress
            val stressResponse = garminCloudApi.getStress(today)
            val avgStress = stressResponse.avgStressLevel

            _status.value = UplinkStatus.Connected
            DeepBiometrics(
                bodyBattery = currentBB,
                sleepScore = sleepScore,
                stressLevel = avgStress,
                lastSyncTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _status.value = UplinkStatus.Error(e.message ?: "Unknown sync error")
            DeepBiometrics()
        }
    }

    override suspend fun authenticate() {
        // Phase 2: Implementation of the WebView SSO / Token Interceptor
        _status.value = UplinkStatus.Authenticating
        // ... flow to open webview and capture tokens
        _status.value = UplinkStatus.Connected
        
        // When connected, we can also try to start BLE sync if enabled
        startBLESync()
    }

    override suspend fun disconnect() {
        // Clear tokens from secure storage
        _status.value = UplinkStatus.Disconnected
        bleManager.disconnect()
    }
    
    /**
     * Start high-frequency BLE broadcast scanning for real-time HR.
     */
    fun startBLESync() {
        bleManager.startScan()
    }
}
