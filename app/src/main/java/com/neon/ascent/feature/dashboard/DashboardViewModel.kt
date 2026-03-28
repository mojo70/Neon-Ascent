package com.neon.ascent.feature.dashboard

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.repository.HealthRepository
import com.neon.ascent.data.repository.WeatherRepository
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

data class WeatherState(
    val isRaining: Boolean = false,
    val isNight: Boolean = false,
    val temperature: Int = 22,
    val unitSymbol: String = if (Locale.getDefault().country == "US") "F" else "C"
)

data class HealthState(
    val steps: Long = 0,
    val heartRate: Int = 0,
    val vo2Max: Double = 0.0,
    val bodyBattery: Int = 75, // Default/Simulated
    val stressLevel: Int = 20, // Default/Simulated
    val isConnected: Boolean = false,
    val lastSyncTimestamp: Long? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userCharacterDao: UserCharacterDao,
    private val biohackingDao: BiohackingDao,
    private val weatherRepository: WeatherRepository,
    private val healthRepository: HealthRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {
    val userCharacter: StateFlow<UserCharacter?> = userCharacterDao.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val biohackingData: StateFlow<BiohackingData?> = biohackingDao.getBiohackingData(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _healthState = MutableStateFlow(HealthState())
    val healthState: StateFlow<HealthState> = _healthState.asStateFlow()

    val tickerMessages: StateFlow<List<String>> = combine(userCharacter, _weatherState) { character, weather ->
        generateTickerMessages(character, weather)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial quick local estimate
        updateAtmosphereSimulated()
        // Then attempt real weather sync
        fetchRealWeather()
        // And health data
        refreshHealthData()
    }

    private fun generateTickerMessages(character: UserCharacter?, weather: WeatherState): List<String> {
        val messages = mutableListOf<String>()
        
        // Character specific
        character?.let {
            messages.add("SUBJECT: ${it.name.uppercase()} // ARCHETYPE: ${it.archetype ?: "UNKNOWN"}")
            messages.add("LEVEL ${it.level} OPERATIVE DETECTED IN SECTOR 7")
            if (it.mbti != null) messages.add("NEURAL_PATTERN: ${it.mbti} // SYNC_RATIO: 98.4%")
            if (it.alignment != null) messages.add("MORAL_ALIGNMENT: ${it.alignment.uppercase()}")
        }

        // Weather/Environment
        val weatherStatus = if (weather.isRaining) "ACID_RAIN_WARNING" else "ATMOSPHERE_STABLE"
        messages.add("LOCAL_CONDITIONS: ${weather.temperature}°${weather.unitSymbol} // $weatherStatus")
        
        // Simulated Market/Mission data
        messages.add("MARKET_TICKER: \$SOL +4.2% // \$ETH -1.5% // \$EURODOLLAR STABLE")
        messages.add("MISSION_LOG: 'NEURAL_BREACH' SUCCESSFUL // REWARD: 5000 ED")
        messages.add("ALERT: ARASAKA_SECURITY_LEVEL_INCREASED_IN_WATSON")
        messages.add("STOCK: KANGA_BIOTECH (KBT) UP 12% AFTER NEURAL_LINK_BREAKTHROUGH")
        
        return messages
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

    fun refreshHealthData() {
        viewModelScope.launch {
            val hasPermissions = healthRepository.hasAllPermissions()
            if (hasPermissions) {
                val steps = healthRepository.getTodaySteps()
                val hr = healthRepository.getLatestHeartRate()
                val vo2 = healthRepository.getLatestVo2Max()
                
                // For Garmin "Body Battery" and "Stress", we simulate or pull from other sources if available.
                // Here we keep it somewhat dynamic based on heart rate/time of day for flavor.
                val simulatedBattery = (80 - (LocalTime.now().hour * 2)).coerceIn(10, 100)
                val simulatedStress = (hr / 4).coerceIn(5, 95)

                _healthState.value = HealthState(
                    steps = steps,
                    heartRate = hr,
                    vo2Max = vo2,
                    bodyBattery = simulatedBattery,
                    stressLevel = simulatedStress,
                    isConnected = true,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
                
                // Also update persistent biohacking data
                biohackingData.value?.let { current ->
                    biohackingDao.insertOrUpdate(current.copy(
                        isWearableSynced = true,
                        lastSyncTimestamp = System.currentTimeMillis()
                    ))
                } ?: run {
                    biohackingDao.insertOrUpdate(BiohackingData(
                        userId = 0,
                        isWearableSynced = true,
                        lastSyncTimestamp = System.currentTimeMillis()
                    ))
                }
            } else {
                _healthState.value = HealthState(isConnected = false)
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

    fun updateChessElo(newElo: Int) {
        viewModelScope.launch {
            userCharacter.value?.let {
                userCharacterDao.updateUserCharacter(it.copy(chessElo = newElo))
            }
        }
    }
}
