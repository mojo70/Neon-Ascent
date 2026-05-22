package com.neon.ascent.feature.health.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GarminCloudApi {
    @GET("modern/proxy/userstats-service/statistics/bodybattery/{date}")
    suspend fun getBodyBattery(
        @Path("date") date: String // yyyy-MM-dd
    ): List<BodyBatteryResponse>

    @GET("modern/proxy/wellness-service/wellness/dailySleepData/{userDisplayName}")
    suspend fun getSleepData(
        @Path("userDisplayName") userDisplayName: String,
        @Query("date") date: String // yyyy-MM-dd
    ): SleepDataResponse

    @GET("modern/proxy/userstats-service/statistics/stress/{date}")
    suspend fun getStress(
        @Path("date") date: String // yyyy-MM-dd
    ): StressResponse

    @GET("modern/proxy/userprofile-service/userprofile/user-settings")
    suspend fun getUserSettings(): UserSettingsResponse
}

data class UserSettingsResponse(
    val userData: UserProfileData
)

data class UserData(
    val displayName: String
)

data class UserProfileData(
    val displayName: String
)

data class BodyBatteryResponse(
    val date: String,
    val stats: List<BodyBatteryStat>
)

data class BodyBatteryStat(
    val bodyBatteryValue: Int,
    val timestamp: Long
)

data class SleepDataResponse(
    val dailySleepDto: DailySleepData,
    val sleepScores: SleepScore?
)

data class DailySleepData(
    val sleepTimeSeconds: Long,
    val deepSleepSeconds: Long,
    val lightSleepSeconds: Long,
    val remSleepSeconds: Long,
    val awakeSleepSeconds: Long
)

data class SleepScore(
    val overallScore: Int,
    val status: String
)

data class StressResponse(
    val maxStressLevel: Int,
    val avgStressLevel: Int,
    val stressValuesArray: List<List<Long>> // [[timestamp, stressValue], ...]
)
