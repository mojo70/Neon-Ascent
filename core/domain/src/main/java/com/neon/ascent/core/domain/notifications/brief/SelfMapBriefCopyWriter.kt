package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefAction
import com.neon.ascent.core.domain.notifications.models.BriefCopy
import com.neon.ascent.core.domain.notifications.models.BriefStance

enum class SelfMapShadeType {
    ITEM_REVIEW,
    LOAD_CHANGED,
    LOG_REACTION
}

object SelfMapBriefCopyWriter {

    fun write(
        shadeType: SelfMapShadeType,
        itemName: String? = null,
        itemId: Long? = null,
        daysUntouched: Int = 14,
        coreLoadPercentage: Float = 100f
    ): BriefCopy {
        return when (shadeType) {
            SelfMapShadeType.ITEM_REVIEW -> {
                val targetName = itemName ?: "Core Attachment"
                val headline = "SELF MAP // ATTACHMENT REVIEW"
                val body = "Core attachment '$targetName' hasn't been reviewed in $daysUntouched days. Notice any expectation shifts?"
                val actions = listOf(
                    BriefAction("REVIEW ITEM", "ACTION_OPEN_SELF_MAP", "SELF_MAP_ITEM")
                )
                BriefCopy(
                    shadeHeadline = headline,
                    shadeBody = body,
                    cardBody = body,
                    actions = actions,
                    stance = BriefStance.HOLD,
                    shadeAllowed = true
                )
            }
            SelfMapShadeType.LOAD_CHANGED -> {
                val headline = "SELF MAP // ATTACHMENT LOAD"
                val body = "Core attachment load is currently ${coreLoadPercentage.toInt()}%. Moving attachments outward expands capacity for happiness."
                val actions = listOf(
                    BriefAction("OPEN SELF MAP", "ACTION_OPEN_SELF_MAP", "SELF_MAP_CANVAS")
                )
                BriefCopy(
                    shadeHeadline = headline,
                    shadeBody = body,
                    cardBody = body,
                    actions = actions,
                    stance = BriefStance.HOLD,
                    shadeAllowed = true
                )
            }
            SelfMapShadeType.LOG_REACTION -> {
                val headline = "SELF MAP // EQUATION OF EMOTION"
                val body = "Noticed an emotional reaction today? Compare expectation (EP) against perception (P) to dissolve resistance."
                val actions = listOf(
                    BriefAction("LOG REACTION", "ACTION_OPEN_SELF_MAP", "SELF_MAP_EOE")
                )
                BriefCopy(
                    shadeHeadline = headline,
                    shadeBody = body,
                    cardBody = body,
                    actions = actions,
                    stance = BriefStance.HOLD,
                    shadeAllowed = true
                )
            }
        }
    }
}
