package com.neon.ascent.feature.notifications.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.neon.ascent.core.common.DeepLinkHelper
import com.neon.ascent.core.domain.notifications.BriefService
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
    @ApplicationContext private val context: Context,
    private val deepLinkHelper: DeepLinkHelper
) : BriefService {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Neural Brief"
            val descriptionText = "Daily compiled insights and gentle guidance from your Neon Guide."
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
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
     * @param actions List of [BriefService.BriefAction] to be added as buttons
     */
    override fun showNeuralBrief(
        title: String,
        content: String,
        actions: List<BriefService.BriefAction>
    ) {
        val notificationId = BRIEF_NOTIFICATION_ID

        // Deep link to the Dashboard
        val dashboardIntent = deepLinkHelper.createDashboardIntent()
        val mainPendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            dashboardIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_neural_ping)
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
            val pendingIntent = when (action.actionName) {
                BriefService.ACTION_OPEN_DECK -> {
                    val intent = deepLinkHelper.createDashboardIntent()
                    PendingIntent.getActivity(context, notificationId + index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
                BriefService.ACTION_LOG_COMPLETE -> {
                    // Use a generic log deep link or specific task if available in 'type'
                    val intent = deepLinkHelper.createTaskCompletionIntent(action.type.ifBlank { "generic" })
                    // Set component to ensure it opens the app's main activity if it's a deep link
                    intent.setPackage(context.packageName)
                    PendingIntent.getActivity(context, notificationId + index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
                BriefService.ACTION_FORGE_DIRECTIVE -> {
                    val intent = deepLinkHelper.createForgeIntent(
                        title = action.type.takeIf { it.isNotBlank() },
                        description = "Suggested from Neural Brief."
                    )
                    PendingIntent.getActivity(context, notificationId + index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
                BriefService.ACTION_SKIP_REFLECT -> {
                    // Deep link to a reflection/journaling UI
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse("neon-ascent://reflection?source=brief&type=${action.type}"),
                        context,
                        context.packageManager.getLaunchIntentForPackage(context.packageName)?.component?.let { 
                            Class.forName(it.className) 
                        } ?: return@forEachIndexed
                    )
                    PendingIntent.getActivity(context, notificationId + index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
                else -> {
                    // Snooze and Skip+Reflect go through the BroadcastReceiver
                    val intent = Intent(context, NeuralPingReceiver::class.java).apply {
                        this.action = action.actionName
                        putExtra(NeuralPingReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                        putExtra(EXTRA_ACTION_TYPE, action.type)
                    }
                    PendingIntent.getBroadcast(
                        context, notificationId + index + 1, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }
            builder.addAction(0, action.label, pendingIntent)
        }

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    companion object {
        const val CHANNEL_ID = "neural_brief_v2"
        const val GROUP_KEY = "com.neon.ascent.NEURAL_BRIEF_GROUP"
        const val BRIEF_NOTIFICATION_ID = 8888
        
        const val EXTRA_ACTION_TYPE = "extra_brief_action_type"
    }
}
