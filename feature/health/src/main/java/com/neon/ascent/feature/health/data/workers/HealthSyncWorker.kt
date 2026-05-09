package com.neon.ascent.feature.health.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.neon.ascent.core.data.datastore.HealthPreferencesDataStore
import com.neon.ascent.core.domain.special.usecases.UpdateSpecialFromHealthUseCase
import com.neon.ascent.feature.health.data.HealthConnectManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthConnectManager: HealthConnectManager,
    private val updateSpecialFromHealthUseCase: UpdateSpecialFromHealthUseCase,
    private val healthPrefs: HealthPreferencesDataStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 0. Check if auto-sync is enabled by user
            if (!healthPrefs.autoSyncEnabled.first()) {
                return@withContext Result.success()
            }

            // 1. Quick availability + permission check
            if (!healthConnectManager.isAvailableAndHasPermissions()) {
                // Not fatal — user might grant later
                return@withContext Result.retry()
            }

            // 2. Perform the actual sync (last 48h window)
            val updatedAttributes = updateSpecialFromHealthUseCase(
                startTime = Instant.now().minus(Duration.ofHours(48))
            )

            // 3. Optional: Log success or trigger notifications
            if (updatedAttributes.isNotEmpty()) {
                healthPrefs.updateLastSyncTime()
            }

            Result.success()
        } catch (e: Exception) {
            // Health Connect can throw on permission changes or temporary unavailability
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "neon_ascent_health_sync"

        /**
         * Schedule periodic sync (every 8 hours)
         */
        fun scheduleDailySync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(
                8, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /**
         * One-time immediate sync
         */
        fun triggerImmediateSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<HealthSyncWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
