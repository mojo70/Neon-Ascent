package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.goals.models.AscensionMissionStatus
import com.neon.ascent.core.domain.goals.models.PriorityTier
import com.neon.ascent.core.domain.notifications.models.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DirectiveClauseResolverTest {

    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun `Soonest A-tier with targetEndDate inside 90 days resolves to named A-tier clause`() {
        val directive = AscensionDirective(
            id = "d1",
            title = "Become netrunner",
            description = "Master matrix",
            priorityTier = PriorityTier.A,
            targetEndDate = today.plusDays(48)
        )

        val clause = DirectiveClauseResolver.resolve(
            directives = listOf(directive),
            today = today
        )

        assertNotNull(clause)
        assertEquals("Become netrunner — A-tier, 48d", clause?.clauseText)
        assertEquals("d1", clause?.namedDirectiveId)
        assertFalse(clause!!.isCountOnly)
    }

    @Test
    fun `Soonest near-term with open Mission resolves to named clause with days left`() {
        val directive = AscensionDirective(
            id = "d2",
            title = "Ship protocol",
            description = "Deploy v1",
            priorityTier = PriorityTier.B,
            targetEndDate = today.plusDays(12)
        )
        val mission = AscensionMission(
            id = "m1",
            directiveId = "d2",
            title = "Alpha test",
            description = "Run tests",
            status = AscensionMissionStatus.ACTIVE
        )

        val clause = DirectiveClauseResolver.resolve(
            directives = listOf(directive),
            missions = listOf(mission),
            today = today
        )

        assertNotNull(clause)
        assertEquals("Ship protocol — 12d left", clause?.clauseText)
        assertEquals("d2", clause?.namedDirectiveId)
        assertFalse(clause!!.isCountOnly)
    }

    @Test
    fun `Density of 4 near-term Directives resolves to count clause`() {
        val directives = (1..4).map { i ->
            AscensionDirective(
                id = "d$i",
                title = "Directive $i",
                description = "Desc",
                priorityTier = PriorityTier.B,
                targetEndDate = today.plusDays(10L * i)
            )
        }

        val clause = DirectiveClauseResolver.resolve(
            directives = directives,
            today = today
        )

        assertNotNull(clause)
        assertEquals("4 near-term Directives open.", clause?.clauseText)
        assertTrue(clause!!.isCountOnly)
        assertNull(clause.namedDirectiveId)
    }

    @Test
    fun `Show Directives preference false returns null clause`() {
        val directive = AscensionDirective(
            id = "d1",
            title = "Ship protocol",
            description = "Deploy v1",
            priorityTier = PriorityTier.A,
            targetEndDate = today.plusDays(12)
        )

        val clause = DirectiveClauseResolver.resolve(
            directives = listOf(directive),
            showDirectives = false,
            today = today
        )

        assertNull(clause)
    }

    @Test
    fun `AmTemplateWriter generates OPEN DIRECTIVE action button when week complete and named Directive shown`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(sleepMinutes = 440, needMin = 440, seed = 80, seedBand = "CLEAR"),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "A",
                isWeeklyTargetMet = true,
                completedThisWeek = 3,
                scheduledThisWeek = 3
            ),
            directiveClause = BriefDirectiveClause(
                clauseText = "Become netrunner — A-tier, 48d",
                namedDirectiveId = "dir_netrunner"
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)

        assertEquals(1, copy.actions.size)
        val action = copy.actions.first()
        assertEquals("OPEN DIRECTIVE", action.label)
        assertEquals("ACTION_OPEN_DIRECTIVE", action.actionName)
        assertEquals("DIRECTIVE_DETAIL", action.type)
        assertEquals("dir_netrunner", action.extraId)
        assertTrue("Shade body contains directive clause", copy.shadeBody.contains("Become netrunner — A-tier, 48d"))
    }

    @Test
    fun `AmTemplateWriter generates OPEN OPS action button when session is due regardless of Directive`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(sleepMinutes = 440, needMin = 440, seed = 80, seedBand = "CLEAR"),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "Push",
                isWeeklyTargetMet = false
            ),
            directiveClause = BriefDirectiveClause(
                clauseText = "Ship protocol — 12d left",
                namedDirectiveId = "dir_ship"
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.PUSH)

        assertEquals(1, copy.actions.size)
        val action = copy.actions.first()
        assertEquals("OPEN OPS", action.label)
        assertEquals("ACTION_OPEN_DECK", action.actionName)
    }
}
