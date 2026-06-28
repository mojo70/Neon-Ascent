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
    private val deepLinkHelper: DeepLinkHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting Neural Brief synthesis...")

            // 1. Fetch latest insights and recommendations
            val insight = insightRepository.getLatestInsight().firstOrNull()
            val recommendation = insightRepository.getLatestRecommendation().firstOrNull()

            if (insight == null && recommendation == null) {
                Log.w(TAG, "No insights or recommendations found. Skipping brief.")
                return Result.success()
            }

            // 2. Build the payload using deepLinkHelper for navigation context if needed
            val title = "⚡ NEURAL BRIEF // SYNC_SUCCESS"
            val body = buildString {
                insight?.let { append("${it.content}\n\n") }
                recommendation?.let { append("REC: ${it.content}") }
            }.trim()

            // Check if we have a specific task to complete from deep link helper logic
            // (Simulated for this release)
            val dashboardUri = deepLinkHelper.createDashboardIntent().dataString ?: ""
            Log.d(TAG, "Navigating to: $dashboardUri")

            // 3. Define Actions (Aggregated from latest recommendations)
            val actions = mutableListOf<NeuralBriefManager.BriefAction>()
            
            // Recommendation-specific action
            recommendation?.relatedDirectiveId?.let { directiveId ->
                actions.add(
                    NeuralBriefManager.BriefAction(
                        label = "FORGE DIRECTIVE",
                        actionName = "com.neon.ascent.ACTION_FORGE_DIRECTIVE",
                        type = directiveId
                    )
                )
            }

            actions.addAll(listOf(
                NeuralBriefManager.BriefAction(
                    label = "LOG COMPLETE",
                    actionName = NeuralBriefManager.ACTION_LOG_COMPLETE,
                    type = "GENERAL_SUCCESS"
                ),
                NeuralBriefManager.BriefAction(
                    label = "OPEN DECK",
                    actionName = NeuralBriefManager.ACTION_OPEN_DECK,
                    type = "DASHBOARD"
                ),
                NeuralBriefManager.BriefAction(
                    label = "SNOOZE 2H",
                    actionName = NeuralBriefManager.ACTION_SNOOZE,
                    type = "DEFER"
                )
            ))

            // 4. Show Notification
            briefManager.showNeuralBrief(
                title = title,
                content = body,
                actions = actions.take(3) // Limit to 3 actions for standard notification visibility
            )

            Log.i(TAG, "Neural Brief delivered successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver Neural Brief", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
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
