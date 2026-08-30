package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.core.domain.ai.AiResult
import com.neon.ascent.core.data.datastore.BriefPreferencesDataStore
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
import com.neon.ascent.core.data.notifications.BriefFactsBuilder
import com.neon.ascent.core.domain.notifications.BriefService
import com.neon.ascent.core.domain.notifications.brief.BriefStanceResolver
import com.neon.ascent.core.domain.notifications.brief.TemplateCopyWriter
import com.neon.ascent.core.domain.notifications.models.BriefStance
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Worker responsible for generating and displaying the daily "Neural Brief".
 * Aggregates biometric insights and provides actionable guidance.
 */
@HiltWorker
class NeuralBriefWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val briefService: BriefService,
    private val factsBuilder: BriefFactsBuilder,
    private val briefPrefs: BriefPreferencesDataStore,
    private val aiCore: AiCore,
    private val smartPingScheduler: SmartPingScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "// NEURAL_BRIEF_WORKER_START")
        return try {
            // 1. Build Facts
            val facts = factsBuilder.build()
            val today = LocalDate.now().toString()
            val factsHash = facts.factsHash

            // 2. DataStore Guard
            val lastDate = briefPrefs.lastBriefDate.first()
            val lastHash = briefPrefs.lastBriefFactsHash.first()

            if (lastDate == today && lastHash == factsHash) {
                Log.i(TAG, "// SKIP: Pulse already delivered for today with same facts.")
                // Still schedule next if this was a timer fire
                scheduleNextIfPossible()
                return Result.success()
            }

            // 3. Resolve Stance and Write Copy
            val stance = BriefStanceResolver.resolve(facts)
            var copy = TemplateCopyWriter.write(facts, stance)

            // 4. AI Polish (P1)
            if (aiCore.isReady()) {
                val polished = runAiPolish(facts, copy.body)
                if (polished != null) {
                    copy = copy.copy(body = polished)
                }
            }

            Log.d(TAG, "// STANCE: ${stance.name} | Headline: ${copy.headline}")

            // 5. Foreground Check
            val isForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

            if (isForeground) {
                Log.i(TAG, "// APP_IN_FOREGROUND: Skipping notification, updating card holder.")
            } else {
                if (hasNotificationPermission()) {
                    val actions = resolveActions(stance)
                    briefService.showNeuralBrief(
                        title = copy.headline,
                        content = copy.body,
                        actions = actions
                    )
                }
            }

            // 6. Update Prefs
            briefPrefs.updateLastBrief(today, factsHash, copy.headline, copy.body)

            // 7. Schedule Next Day (P1)
            scheduleNextIfPossible()

            Log.i(TAG, "// NEURAL_BRIEF_DELIVERED")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "// CRITICAL_FAILURE: Neural Brief cycle failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun resolveActions(stance: BriefStance): List<BriefService.BriefAction> {
        val actions = mutableListOf<BriefService.BriefAction>()
        when (stance) {
            BriefStance.PUSH -> {
                actions.add(BriefService.BriefAction("OPEN OPS", BriefService.ACTION_OPEN_DECK, "DASHBOARD"))
            }
            BriefStance.RECOVER -> {
                actions.add(BriefService.BriefAction("REVIEW DELOAD", BriefService.ACTION_OPEN_DECK, "DELOAD"))
            }
            BriefStance.HOLD -> {
                actions.add(BriefService.BriefAction("OPEN DECK", BriefService.ACTION_OPEN_DECK, "DASHBOARD"))
            }
            BriefStance.MISSING_DATA -> {
                actions.add(BriefService.BriefAction("SYNC DECK", BriefService.ACTION_OPEN_DECK, "DASHBOARD"))
            }
        }
        return actions
    }

    private suspend fun runAiPolish(facts: com.neon.ascent.core.domain.notifications.models.BriefFacts, templateDraft: String): String? {
        val factsJson = Gson().toJson(facts)
        val prompt = """
            [SYSTEM: Neural Brief Polisher]
            Rewrite the following health/training brief to be more immersive and cyberpunk.
            
            [FACTS]
            $factsJson
            
            [DRAFT]
            $templateDraft
            
            [CONSTRAINTS]
            - Keep every number from the draft.
            - Maximum 80 words.
            - No questions.
            - Do not use ERROR tokens or malfunction language.
            - Output only the polished text.
        """.trimIndent()

        return when (val result = aiCore.generate(prompt, forceLocal = true)) {
            is AiResult.Success -> {
                val polished = result.text
                if (validatePolish(polished, facts, templateDraft)) {
                    polished
                } else {
                    Log.w(TAG, "// AI_POLISH_VALIDATION_FAILED: Numbers mismatched.")
                    null
                }
            }
            is AiResult.Failure -> null
        }
    }

    private fun validatePolish(polished: String, facts: com.neon.ascent.core.domain.notifications.models.BriefFacts, draft: String): Boolean {
        // Extract all numbers from facts and draft
        val expectedNumbers = extractNumbers(Gson().toJson(facts) + draft)
        val polishedNumbers = extractNumbers(polished)
        
        // Every number in polished must appear in expected
        return polishedNumbers.all { it in expectedNumbers } && !polished.contains("ERROR")
    }

    private fun extractNumbers(text: String): Set<String> {
        return Regex("[0-9]+(?:\\.[0-9]+)?").findAll(text).map { it.value }.toSet()
    }

    private suspend fun scheduleNextIfPossible() {
        // For OneTimeWorkRequest chain
        smartPingScheduler.scheduleNextAdaptiveBrief()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
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
