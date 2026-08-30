package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neon.ascent.core.domain.ai.AiCore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AiWarmupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val aiCore: AiCore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            aiCore.warmup()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
