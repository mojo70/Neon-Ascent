package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.goals.models.AscensionMissionStatus
import com.neon.ascent.core.domain.goals.models.PriorityTier
import com.neon.ascent.core.domain.notifications.models.BriefDirectiveClause
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ResolvedDirectiveCandidate(
    val directive: AscensionDirective,
    val daysLeft: Long
)

object DirectiveClauseResolver {

    fun resolveDirectiveCandidate(
        directives: List<AscensionDirective>,
        missions: List<AscensionMission> = emptyList(),
        today: LocalDate = LocalDate.now(ZoneId.systemDefault())
    ): ResolvedDirectiveCandidate? {
        if (directives.isEmpty()) return null

        val nearTermDirectives = directives.filter { d ->
            d.targetEndDate != null && ChronoUnit.DAYS.between(today, d.targetEndDate) in 0..90
        }

        // 1. Soonest A-tier with targetEndDate inside 90 days
        val candidate1 = nearTermDirectives
            .filter { it.priorityTier == PriorityTier.A }
            .minByOrNull { it.targetEndDate!! }

        if (candidate1 != null) {
            val days = ChronoUnit.DAYS.between(today, candidate1.targetEndDate)
            return ResolvedDirectiveCandidate(candidate1, days)
        }

        // 2. Soonest near-term (<= 90 days) that has an open Mission
        val activeMissionDirectiveIds = missions
            .filter { it.status == AscensionMissionStatus.ACTIVE }
            .mapNotNull { it.directiveId }
            .toSet()

        val candidate2 = nearTermDirectives
            .filter { it.id in activeMissionDirectiveIds }
            .minByOrNull { it.targetEndDate!! }

        if (candidate2 != null) {
            val days = ChronoUnit.DAYS.between(today, candidate2.targetEndDate)
            return ResolvedDirectiveCandidate(candidate2, days)
        }

        return null
    }

    fun resolve(
        directives: List<AscensionDirective>,
        missions: List<AscensionMission> = emptyList(),
        showDirectives: Boolean = true,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault())
    ): BriefDirectiveClause? {
        if (!showDirectives) return null
        if (directives.isEmpty()) return null

        val nearTermDirectives = directives.filter { d ->
            d.targetEndDate != null && ChronoUnit.DAYS.between(today, d.targetEndDate) in 0..90
        }
        val aTierDirectives = directives.filter { d ->
            d.priorityTier == PriorityTier.A
        }

        val nearTermCount = nearTermDirectives.size
        val aTierCount = aTierDirectives.size

        // 3. Density check: If density >= 4 near-term or >= 3 A-tier -> count only
        if (nearTermCount >= 4 || aTierCount >= 3) {
            val countToShow = if (nearTermCount > 0) nearTermCount else aTierCount
            return BriefDirectiveClause(
                clauseText = "$countToShow near-term Directives open.",
                isCountOnly = true,
                count = countToShow
            )
        }

        // 1. Soonest A-tier with targetEndDate inside 90 days
        val candidate1 = nearTermDirectives
            .filter { it.priorityTier == PriorityTier.A }
            .minByOrNull { it.targetEndDate!! }

        if (candidate1 != null) {
            val days = ChronoUnit.DAYS.between(today, candidate1.targetEndDate)
            val title = candidate1.title.take(22)
            val clauseText = "$title — A-tier, ${days}d"
            return BriefDirectiveClause(
                clauseText = clauseText,
                namedDirectiveId = candidate1.id,
                isCountOnly = false
            )
        }

        // 2. Soonest near-term (<= 90 days) that has an open Mission
        val activeMissionDirectiveIds = missions
            .filter { it.status == AscensionMissionStatus.ACTIVE }
            .mapNotNull { it.directiveId }
            .toSet()

        val candidate2 = nearTermDirectives
            .filter { it.id in activeMissionDirectiveIds }
            .minByOrNull { it.targetEndDate!! }

        if (candidate2 != null) {
            val days = ChronoUnit.DAYS.between(today, candidate2.targetEndDate)
            val title = candidate2.title.take(22)
            val clauseText = if (candidate2.priorityTier == PriorityTier.A) {
                "$title — A-tier, ${days}d"
            } else {
                "$title — ${days}d left"
            }
            return BriefDirectiveClause(
                clauseText = clauseText,
                namedDirectiveId = candidate2.id,
                isCountOnly = false
            )
        }

        // 4. Else omit
        return null
    }
}
