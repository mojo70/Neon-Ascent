package com.neon.ascent.feature.health.di

import com.neon.ascent.feature.health.data.remote.GarminAuthManager
import com.neon.ascent.feature.health.data.remote.GarminCloudApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthNetworkModule {

    @Provides
    @Singleton
    fun provideGarminOkHttpClient(authManager: GarminAuthManager): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .cookieJar(authManager)
            .build()
    }

    @Provides
    @Singleton
    fun provideGarminCloudApi(okHttpClient: OkHttpClient): GarminCloudApi {
        return Retrofit.Builder()
            .baseUrl("https://connect.garmin.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GarminCloudApi::class.java)
    }
}
