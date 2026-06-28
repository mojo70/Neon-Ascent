package com.neon.ascent.feature.notifications.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.neon.ascent.feature.notifications.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the "Neural Brief" notification channel and delivery.
 * Designed to be polite, low-density, and highly informative.
 */
@Singleton
class NeuralBriefManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Neural Brief"
            val descriptionText = "Daily compiled insights and gentle guidance from your Neon Guide."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // Set vibration and lights off by default for politeness
                enableLights(false)
                enableVibration(false)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds and displays the Daily Neural Brief notification.
     * 
     * @param title The headline of the brief (e.g., "SYSTEM_SYNC // OPTIMIZED")
     * @param content The main body text (aggregated biometrics and guidance)
     * @param actions List of [BriefAction] to be added as buttons
     */
    fun showNeuralBrief(
        title: String,
        content: String,
        actions: List<BriefAction> = emptyList()
    ) {
        val notificationId = BRIEF_NOTIFICATION_ID

        // Deep link to the Cyberdeck / Dashboard (main entry point)
        // Note: Replace with actual activity/uri when deep linking is fully wired
        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_neon_deck) // Reusing existing cyberpunk icon
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(0xFF00FF9F.toInt()) // Neon Cyan
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .setGroup(GROUP_KEY)
            .setGroupSummary(false)

        // Add quick actions
        actions.forEachIndexed { index, action ->
            val intent = Intent(context, NeuralPingReceiver::class.java).apply {
                this.action = action.actionName
                putExtra(EXTRA_ACTION_TYPE, action.type)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, notificationId + index + 1, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, action.label, pendingIntent)
        }

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    /**
     * Data class representing a quick action in the notification.
     */
    data class BriefAction(
        val label: String,
        val actionName: String,
        val type: String
    )

    companion object {
        const val CHANNEL_ID = "neural_brief"
        const val GROUP_KEY = "com.neon.ascent.NEURAL_BRIEF_GROUP"
        const val BRIEF_NOTIFICATION_ID = 8888
        
        const val EXTRA_ACTION_TYPE = "extra_brief_action_type"
        
        // Action Types defined in Roadmap
        const val ACTION_LOG_COMPLETE = "ACTION_LOG_COMPLETE"
        const val ACTION_OPEN_DECK = "ACTION_OPEN_DECK"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
        const val ACTION_SKIP_REFLECT = "ACTION_SKIP_REFLECT"
    }
}
