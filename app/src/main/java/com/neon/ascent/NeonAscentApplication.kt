package com.neon.ascent

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.neon.ascent.core.domain.ai.AiCore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SQLiteDatabase
import javax.inject.Inject

@HiltAndroidApp
class NeonAscentApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var aiCore: AiCore

    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
        
        // P1: Warmup AI core from background
        CoroutineScope(Dispatchers.IO).launch {
            aiCore.warmup()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
