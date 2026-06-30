package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.health.connect.client.records.*
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@HiltWorker
class HealthTriggerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val healthManager: HealthManager,
    private val ascensionRepository: AscensionRepository,
    private val neuralPingManager: NeuralPingManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!healthManager.isAvailableAndHasPermissions()) return Result.success()

        val now = Instant.now()
        val localTime = LocalTime.now()
        
        // 1. Detect "Wake" - check if sleep session ended in last 30 mins
        val recentData = healthManager.readRecentData(1)
        val lastSleep = recentData.sleep.maxByOrNull { it.endTime }
        
        val isWakeDetected = (lastSleep != null) && 
            lastSleep.endTime.isAfter(now.minusSeconds(1800)) &&
            lastSleep.endTime.isBefore(now)

        // 2. Detect "Before Bed" - simplified: check if it's evening and steps have dropped
        val isBeforeBedDetected = localTime.isAfter(LocalTime.of(21, 0)) && 
            localTime.isBefore(LocalTime.of(23, 30))

        if (isWakeDetected || isBeforeBedDetected) {
            val tasks = ascensionRepository.getAllRecurringTasks().first()
            val triggerWord = if (isWakeDetected) "wake" else "bed"
            
            val matchingTasks = tasks.filter { task ->
                task.timeWindows.any { it.contains(triggerWord, ignoreCase = true) }
            }

            if (matchingTasks.isNotEmpty()) {
                if (matchingTasks.size > 1) {
                    neuralPingManager.sendNeuralBrief(matchingTasks.map { it.title })
                } else {
                    val task = matchingTasks.first()
                    neuralPingManager.sendNeuralPing(
                        title = "⚡ NEURAL PING // ${task.title}",
                        message = "Dynamic trigger: $triggerWord detected. Execute protocol.",
                        taskId = task.id
                    )
                }
            }
        }

        return Result.success()
    }
}
