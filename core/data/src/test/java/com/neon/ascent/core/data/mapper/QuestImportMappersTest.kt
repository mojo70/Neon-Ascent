package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.domain.goals.models.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestImportMappersTest {

    // Test 1: Quest title X → mission title X under imported_quests.
    @Test
    fun testQuestToMissionUnderImportedQuests() {
        val quest = QuestInput(
            id = "quest_alpha",
            title = "Infiltrate Arasaka Tower",
            description = "Breach the subnet without tripping ICE",
            isLongTerm = true,
            status = "ACTIVE"
        )

        val mission = QuestImportMappers.mapQuestToAscensionMission(quest)

        assertEquals("quest_alpha", mission.id)
        assertEquals(QUEST_IMPORTED_DIRECTIVE_ID, mission.directiveId)
        assertEquals("imported_quests", mission.directiveId)
        assertEquals("Infiltrate Arasaka Tower", mission.title)
        assertEquals("Breach the subnet without tripping ICE", mission.description)
        assertEquals(AscensionMissionStatus.ACTIVE, mission.status)
        assertEquals(0.0f, mission.progress, 0.001f)
    }

    // Test 2: Daily task → RECURRING, not ONE_TIME.
    @Test
    fun testDailyTaskToRecurring() {
        val dailyTask = TaskInput(
            id = "task_daily_1",
            questId = "quest_alpha",
            description = "Run diagnostic ping every dawn",
            isCompleted = false,
            isDaily = true
        )

        val nonDailyTask = TaskInput(
            id = "task_onetime_1",
            questId = "quest_alpha",
            description = "Acquire forged neural interface",
            isCompleted = false,
            isDaily = false
        )

        val recurringAscensionTask = QuestImportMappers.mapTaskToAscensionTask(dailyTask)
        val oneTimeAscensionTask = QuestImportMappers.mapTaskToAscensionTask(nonDailyTask)

        assertEquals("task_daily_1", recurringAscensionTask.id)
        assertEquals("quest_alpha", recurringAscensionTask.parentId)
        assertEquals(AscensionTaskType.RECURRING, recurringAscensionTask.type)
        assertNotEquals(AscensionTaskType.ONE_TIME, recurringAscensionTask.type)
        assertNotNull(recurringAscensionTask.recurrence)
        assertEquals(RecurrenceTypeV3.DAILY, recurringAscensionTask.recurrence?.type)

        assertEquals("task_onetime_1", oneTimeAscensionTask.id)
        assertEquals("quest_alpha", oneTimeAscensionTask.parentId)
        assertEquals(AscensionTaskType.ONE_TIME, oneTimeAscensionTask.type)
        assertNotEquals(AscensionTaskType.RECURRING, oneTimeAscensionTask.type)
        assertNull(oneTimeAscensionTask.recurrence)
    }

    // Test 3: Existing V3 id skipped / collision prefix quest: / qtask:.
    @Test
    fun testIdCollisionHandling() {
        val existingV3Ids = setOf("collision_quest", "collision_task")
        val existsLambda: (String) -> Boolean = { existingV3Ids.contains(it) }

        val questWithCollision = QuestInput(
            id = "collision_quest",
            title = "Subnet Crawl",
            description = "Desc"
        )
        val mappedMission = QuestImportMappers.mapQuestToAscensionMission(
            quest = questWithCollision,
            exists = existsLambda
        )
        assertEquals("quest:collision_quest", mappedMission.id)
        assertNotEquals("collision_quest", mappedMission.id)

        val taskWithCollision = TaskInput(
            id = "collision_task",
            questId = "collision_quest",
            description = "Task desc",
            isDaily = false
        )
        val mappedTask = QuestImportMappers.mapTaskToAscensionTask(
            task = taskWithCollision,
            exists = existsLambda,
            mappedQuestId = { mappedMission.id }
        )
        assertEquals("qtask:collision_task", mappedTask.id)
        assertEquals("quest:collision_quest", mappedTask.parentId)
        assertNotEquals("collision_task", mappedTask.id)
    }

    // Test 4: Saying mapper does not create an AscensionTask (Sayings are seed, not goals/tasks).
    @Test
    fun testSayingDoesNotCreateAscensionTask() {
        val saying = SayingInput(
            id = "s1",
            text = "Chrome your body, but guard the analog heart.",
            category = "Self & Identity",
            engagementScore = 85
        )

        // Verifying saying input remains distinct catalog seed data and is never converted to an AscensionTask
        val sayingObj: Any = saying
        assertFalse(
            "Saying must not be an instance of AscensionTask",
            sayingObj is AscensionTask
        )
        assertFalse(
            "Saying must not be an instance of AscensionMission",
            sayingObj is AscensionMission
        )
        assertFalse(
            "Saying must not be an instance of AscensionDirective",
            sayingObj is AscensionDirective
        )
        assertTrue(sayingObj is SayingInput)
    }
}
