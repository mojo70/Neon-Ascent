package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ContextualPingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val neuralPingManager: NeuralPingManager,
    private val healthManager: HealthManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: return Result.failure()
        val title = inputData.getString("task_title") ?: "Protocol Ready"

        // Contextual intelligence
        val liveMetrics = healthManager.liveMetricsFlow().first()
        val heartRate = liveMetrics.heartRate
        val stepsToday = liveMetrics.stepsToday ?: 0L

        val message = when {
            heartRate != null && heartRate < 65 ->
                "Low HRV window detected. Perfect time for recovery protocol."
            stepsToday < 4000 ->
                "Movement signal weak. Time to move the frame."
            else -> "Deck is clear. Execute protocol: $title"
        }

        neuralPingManager.sendNeuralPing(
            title = "⚡ NEURAL PING // $title",
            message = message,
            taskId = taskId
        )

        return Result.success()
    }
}
