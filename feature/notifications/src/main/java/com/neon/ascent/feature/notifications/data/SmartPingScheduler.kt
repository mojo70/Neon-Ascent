package com.neon.ascent.feature.notifications.data

import android.content.Context
import androidx.work.*
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.Habit
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
    private val goalRepository: GoalRepository,
    private val healthConnectManager: HealthConnectManager
) {

    private val workManager = WorkManager.getInstance(context)

    /** Main scheduling entry point — call after onboarding and on app start */
    suspend fun scheduleSmartPings() {
        val activeHabits = goalRepository.getHabits().first()

        activeHabits.forEach { habit ->
            scheduleForHabit(habit)
        }

        // Global daily summary ping
        scheduleDailySummary()
    }

    private fun scheduleForHabit(habit: Habit) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val inputData = Data.Builder()
            .putString("habit_id", habit.id)
            .putString("habit_title", habit.title)
            .build()

        val request = OneTimeWorkRequestBuilder<ContextualPingWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(calculateInitialDelay(habit), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "ping_${habit.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun calculateInitialDelay(habit: Habit): Long {
        val now = LocalTime.now()

        // Smart time windows
        return when {
            // Morning habits (Strength, Agility)
            habit.linkedAttributes.any { it in listOf(SpecialType.STRENGTH, SpecialType.AGILITY) } -> {
                if (now.isBefore(LocalTime.of(9, 0))) 30_000L else 4 * 60 * 60 * 1000L // 4 hours
            }
            // Evening / Recovery habits
            habit.linkedAttributes.contains(SpecialType.ENDURANCE) -> {
                if (now.isAfter(LocalTime.of(18, 0))) 45_000L else 6 * 60 * 60 * 1000L
            }
            // Focus / Intelligence habits
            else -> {
                // Avoid lunch and late night
                when {
                    now.isAfter(LocalTime.of(12, 0)) && now.isBefore(LocalTime.of(14, 0)) -> 2 * 60 * 60 * 1000L
                    now.isAfter(LocalTime.of(22, 0)) -> 8 * 60 * 60 * 1000L
                    else -> 3 * 60 * 60 * 1000L
                }
            }
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
