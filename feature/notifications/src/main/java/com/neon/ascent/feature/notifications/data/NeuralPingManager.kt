package com.neon.ascent.feature.notifications.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.neon.ascent.feature.notifications.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeuralPingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Neural Pings",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Cyberdeck reminders and mission alerts"
            enableLights(true)
            lightColor = 0xFF00FF00.toInt() // Neon green
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun sendNeuralPing(title: String, message: String, taskId: String? = null) {
        val notificationId = taskId?.hashCode() ?: System.currentTimeMillis().toInt()

        // Content Intent: Open the app (Dashboard) when notification is clicked
        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_NOTIFICATION_TITLE, title)
            putExtra(EXTRA_NOTIFICATION_MESSAGE, message)
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val contentIntent = PendingIntent.getActivity(
            context, notificationId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, NeuralPingReceiver::class.java).apply {
            action = NeuralPingReceiver.ACTION_MARK_DONE
            putExtra(NeuralPingReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NeuralPingReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context, notificationId, markDoneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NeuralPingReceiver::class.java).apply {
            action = NeuralPingReceiver.ACTION_SNOOZE
            putExtra(NeuralPingReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NeuralPingReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_neural_ping)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setColor(0xFF00FF9F.toInt()) // Neon cyan
            .setContentIntent(contentIntent) // Click behavior
            .addAction(0, "MARK DONE", markDonePendingIntent)
            .addAction(0, "SNOOZE", snoozePendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun sendNeuralBrief(taskTitles: List<String>) {
        val message = taskTitles.joinToString("\n") { "• $it" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_neural_ping)
            .setContentTitle("⚡ NEURAL BRIEF // MULTIPLE_PROTOCOLS")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setColor(0xFF00FF9F.toInt())
            .build()

        notificationManager.notify(SYSTEM_BRIEF_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "neural_pings_channel"
        const val SYSTEM_BRIEF_ID = 9999
        const val EXTRA_NOTIFICATION_TITLE = "notification_title"
        const val EXTRA_NOTIFICATION_MESSAGE = "notification_message"
        const val EXTRA_TASK_ID = "notification_task_id"
    }
}
