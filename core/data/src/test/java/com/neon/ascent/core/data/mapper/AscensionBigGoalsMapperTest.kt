package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.data.local.entity.AscensionDirectiveEntity
import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.DirectiveStatus
import com.neon.ascent.core.domain.goals.models.PriorityTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AscensionBigGoalsMapperTest {

    @Test
    fun `test Big Goals fields round trip through AscensionMappers`() {
        val originalDirective = AscensionDirective(
            id = "dir_big_goals_001",
            title = "Achieve Financial Sovereignty",
            description = "Build high margin software assets",
            status = DirectiveStatus.ACTIVE,
            targetEndDate = LocalDate.of(2026, 12, 31),
            priorityTier = PriorityTier.A,
            outcomeStatement = "$100k ARR from independent protocols",
            affirmation = "I build resilient software daily.",
            visualUri = "file:///storage/emulated/0/Pictures/goals_book_01.jpg"
        )

        // Map to entity
        val entity: AscensionDirectiveEntity = originalDirective.toEntity()

        assertEquals("dir_big_goals_001", entity.id)
        assertEquals("A", entity.priorityTier)
        assertEquals("$100k ARR from independent protocols", entity.outcomeStatement)
        assertEquals("I build resilient software daily.", entity.affirmation)
        assertEquals("file:///storage/emulated/0/Pictures/goals_book_01.jpg", entity.visualUri)
        assertEquals(LocalDate.of(2026, 12, 31), entity.targetEndDate)

        // Map back to domain
        val mappedDirective: AscensionDirective = entity.toDomain()

        assertEquals(originalDirective.id, mappedDirective.id)
        assertEquals(originalDirective.title, mappedDirective.title)
        assertEquals(originalDirective.description, mappedDirective.description)
        assertEquals(originalDirective.status, mappedDirective.status)
        assertEquals(originalDirective.targetEndDate, mappedDirective.targetEndDate)
        assertEquals(originalDirective.priorityTier, mappedDirective.priorityTier)
        assertEquals(originalDirective.outcomeStatement, mappedDirective.outcomeStatement)
        assertEquals(originalDirective.affirmation, mappedDirective.affirmation)
        assertEquals(originalDirective.visualUri, mappedDirective.visualUri)
    }

    @Test
    fun `test Big Goals fields null values round trip through AscensionMappers`() {
        val originalDirective = AscensionDirective(
            id = "dir_null_fields_002",
            title = "Undated Goal",
            description = "No date or priority",
            status = DirectiveStatus.ACTIVE,
            targetEndDate = null,
            priorityTier = null,
            outcomeStatement = null,
            affirmation = null,
            visualUri = null
        )

        val entity = originalDirective.toEntity()
        assertNull(entity.priorityTier)
        assertNull(entity.outcomeStatement)
        assertNull(entity.affirmation)
        assertNull(entity.visualUri)
        assertNull(entity.targetEndDate)

        val mappedDirective = entity.toDomain()
        assertNull(mappedDirective.priorityTier)
        assertNull(mappedDirective.outcomeStatement)
        assertNull(mappedDirective.affirmation)
        assertNull(mappedDirective.visualUri)
        assertNull(mappedDirective.targetEndDate)
    }

    @Test
    fun `test filtering active directives returns only active directives`() {
        val now = Instant.now()
        val entities = listOf(
            AscensionDirectiveEntity(
                id = "1", title = "Active 1", description = "Desc 1", visionStatement = null,
                status = "ACTIVE", successMetrics = emptyList(), targetEndDate = LocalDate.now(),
                isQuarterly = false, createdAt = now, archivedAt = null, currentProgress = 0f,
                totalXPContributed = 0L, xpTarget = null, archetypeTag = null, tags = emptyList(),
                linkedAttributes = emptyList(), aiMentorMode = "REVIEW", aiGenerated = false,
                notes = null, completionHistorySummary = null, lastReviewDate = null
            ),
            AscensionDirectiveEntity(
                id = "2", title = "Completed 1", description = "Desc 2", visionStatement = null,
                status = "COMPLETED", successMetrics = emptyList(), targetEndDate = LocalDate.now(),
                isQuarterly = false, createdAt = now, archivedAt = null, currentProgress = 1f,
                totalXPContributed = 100L, xpTarget = null, archetypeTag = null, tags = emptyList(),
                linkedAttributes = emptyList(), aiMentorMode = "REVIEW", aiGenerated = false,
                notes = null, completionHistorySummary = null, lastReviewDate = null
            ),
            AscensionDirectiveEntity(
                id = "3", title = "Active 2", description = "Desc 3", visionStatement = null,
                status = "ACTIVE", successMetrics = emptyList(), targetEndDate = null,
                isQuarterly = false, createdAt = now, archivedAt = null, currentProgress = 0.5f,
                totalXPContributed = 50L, xpTarget = null, archetypeTag = null, tags = emptyList(),
                linkedAttributes = emptyList(), aiMentorMode = "REVIEW", aiGenerated = false,
                notes = null, completionHistorySummary = null, lastReviewDate = null
            ),
            AscensionDirectiveEntity(
                id = "4", title = "Archived 1", description = "Desc 4", visionStatement = null,
                status = "ARCHIVED", successMetrics = emptyList(), targetEndDate = null,
                isQuarterly = false, createdAt = now, archivedAt = now, currentProgress = 0f,
                totalXPContributed = 0L, xpTarget = null, archetypeTag = null, tags = emptyList(),
                linkedAttributes = emptyList(), aiMentorMode = "REVIEW", aiGenerated = false,
                notes = null, completionHistorySummary = null, lastReviewDate = null
            )
        )

        val activeDirectives = entities.map { it.toDomain() }.filter { it.status == DirectiveStatus.ACTIVE }

        assertEquals(2, activeDirectives.size)
        assertEquals(listOf("1", "3"), activeDirectives.map { it.id })
    }
}
