package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.goals.domain.usecases.CheckRecoveryMissionsUseCase
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val neuralPingManager: NeuralPingManager,
    private val repository: AscensionRepository,
    private val checkRecoveryUseCase: CheckRecoveryMissionsUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Run recovery check first
        val recoveries = checkRecoveryUseCase()
        if (recoveries.isNotEmpty()) {
            neuralPingManager.sendNeuralPing(
                title = "⚡ RECOVERY PROTOCOL",
                message = "Gaps detected in ${recoveries.size} neural loops. Recovery missions deployed."
            )
        }

        val tasks = repository.getAllRecurringTasks().first()
        val today = LocalDate.now()
        val completedToday = tasks.count { 
            it.lastCompleted?.atZone(ZoneId.systemDefault())?.toLocalDate() == today 
        }

        neuralPingManager.sendNeuralPing(
            title = "DAILY DECK SUMMARY",
            message = "$completedToday/${tasks.size} protocols executed today. Systems nominal."
        )

        return Result.success()
    }
}
