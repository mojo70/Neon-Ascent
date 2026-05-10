package com.neon.ascent.feature.notifications.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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

    fun sendNeuralPing(title: String, message: String, habitId: String? = null) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_neon_deck)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setColor(0xFF00FF9F.toInt()) // Neon cyan
            .build()

        notificationManager.notify(habitId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "neural_pings_channel"
    }
}
