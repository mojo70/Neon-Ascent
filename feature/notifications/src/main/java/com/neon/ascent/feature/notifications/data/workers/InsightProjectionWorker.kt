package com.neon.ascent.feature.notifications.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neon.ascent.core.data.processor.InsightProjectionProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class InsightProjectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processor: InsightProjectionProcessor
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            processor.processProjections()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
