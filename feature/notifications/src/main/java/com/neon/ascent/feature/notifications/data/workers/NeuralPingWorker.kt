package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.Habit
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class NeuralPingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val neuralPingManager: NeuralPingManager,
    private val goalRepository: GoalRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingHabits = goalRepository.getDueHabits().first()

        pendingHabits.forEach { habit ->
            val message = generateContextualMessage(habit)
            neuralPingManager.sendNeuralPing(
                title = "NEURAL PING // ${habit.title.uppercase()}",
                message = message,
                habitId = habit.id
            )
        }

        return Result.success()
    }

    private fun generateContextualMessage(habit: Habit): String = when {
        habit.streak > 5 -> "Your ${habit.streak}-day streak is live. Don't break the chain, Netrunner."
        habit.linkedAttributes.contains(SpecialType.INTELLIGENCE) -> "The deck is waiting. Time to run a focus protocol."
        habit.linkedAttributes.contains(SpecialType.ENDURANCE) -> "Recovery window detected. Maintain the signal."
        else -> "Protocol ${habit.title} is ready for execution."
    }
}
