package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.neon.ascent.core.common.DeepLinkHelper
import com.neon.ascent.core.domain.repository.InsightProjectionRepository
import com.neon.ascent.feature.notifications.data.NeuralBriefManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Worker responsible for generating and displaying the daily "Neural Brief".
 * Aggregates biometric insights and provides actionable guidance.
 */
@HiltWorker
class NeuralBriefWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val briefManager: NeuralBriefManager,
    private val insightRepository: InsightProjectionRepository,
    private val deepLinkHelper: com.neon.ascent.core.common.DeepLinkHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "// NEURAL_BRIEF_WORKER_START")
        val isTest = inputData.getBoolean("is_test", false)
        return try {
            // Log permission and system state
            val hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
            
            Log.d(TAG, "// SYSTEM_STATE: Permission=$hasNotificationPermission, Attempt=$runAttemptCount, IsBatteryLow=${isLowBattery()}, IsTest=$isTest")

            if (!hasNotificationPermission) {
                Log.e(TAG, "// ABORT: NOTIFICATION_PERMISSIONS_DENIED")
                return Result.failure()
            }

            // 1. Fetch latest insights and recommendations
            val insight = if (isTest) {
                com.neon.ascent.core.domain.repository.SocraticInsight(
                    content = "TEST_INSIGHT: HRV recovered overnight. Body Battery strong. Systems optimal for high-intensity protocols.",
                    sourceMetrics = listOf("HRV", "SLEEP"),
                    timestamp = System.currentTimeMillis()
                )
            } else {
                Log.d(TAG, "// FETCHING_PROJECTIONS...")
                insightRepository.getLatestInsight().firstOrNull()
            }
            
            val recommendation = if (isTest) {
                com.neon.ascent.core.domain.repository.RecommendationProjection(
                    content = "Today's Strength protocol looks good. Add the mobility micro-mission?",
                    relatedDirectiveId = "STRENGTH_PROTOCOL",
                    relatedSpecialAttribute = "S",
                    timestamp = System.currentTimeMillis()
                )
            } else {
                insightRepository.getLatestRecommendation().firstOrNull()
            }

            // 2. Build the polite, neon-flavored body
            val title = "⚡ NEURAL BRIEF // SYNC_SUCCESS"
            val body = if (insight == null && recommendation == null) {
                Log.d(TAG, "// SYNTHESIS_FALLBACK: Using default payload")
                "The network is quiet. Your biometrics remain within stable parameters. Maintain current momentum."
            } else {
                Log.d(TAG, "// SYNTHESIS_SUCCESS: Mapping data to neon-tone body")
                formatBriefContent(insight, recommendation)
            }

            Log.d(TAG, "// PAYLOAD_GENERATED: BodyLength=${body.length}")
            
            // Log DeepLink targets for debug
            Log.d(TAG, "// DEEPLINK_TARGET: ${deepLinkHelper.createDashboardIntent().dataString}")

            // 3. Define Actions
            val actions = listOf(
                NeuralBriefManager.BriefAction(
                    label = "OPEN DECK",
                    actionName = NeuralBriefManager.ACTION_OPEN_DECK,
                    type = "DASHBOARD"
                ),
                NeuralBriefManager.BriefAction(
                    label = "LOG COMPLETE",
                    actionName = NeuralBriefManager.ACTION_LOG_COMPLETE,
                    type = recommendation?.relatedDirectiveId ?: "GENERAL"
                ),
                NeuralBriefManager.BriefAction(
                    label = "SNOOZE 2H",
                    actionName = NeuralBriefManager.ACTION_SNOOZE,
                    type = "DEFER_2H"
                )
            )

            // 4. Show Notification
            Log.d(TAG, "// DISPATCH: Sending to NeuralBriefManager")
            briefManager.showNeuralBrief(
                title = title,
                content = body,
                actions = actions.take(3)
            )

            Log.i(TAG, "// NEURAL_BRIEF_DELIVERED")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "// CRITICAL_FAILURE: Neural Brief cycle failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun isLowBattery(): Boolean {
        val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL

        val level: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()
        
        return !isCharging && batteryPct < 15
    }

    /**
     * Formats the brief content with a calm, competent, neon tone.
     * Highlights biometric data if available and appends the recommendation.
     */
    private fun formatBriefContent(
        insight: com.neon.ascent.core.domain.repository.SocraticInsight?,
        recommendation: com.neon.ascent.core.domain.repository.RecommendationProjection?
    ): String = buildString {
        if (insight != null) {
            append(insight.content)
            if (!insight.content.endsWith(".")) append(".")
            append(" ")
        }
        
        if (recommendation != null) {
            append(recommendation.content)
            if (!recommendation.content.endsWith(".")) append(".")
        }
    }.trim().ifEmpty { 
        "Systems nominal. Ready for next cycle."
    }

    companion object {
        private const val TAG = "NeuralBriefWorker"
        const val UNIQUE_WORK_NAME = "neural_brief_daily"

        /**
         * Schedules the periodic daily brief.
         * Default window is early morning, but respects user settings if available.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(false) // Start with false for reliability, can be tuned
                .build()

            val workRequest = PeriodicWorkRequestBuilder<NeuralBriefWorker>(
                24, TimeUnit.HOURS, // Daily
                1, TimeUnit.HOURS   // 1-hour flex window
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
