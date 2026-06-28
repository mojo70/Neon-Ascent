package com.neon.ascent.feature.notifications.data

import android.content.Context
import androidx.work.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.feature.health.data.HealthConnectManager
import com.neon.ascent.feature.notifications.data.workers.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartPingScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ascensionRepository: AscensionRepository,
    private val healthConnectManager: HealthConnectManager
) {

    private val workManager = WorkManager.getInstance(context)

    /** Main scheduling entry point — call after onboarding and on app start */
    suspend fun scheduleSmartPings() {
        val activeTasks = ascensionRepository.getAllRecurringTasks().first()

        activeTasks.forEach { task ->
            scheduleForTask(task)
        }

        // Global daily summary ping
        scheduleDailySummary()

        // Periodic health state check for dynamic triggers (wake/bed)
        scheduleHealthTriggerCheck()
    }

    /**
     * Enqueues the Daily Neural Brief via NeuralBriefWorker.
     * Respects user quiet hours and delivery preferences.
     */
    suspend fun enqueueDailyNeuralBrief(isTestRequest: Boolean = false) {
        if (isTestRequest) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<NeuralBriefWorker>()
                .addTag("neural_brief_test")
                .build()
            workManager.enqueue(oneTimeRequest)
        } else {
            NeuralBriefWorker.schedule(context)
        }
    }

    /**
     * Triggered when a high-value insight is projected.
     * Enqueues an expedited brief with a cooldown to prevent spam.
     */
    fun triggerExpeditedBrief(reason: String) {
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

    private fun scheduleDailySummary() {
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateTimeUntilEveningSummary(), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_neural_summary",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    private fun calculateTimeUntilEveningSummary(): Long {
        // Aim for ~7 PM
        val target = LocalTime.of(19, 0)
        val now = LocalTime.now()
        return if (now.isBefore(target)) {
            Duration.between(now, target).toMillis()
        } else {
            Duration.between(now, target.plusHours(24)).toMillis()
        }
    }
}
