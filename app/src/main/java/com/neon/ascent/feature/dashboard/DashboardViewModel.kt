package com.neon.ascent.feature.dashboard

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.repository.WeatherRepository
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import javax.inject.Inject

data class WeatherState(
    val isRaining: Boolean = false,
    val isNight: Boolean = false,
    val temperature: Int = 22
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userCharacterDao: UserCharacterDao,
    private val weatherRepository: WeatherRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {
    val userCharacter: StateFlow<UserCharacter?> = userCharacterDao.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    init {
        // Initial quick local estimate
        updateAtmosphereSimulated()
        // Then attempt real weather sync
        fetchRealWeather()
    }

    @SuppressLint("MissingPermission")
    private fun fetchRealWeather() {
        viewModelScope.launch {
            try {
                val location: Location? = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                location?.let {
                    val realWeather = weatherRepository.getWeatherData(it.latitude, it.longitude)
                    _weatherState.value = realWeather
                }
            } catch (e: Exception) {
                // If anything fails, we stay with the simulated or default state
            }
        }
    }

    private fun updateAtmosphereSimulated() {
        val now = LocalTime.now()
        val isNight = now.hour < 6 || now.hour > 19
        _weatherState.value = WeatherState(
            isRaining = false,
            isNight = isNight,
            temperature = if (isNight) 18 else 26
        )
    }

    fun updateNetrunnerName(newName: String) {
        viewModelScope.launch {
            userCharacter.value?.let {
                userCharacterDao.updateUserCharacter(it.copy(netrunnerName = newName))
            }
        }
    }
}
