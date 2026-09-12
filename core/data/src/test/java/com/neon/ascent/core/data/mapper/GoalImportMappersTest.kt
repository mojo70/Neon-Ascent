package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class GoalImportMappersTest {

    // Test 1: Mapper: V2 Aspiration title X → Directive title X, status ACTIVE.
    @Test
    fun testV2AspirationToDirective() {
        val aspiration = Aspiration(
            id = "asp_123",
            title = "Master Cybernetics",
            description = "Become top tier netrunner",
            targetDate = LocalDate.of(2027, 1, 1),
            linkedAttributes = listOf(SpecialType.INTELLIGENCE),
            progress = GoalProgress(current = 0f, target = 100f),
            status = GoalStatus.ACTIVE
        )

        val directive = GoalImportMappers.mapV2AspirationToDirective(aspiration)

        assertEquals("asp_123", directive.id)
        assertEquals("Master Cybernetics", directive.title)
        assertEquals("Become top tier netrunner", directive.description)
        assertEquals(DirectiveStatus.ACTIVE, directive.status)
        assertEquals(LocalDate.of(2027, 1, 1), directive.targetEndDate)
        assertEquals(listOf(SpecialType.INTELLIGENCE), directive.linkedAttributes)
    }

    // Test 2: Mapper: V2 Habit daily → Task RECURRING, not ONE_TIME.
    @Test
    fun testV2HabitToTaskRecurring() {
        val habit = Habit(
            id = "habit_456",
            title = "Daily Neural Meditation",
            description = "20 minutes daily focus",
            recurrence = Recurrence(type = RecurrenceType.DAILY),
            linkedAttributes = listOf(SpecialType.ENDURANCE),
            progress = GoalProgress(current = 5f, target = 30f),
            streak = 5
        )

        val task = GoalImportMappers.mapV2HabitToAscensionTask(habit)

        assertEquals("habit_456", task.id)
        assertEquals("Daily Neural Meditation", task.title)
        assertEquals(AscensionTaskType.RECURRING, task.type)
        assertNotEquals(AscensionTaskType.ONE_TIME, task.type)
        assertEquals(RecurrenceTypeV3.DAILY, task.recurrence?.type)
        assertEquals(5, task.currentStreak)
    }

    // Test 3: Import skip / ID collision: existing V3 id is not overwritten.
    @Test
    fun testIdCollisionHandling() {
        val existingV3Ids = setOf("existing_id_1", "asp_123")
        val existsLambda: (String) -> Boolean = { existingV3Ids.contains(it) }

        val aspirationWithCollision = Aspiration(
            id = "asp_123",
            title = "Conflicting Aspiration",
            description = "Desc",
            targetDate = null,
            linkedAttributes = emptyList(),
            progress = GoalProgress(current = 0f, target = 10f)
        )

        val mappedDirective = GoalImportMappers.mapV2AspirationToDirective(
            aspiration = aspirationWithCollision,
            exists = existsLambda
        )

        assertEquals("v2:asp_123", mappedDirective.id)
        assertNotEquals("asp_123", mappedDirective.id)

        val v1GoalWithCollision = V1GoalInput(
            id = "existing_id_1",
            title = "V1 Conflicting Goal",
            targetValue = 100f,
            currentValue = 50f
        )

        val mappedV1Mission = GoalImportMappers.mapV1GoalToAscensionMission(
            v1 = v1GoalWithCollision,
            exists = existsLambda
        )

        assertEquals("v1:existing_id_1", mappedV1Mission.id)
        assertNotEquals("existing_id_1", mappedV1Mission.id)
    }

    // Test 4: V1 row with target 10 / current 4 → mission progress 0.4.
    @Test
    fun testV1GoalToMissionProgress() {
        val v1Goal = V1GoalInput(
            id = "v1_goal_789",
            title = "Complete 10 Code Reviews",
            objective = "Improve codebase quality",
            description = "Review PRs daily",
            targetValue = 10f,
            currentValue = 4f,
            unit = "reviews",
            linkedSpecial = SpecialType.INTELLIGENCE,
            isActive = true
        )

        val mission = GoalImportMappers.mapV1GoalToAscensionMission(v1Goal)

        assertEquals("v1_goal_789", mission.id)
        assertEquals(V1_IMPORTED_DIRECTIVE_ID, mission.directiveId)
        assertEquals("Complete 10 Code Reviews", mission.title)
        assertEquals("Review PRs daily", mission.description)
        assertEquals("Improve codebase quality", mission.objective)
        assertEquals(0.4f, mission.progress, 0.001f)
        assertEquals(AscensionMissionStatus.ACTIVE, mission.status)
        assertEquals(listOf(SpecialType.INTELLIGENCE), mission.linkedAttributes)
    }
}
