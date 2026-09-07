package com.neon.ascent.core.domain.notifications

interface BriefService {
    fun showNeuralBrief(
        title: String,
        content: String,
        actions: List<BriefAction> = emptyList(),
        notificationId: Int = BRIEF_NOTIFICATION_ID_AM
    )

    data class BriefAction(
        val label: String,
        val actionName: String,
        val type: String
    )

    companion object {
        const val ACTION_LOG_COMPLETE = "com.neon.ascent.ACTION_LOG_COMPLETE"
        const val ACTION_FORGE_DIRECTIVE = "com.neon.ascent.ACTION_FORGE_DIRECTIVE"
        const val ACTION_OPEN_DECK = "com.neon.ascent.ACTION_OPEN_DECK"
        const val ACTION_SNOOZE = "com.neon.ascent.ACTION_SNOOZE"
        const val ACTION_SKIP_REFLECT = "com.neon.ascent.ACTION_SKIP_REFLECT"

        const val BRIEF_NOTIFICATION_ID_AM = 8888
        const val BRIEF_NOTIFICATION_ID_PM = 8889
    }
}
