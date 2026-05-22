package com.neon.ascent.feature.notifications.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neon.ascent.core.domain.repository.AscensionRepository
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
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val action = intent.action ?: return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (action) {
            ACTION_MARK_DONE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val tasks = ascensionRepository.getAllRecurringTasks().first() // Simplified for now
                    val task = tasks.find { it.id == taskId } ?: return@launch
                    
                    ascensionRepository.completeTask(task, null, null, null)
                    
                    // If task is tied to a directive (via mission), we might want to prompt for reflection.
                    // For now, let's just dismiss the notification.
                    if (notificationId != -1) {
                        notificationManager.cancel(notificationId)
                    }
                    
                    // Check if we should prompt for reflection (ADHD requirement)
                    // This would likely involve launching an activity or a transparent dialog
                    if (task.parentId != null) {
                        // TODO: Launch reflection UI if parent exists
                    }
                }
            }
            ACTION_SNOOZE -> {
                // Handle snooze logic (e.g., reschedule in 15 mins)
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
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
