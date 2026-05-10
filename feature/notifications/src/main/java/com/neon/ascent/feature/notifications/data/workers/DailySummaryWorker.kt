package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val neuralPingManager: NeuralPingManager,
    private val goalRepository: GoalRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habits = goalRepository.getHabits().first()
        val completed = habits.count { it.progress.current >= 1f }

        neuralPingManager.sendNeuralPing(
            title = "DAILY DECK SUMMARY",
            message = "$completed/${habits.size} protocols executed today. Streak integrity: ${habits.maxOfOrNull { it.streak } ?: 0} days."
        )

        return Result.success()
    }
}
