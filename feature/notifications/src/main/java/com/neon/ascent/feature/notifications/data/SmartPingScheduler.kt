package com.neon.ascent.feature.notifications.data

import android.content.Context
import androidx.work.*
import com.neon.ascent.core.domain.NeuralPingScheduler
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.notifications.data.workers.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

import com.neon.ascent.core.data.datastore.BriefPreferencesDataStore
import java.time.temporal.ChronoUnit

@Singleton
class SmartPingScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ascensionRepository: AscensionRepository,
    private val healthManager: HealthManager,
    private val briefPrefs: BriefPreferencesDataStore
) : NeuralPingScheduler {

    private val workManager = WorkManager.getInstance(context)

    /** Main scheduling entry point — call after onboarding and on app start */
    override suspend fun scheduleSmartPings() {
        val activeTasks = ascensionRepository.getAllRecurringTasks().first()

        activeTasks.forEach { task ->
            scheduleForTask(task)
        }

        // Periodic health state check for dynamic triggers (wake/bed)
        scheduleHealthTriggerCheck()

        // Periodic insight full re-materialization (Nightly)
        scheduleNightlyInsightProjection()
        
        // P1: Adaptive scheduling
        scheduleNextAdaptiveBrief()
        
        // Warmup AI core at 05:30
        scheduleAiWarmup()
    }

    suspend fun scheduleNextAdaptiveBrief() {
        val now = LocalDateTime.now()
        val adaptiveEnabled = briefPrefs.adaptiveWakeEnabled.first()
        val quietEndStr = briefPrefs.quietHoursEnd.first()
        val quietEnd = LocalTime.parse(quietEndStr)

        var targetTime = quietEnd

        if (adaptiveEnabled) {
            val healthData = healthManager.readRecentData(1)
            val lastSleep = healthData.sleep.maxByOrNull { it.endTime }
            if (lastSleep != null) {
                val wakeTime = lastSleep.endTime.atZone(ZoneId.systemDefault()).toLocalTime()
                val adaptiveTime = wakeTime.plusMinutes(20)
                if (adaptiveTime.isAfter(quietEnd)) {
                    targetTime = adaptiveTime
                }
            }
        }

        var targetDateTime = now.with(targetTime)
        if (targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val delay = Duration.between(now, targetDateTime).toMillis()

        val request = OneTimeWorkRequestBuilder<NeuralBriefWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("adaptive_brief")
            .build()

        workManager.enqueueUniqueWork(
            NeuralBriefWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleAiWarmup() {
        val now = LocalDateTime.now()
        var target = now.with(LocalTime.of(5, 30))
        if (target.isBefore(now)) target = target.plusDays(1)
        
        val delay = Duration.between(now, target).toMillis()
        
        val request = OneTimeWorkRequestBuilder<AiWarmupWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
            
        workManager.enqueueUniqueWork("ai_warmup", ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Enqueues the Daily Neural Brief via NeuralBriefWorker.
     * Respects user quiet hours and delivery preferences.
     */
    override fun enqueueDailyNeuralBrief(isTestRequest: Boolean) {
        android.util.Log.d("SmartPingScheduler", "// ENQUEUE_BRIEF: isTest=$isTestRequest")
        if (isTestRequest) {
            android.util.Log.d("SmartPingScheduler", "// SCHEDULING_TEST_BRIEF...")
            val inputData = workDataOf("is_test" to true)
            val oneTimeRequest = OneTimeWorkRequestBuilder<NeuralBriefWorker>()
                .addTag("neural_brief_test")
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // Use expedited for test
                .build()
            workManager.enqueue(oneTimeRequest)
        } else {
            android.util.Log.d("SmartPingScheduler", "// SCHEDULING_PERIODIC_BRIEF...")
            NeuralBriefWorker.schedule(context)
        }
    }

    /**
     * Triggered when a high-value insight is projected.
     * Enqueues an expedited brief with a cooldown to prevent spam.
     */
    override fun triggerExpeditedBrief(reason: String) {
        android.util.Log.i("SmartPingScheduler", "// EXPEDITED_BRIEF_TRIGGERED: reason=$reason")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<NeuralBriefWorker>()
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.MINUTES) // Small buffer for synthesis
            .addTag("expedited_brief_$reason")
            .build()

        // Use UNIQUE work with KEEP to avoid spamming the user if multiple insights pop
        workManager.enqueueUniqueWork(
            "expedited_neural_brief",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun triggerBriefUpdateIfNecessary() {
        val now = LocalTime.now()
        // Only update before 11:00 AM if data changed
        if (now.isBefore(LocalTime.of(11, 0))) {
            val request = OneTimeWorkRequestBuilder<NeuralBriefWorker>()
                .addTag("brief_update")
                .build()
            workManager.enqueueUniqueWork(
                "neural_brief_update",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    private fun scheduleForTask(task: AscensionTask) {
        if (task.timeWindows.isEmpty() && task.recurrence == null) return

        // Fixed times are scheduled here. Dynamic triggers (wake/bed) are handled by HealthTriggerWorker.
        val fixedWindows = task.timeWindows.filter { !it.contains("wake", true) && !it.contains("bed", true) }
        
        fixedWindows.forEachIndexed { index, window ->
            scheduleFixedTime(task, window, index)
        }
    }

    private fun scheduleFixedTime(task: AscensionTask, window: String, index: Int) {
        val targetTime = try { LocalTime.parse(window) } catch (e: Exception) { return }
        val delay = calculateDelayToNextOccurrence(targetTime, task.recurrence)

        val inputData = Data.Builder()
            .putString("task_id", task.id)
            .putString("task_title", task.title)
            .build()

        val request = OneTimeWorkRequestBuilder<ContextualPingWorker>()
            .setInputData(inputData)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "ping_${task.id}_$index",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun calculateDelayToNextOccurrence(targetTime: LocalTime, recurrence: RecurrenceV3?): Long {
        val now = LocalDateTime.now()
        var target = now.with(targetTime)

        if (target.isBefore(now)) {
            target = target.plusDays(1)
        }

        // Adjust for days of week
        if (recurrence?.type == RecurrenceTypeV3.DAYS_OF_WEEK && recurrence.daysOfWeek.isNotEmpty()) {
            while (!recurrence.daysOfWeek.contains(target.dayOfWeek)) {
                target = target.plusDays(1)
            }
        } else if (recurrence?.type == RecurrenceTypeV3.WEEKDAYS) {
            while (target.dayOfWeek == DayOfWeek.SATURDAY || target.dayOfWeek == DayOfWeek.SUNDAY) {
                target = target.plusDays(1)
            }
        }

        return Duration.between(now, target).toMillis()
    }

    private fun scheduleHealthTriggerCheck() {
        val request = PeriodicWorkRequestBuilder<HealthTriggerWorker>(15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "health_dynamic_trigger_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleNightlyInsightProjection() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()

        val request = PeriodicWorkRequestBuilder<InsightProjectionWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "nightly_insight_projection",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
