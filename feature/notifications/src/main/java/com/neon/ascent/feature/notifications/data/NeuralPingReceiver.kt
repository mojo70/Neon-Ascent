package com.neon.ascent.feature.notifications.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neon.ascent.core.domain.notifications.BriefService
import com.neon.ascent.core.domain.repository.AscensionRepository
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neon.ascent.feature.notifications.data.workers.NeuralBriefWorker
import java.util.concurrent.TimeUnit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NeuralPingReceiver : BroadcastReceiver() {

    @Inject
    lateinit var ascensionRepository: AscensionRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (action) {
            ACTION_MARK_DONE -> {
                // If it's a test or generic ping, just dismiss it
                if (taskId == null || taskId == "test_id") {
                    if (notificationId != -1) {
                        notificationManager.cancel(notificationId)
                    }
                    return
                }

                taskId.let { id ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val tasks = ascensionRepository.getAllRecurringTasks().first()
                        val task = tasks.find { it.id == id } ?: return@launch
                        
                        ascensionRepository.completeTask(task, null, null, null)
                        
                        if (notificationId != -1) {
                            notificationManager.cancel(notificationId)
                        }
                    }
                }
            }
            BriefService.ACTION_LOG_COMPLETE -> {
                // Handle "LOG COMPLETE" for Daily Brief (Test or Real)
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            }
            BriefService.ACTION_SNOOZE -> {
                // Logic for Snooze (reschedule WorkManager for 2 hours)
                val workManager = WorkManager.getInstance(context)
                val snoozeRequest = OneTimeWorkRequestBuilder<NeuralBriefWorker>()
                    .setInitialDelay(2, TimeUnit.HOURS)
                    .addTag("SNOOZE_BRIEF")
                    .build()
                
                workManager.enqueue(snoozeRequest)

                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            }
            BriefService.ACTION_SKIP_REFLECT -> {
                // Handle "Skip + Reflect" (e.g., log a 'skipped' state and open reflection UI)
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
                // Deep link to reflection UI could be triggered here if not handled by PendingIntent
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.neon.ascent.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.neon.ascent.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
