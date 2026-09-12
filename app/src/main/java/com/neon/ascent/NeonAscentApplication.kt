package com.neon.ascent

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.data.chronicle.ChronicleImportUseCase
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

    @Inject
    lateinit var chronicleImportUseCase: ChronicleImportUseCase

    override fun onCreate() {
        super.onCreate()
        try {
            SQLiteDatabase.loadLibs(this)
        } catch (e: Throwable) {
            Log.e("NeonAscentApplication", "Failed to load SQLCipher libs", e)
        }
        
        // Background tasks: Warmup AI core & run Chronicle import
        CoroutineScope(Dispatchers.IO).launch {
            try {
                aiCore.warmup()
            } catch (e: Throwable) {
                Log.e("NeonAscentApplication", "Failed to warmup AI core", e)
            }
            try {
                chronicleImportUseCase.runImport()
            } catch (e: Throwable) {
                Log.e("NeonAscentApplication", "Failed to run Chronicle import", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() {
            val builder = Configuration.Builder()
            if (::workerFactory.isInitialized) {
                builder.setWorkerFactory(workerFactory)
            } else {
                Log.w("NeonAscentApplication", "workerFactory not yet injected when WorkManager config requested")
            }
            return builder.build()
        }
}
