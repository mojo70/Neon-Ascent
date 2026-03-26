package com.neon.ascent.data.repository

import com.neon.ascent.BuildConfig
import com.neon.ascent.data.remote.WeatherApi
import com.neon.ascent.feature.dashboard.WeatherState
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi
) {
    suspend fun getWeatherData(lat: Double, lon: Double): WeatherState {
        val useFahrenheit = Locale.getDefault().country == "US"
        val units = if (useFahrenheit) "imperial" else "metric"
        
        return try {
            val response = weatherApi.getCurrentWeather(
                lat = lat,
                lon = lon,
                apiKey = BuildConfig.OPENWEATHER_API_KEY,
                units = units
            )
            
            val isRaining = response.weather.any { 
                it.main.contains("Rain", ignoreCase = true) || 
                it.main.contains("Drizzle", ignoreCase = true) 
            }
            
            val currentTime = System.currentTimeMillis() / 1000
            val isNight = currentTime < response.sys.sunrise || currentTime > response.sys.sunset

            WeatherState(
                isRaining = isRaining,
                isNight = isNight,
                temperature = response.main.temp.toInt(),
                unitSymbol = if (useFahrenheit) "F" else "C"
            )
        } catch (e: Exception) {
            // Fallback to default if API fails
            WeatherState(unitSymbol = if (useFahrenheit) "F" else "C")
        }
    }
}
