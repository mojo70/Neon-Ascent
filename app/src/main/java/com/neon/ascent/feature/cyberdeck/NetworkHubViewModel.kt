package com.neon.ascent.feature.cyberdeck

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class NetworkHubViewModel @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _scanningState = MutableStateFlow<ScanningState>(ScanningState.Idle)
    val scanningState: StateFlow<ScanningState> = _scanningState.asStateFlow()

    private var targetLocation: Location? = null

    @SuppressLint("MissingPermission")
    fun startScan() {
        viewModelScope.launch {
            _scanningState.value = ScanningState.Scanning
            val currentLocation = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).result
            
            if (currentLocation != null) {
                // Generate random target within 50 yards (~45 meters)
                targetLocation = generateRandomLocation(currentLocation, 45.0)
                monitorProximity()
            } else {
                _scanningState.value = ScanningState.Error("COULD_NOT_GET_LOCATION")
            }
        }
    }

    private fun monitorProximity() {
        viewModelScope.launch {
            while (targetLocation != null && _scanningState.value is ScanningState.Scanning) {
                val current = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).result
                if (current != null && targetLocation != null) {
                    val distance = current.distanceTo(targetLocation!!)
                    if (distance < 5.0) {
                        _scanningState.value = ScanningState.Found("NETRUNNER_${(1000..9999).random()}")
                        targetLocation = null
                    } else {
                        // Keep scanning, update distance UI if needed
                        _scanningState.value = ScanningState.Tracing(distance)
                    }
                }
                delay(3000)
            }
        }
    }

    fun stopScan() {
        _scanningState.value = ScanningState.Idle
        targetLocation = null
    }

    private fun generateRandomLocation(base: Location, radiusMeters: Double): Location {
        val random = java.util.Random()
        val radiusInDegrees = radiusMeters / 111000f
        val u = random.nextDouble()
        val v = random.nextDouble()
        val w = radiusInDegrees * sqrt(u)
        val t = 2 * PI * v
        val x = w * cos(t)
        val y = w * sin(t)
        
        val newLocation = Location("")
        newLocation.latitude = base.latitude + x
        newLocation.longitude = base.longitude + y
        return newLocation
    }
}

sealed class ScanningState {
    object Idle : ScanningState()
    object Scanning : ScanningState()
    data class Tracing(val distanceMeters: Float) : ScanningState()
    data class Found(val name: String) : ScanningState()
    data class Error(val message: String) : ScanningState()
}
