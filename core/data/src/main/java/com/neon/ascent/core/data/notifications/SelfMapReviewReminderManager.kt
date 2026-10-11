package com.neon.ascent.core.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.neon.ascent.core.common.DeepLinkHelper
import com.neon.ascent.core.data.repository.SelfMapRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfMapReviewReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val selfMapRepository: SelfMapRepository,
    private val deepLinkHelper: DeepLinkHelper
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Self Map Review Reminders"
            val descriptionText = "Opt-in periodic reflection prompts for high-attachment items."
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(false)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun checkAndDeliverReviewReminderIfNeeded(): Boolean {
        val isEnabled = selfMapRepository.reviewReminderEnabled.first()
        if (!isEnabled) return false

        val cadenceDays = selfMapRepository.reviewReminderCadenceDays.first()
        val items = selfMapRepository.getAllItemsOnce()
        val highAttachmentItems = items.filter { it.importance >= 0.7f && !it.isArchived }

        val cutoff = Instant.now().minus(cadenceDays.toLong(), ChronoUnit.DAYS)
        val staleItem = highAttachmentItems.firstOrNull { it.updatedAt.isBefore(cutoff) }
            ?: highAttachmentItems.minByOrNull { it.updatedAt }

        if (staleItem == null) return false

        val title = "SELF MAP // ATTACHMENT REVIEW"
        val content = "One high-attachment item ('${staleItem.name}') hasn't been reviewed in $cadenceDays days. Notice any expectation shifts today?"

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            deepLinkHelper.createSelfMapIntent(itemId = staleItem.id, action = "eoe_log"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(0xFF00FFFF.toInt()) // Neon Cyan
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            if (hasNotificationPermission()) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    companion object {
        const val CHANNEL_ID = "self_map_review_reminders"
        const val NOTIFICATION_ID = 9001
    }
}
