package com.neon.ascent.feature.health.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.neon.ascent.core.data.datastore.HealthPreferencesDataStore
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.core.domain.special.usecases.UpdateSpecialFromHealthUseCase
import com.neon.ascent.core.domain.goals.usecases.SyncBiometricMetricsUseCase
import com.neon.ascent.feature.health.data.uplink.NeuralUplinkManager
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.core.app.NotificationCompat
import com.neon.ascent.feature.health.R
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthManager: HealthManager,
    private val uplinkManager: NeuralUplinkManager,
    private val updateSpecialFromHealthUseCase: UpdateSpecialFromHealthUseCase,
    private val syncBiometricMetricsUseCase: SyncBiometricMetricsUseCase,
    private val healthPrefs: HealthPreferencesDataStore,
    private val smartPingScheduler: SmartPingScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "health_sync_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Health Data Sync",
                    android.app.NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Silent background telemetry synchronization."
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Neural Uplink Active")
            .setContentText("Syncing biometric data...")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()
        return ForegroundInfo(888, notification)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("HealthSyncWorker", "Starting background health sync")
        try {
            // 0. Check if auto-sync is enabled by user (unless this is a manual expedited request)
            val isManual = tags.contains("manual_sync")
            if (!isManual && !healthPrefs.autoSyncEnabled.first()) {
                Log.d("HealthSyncWorker", "Auto-sync disabled, skipping")
                return@withContext Result.success()
            }

            // 1. Perform Deep Sync for all uplinks (Garmin, etc)
            Log.d("HealthSyncWorker", "Fetching deep metrics from all uplinks")
            val deepMetrics = uplinkManager.fetchAllDeepMetrics()

            // 2. Perform Health Connect Sync + S.P.E.C.I.A.L. processing
            Log.d("HealthSyncWorker", "Processing Health Connect data and updating S.P.E.C.I.A.L.")
            val updatedAttributes = updateSpecialFromHealthUseCase(
                startTime = Instant.now().minus(Duration.ofHours(48))
            )

            // 3. Update Ascension Success Metrics
            Log.d("HealthSyncWorker", "Updating Ascension Directive Success Metrics")
            syncBiometricMetricsUseCase()

            // 4. P1: Trigger Brief Update if sleep data updated before 11:00
            smartPingScheduler.triggerBriefUpdateIfNecessary()

            // 5. Update sync state
            healthPrefs.updateLastSyncTime()
            Log.i("HealthSyncWorker", "Health sync complete. Updated ${updatedAttributes.size} attributes.")

            Result.success()
        } catch (e: Exception) {
            Log.e("HealthSyncWorker", "Sync failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_WORK_NAME = "neon_ascent_health_sync_periodic"
        private const val UNIQUE_ONE_TIME_WORK_NAME = "neon_ascent_health_sync_one_time"

        /**
         * Schedule periodic sync (every 60 minutes)
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag("periodic_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /**
         * Trigger immediate sync (Expedited)
         */
        fun triggerManualSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<HealthSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("manual_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
