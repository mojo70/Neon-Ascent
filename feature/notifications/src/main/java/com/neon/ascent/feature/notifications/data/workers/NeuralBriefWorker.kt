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
    private val insightRepository: InsightProjectionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SYNTHESIS_INIT: Starting Neural Brief work cycle")
        return try {
            // Log permission and system state
            val hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
            Log.d(TAG, "SYSTEM_STATE: Permission status = $hasNotificationPermission, Attempt = $runAttemptCount")

            // 1. Fetch latest insights and recommendations from the projection layer
            Log.d(TAG, "DATA_FETCH: Accessing InsightProjectionRepository")
            val insight = insightRepository.getLatestInsight().firstOrNull()
            val recommendation = insightRepository.getLatestRecommendation().firstOrNull()

            // 2. Build the polite, neon-flavored body
            val title = "⚡ NEURAL BRIEF // SYNC_SUCCESS"
            val body = if (insight == null && recommendation == null) {
                Log.d(TAG, "SYNTHESIS_FALLBACK: No fresh data found, using default payload")
                "The network is quiet. Your biometrics remain within stable parameters. Maintain current momentum."
            } else {
                Log.d(TAG, "SYNTHESIS_SUCCESS: Mapping insights to neon-tone body")
                formatBriefContent(insight, recommendation)
            }

            Log.d(TAG, "PAYLOAD_GENERATED: Body length = ${body.length}")

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
                    type = recommendation?.relatedDirectiveId ?: ""
                ),
                NeuralBriefManager.BriefAction(
                    label = "SNOOZE 2H",
                    actionName = NeuralBriefManager.ACTION_SNOOZE,
                    type = "DEFER_2H"
                )
            )

            // 4. Show Notification
            Log.d(TAG, "DISPATCH: Sending payload to NeuralBriefManager")
            briefManager.showNeuralBrief(
                title = title,
                content = body,
                actions = actions.take(3)
            )

            Log.i(TAG, "SYNTHESIS_COMPLETE: Neural Brief delivered successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SYNTHESIS_FAILURE: Critical error during work cycle", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
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
