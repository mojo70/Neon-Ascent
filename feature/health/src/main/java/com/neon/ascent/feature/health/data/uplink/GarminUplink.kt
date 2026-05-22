package com.neon.ascent.feature.health.data.uplink

import com.neon.ascent.core.data.local.UplinkSecurityManager
import com.neon.ascent.feature.health.domain.uplink.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarminUplink @Inject constructor(
    private val securityManager: UplinkSecurityManager,
    private val bleManager: BleManager
) : NeuralUplink {

    override val provider: UplinkProvider = UplinkProvider.GARMIN

    private val _status = MutableStateFlow<UplinkStatus>(UplinkStatus.Disconnected)
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
        // This will implement the polling logic from the Cloud API
        // Mapping Garmin-specific "Body Battery" and "Stress" here
        return DeepBiometrics(
            bodyBattery = 85, // Stub
            stressLevel = 20, // Stub
            sleepScore = 92,  // Stub
            lastSyncTimestamp = System.currentTimeMillis()
        )
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
