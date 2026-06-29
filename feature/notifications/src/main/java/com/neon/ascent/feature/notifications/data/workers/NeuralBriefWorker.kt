package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.InsightProjectionRepository
import com.neon.ascent.core.domain.repository.RecommendationProjection
import com.neon.ascent.core.domain.repository.SocraticInsight
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
    private val ascensionRepository: AscensionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "// NEURAL_BRIEF_WORKER_START")
        val isTest = inputData.getBoolean("is_test", false)
        return try {
            if (!hasNotificationPermission()) {
                Log.e(TAG, "// ABORT: NOTIFICATION_PERMISSIONS_DENIED")
                return Result.failure()
            }

            // 1. Fetch latest insights and recommendations
            val insight = if (isTest) {
                SocraticInsight(
                    content = "HRV recovered nicely. Body Battery strong. Systems optimal for high-intensity protocols.",
                    sourceMetrics = listOf("HRV", "SLEEP"),
                    timestamp = System.currentTimeMillis()
                )
            } else {
                Log.d(TAG, "// FETCHING_PROJECTIONS...")
                insightRepository.getLatestInsight().firstOrNull()
            }
            
            val recommendation = if (isTest) {
                RecommendationProjection(
                    content = "Good window for the mobility micro-mission today.",
                    relatedDirectiveId = "STRENGTH_PROTOCOL",
                    relatedSpecialAttribute = "S",
                    timestamp = System.currentTimeMillis()
                )
            } else {
                insightRepository.getLatestRecommendation().firstOrNull()
            }

            val activeMissions = if (isTest) emptyList() else {
                ascensionRepository.getActiveMissions().firstOrNull() ?: emptyList()
            }

            // 2. Build the polite, neon-flavored body
            val title = "⚡ NEURAL BRIEF // SYNC_SUCCESS"
            val body = formatBriefContent(insight, recommendation, activeMissions)

            Log.d(TAG, "// PAYLOAD_GENERATED: BodyLength=${body.length}")

            // 3. Define Actions
            val actions = mutableListOf<NeuralBriefManager.BriefAction>()
            
            val logActionType = recommendation?.relatedDirectiveId ?: activeMissions.firstOrNull()?.id ?: "GENERAL"
            
            actions.add(NeuralBriefManager.BriefAction(
                label = "LOG COMPLETE",
                actionName = NeuralBriefManager.ACTION_LOG_COMPLETE,
                type = logActionType
            ))

            actions.add(NeuralBriefManager.BriefAction(
                label = "SKIP + REFLECT",
                actionName = NeuralBriefManager.ACTION_SKIP_REFLECT,
                type = logActionType
            ))

            actions.add(NeuralBriefManager.BriefAction(
                label = "SNOOZE 2H",
                actionName = NeuralBriefManager.ACTION_SNOOZE,
                type = "DEFER_2H"
            ))

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

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Formats the brief content with a calm, competent, neon tone.
     * Highlights biometric data if available and appends the recommendation.
     */
    private fun formatBriefContent(
        insight: SocraticInsight?,
        recommendation: RecommendationProjection?,
        activeMissions: List<AscensionMission>
    ): String = buildString {
        if (insight != null) {
            append(insight.content)
            if (!insight.content.endsWith(".")) append(".")
            append(" ")
        } else {
            append("Biometrics remain stable. Recovery protocols holding. ")
        }
        
        if (recommendation != null) {
            append(recommendation.content)
            if (!recommendation.content.endsWith(".")) append(".")
        } else if (activeMissions.isNotEmpty()) {
            val mission = activeMissions.randomOrNull() ?: activeMissions.first()
            append("The window is open for the '${mission.title}' mission. Proceed when ready.")
        } else {
            append("Maintain current momentum. No critical directives pending.")
        }
    }.trim()

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
