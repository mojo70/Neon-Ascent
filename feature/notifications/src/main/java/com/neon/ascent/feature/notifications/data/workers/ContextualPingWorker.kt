package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neon.ascent.feature.health.data.HealthConnectManager
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ContextualPingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val neuralPingManager: NeuralPingManager,
    private val healthConnectManager: HealthConnectManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString("habit_id") ?: return Result.failure()
        val title = inputData.getString("habit_title") ?: "Protocol Ready"

        // Contextual intelligence
        val liveMetrics = healthConnectManager.liveMetricsFlow().first()

        val message = when {
            liveMetrics.heartRate != null && liveMetrics.heartRate!! < 65 ->
                "Low HRV window detected. Perfect time for recovery protocol."
            liveMetrics.stepsToday < 4000 ->
                "Movement signal weak. Time to move the frame."
            else -> "Deck is clear. Execute protocol: $title"
        }

        neuralPingManager.sendNeuralPing(
            title = "⚡ NEURAL PING // $title",
            message = message,
            habitId = habitId
        )

        return Result.success()
    }
}
