package com.neon.ascent.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

data class WeatherResponse(
    val weather: List<WeatherDescription>,
    val main: MainStats,
    val sys: SysInfo
)

data class WeatherDescription(
    val main: String,
    val description: String,
    val icon: String
)

data class MainStats(
    val temp: Double
)

data class SysInfo(
    val sunrise: Long,
    val sunset: Long
)
