package com.neon.ascent.feature.notifications.data

import android.content.Context
import androidx.work.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.goals.models.AscensionTask
import com.neon.ascent.core.domain.goals.models.AscensionTaskType
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.feature.health.data.HealthConnectManager
import com.neon.ascent.feature.notifications.data.workers.ContextualPingWorker
import com.neon.ascent.feature.notifications.data.workers.DailySummaryWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalTime
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
    }

    private fun scheduleForTask(task: AscensionTask) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val inputData = Data.Builder()
            .putString("task_id", task.id)
            .putString("task_title", task.title)
            .build()

        val request = OneTimeWorkRequestBuilder<ContextualPingWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(calculateInitialDelay(task), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "ping_${task.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun calculateInitialDelay(task: AscensionTask): Long {
        val now = LocalTime.now()

        // Smart time windows - V3 logic
        return when {
            task.timeWindows.any { it.contains("wake", ignoreCase = true) } -> {
                // Approximate morning
                if (now.isBefore(LocalTime.of(9, 0))) 30_000L else 4 * 60 * 60 * 1000L
            }
            else -> 3 * 60 * 60 * 1000L
        }
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
            java.time.Duration.between(now, target).toMillis()
        } else {
            java.time.Duration.between(now, target.plusHours(24)).toMillis()
        }
    }
}
