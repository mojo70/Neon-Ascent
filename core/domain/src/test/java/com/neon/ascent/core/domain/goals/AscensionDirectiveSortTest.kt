package com.neon.ascent.core.domain.goals

import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.AscensionDirectiveComparator
import com.neon.ascent.core.domain.goals.models.PriorityTier
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AscensionDirectiveSortTest {

    private fun createDirective(
        id: String,
        priorityTier: PriorityTier? = null,
        targetEndDate: LocalDate? = null
    ): AscensionDirective {
        return AscensionDirective(
            id = id,
            title = "Directive $id",
            description = "Description $id",
            priorityTier = priorityTier,
            targetEndDate = targetEndDate
        )
    }

    @Test
    fun `test priority tier ordering A before B before C`() {
        val directiveC = createDirective("C", priorityTier = PriorityTier.C, targetEndDate = LocalDate.of(2026, 1, 1))
        val directiveA = createDirective("A", priorityTier = PriorityTier.A, targetEndDate = LocalDate.of(2026, 12, 31))
        val directiveB = createDirective("B", priorityTier = PriorityTier.B, targetEndDate = LocalDate.of(2026, 6, 1))

        val list = listOf(directiveC, directiveA, directiveB)
        val sorted = list.sortedWith(AscensionDirectiveComparator)

        val sortedIds = sorted.map { it.id }
        assertEquals(listOf("A", "B", "C"), sortedIds)
    }

    @Test
    fun `test null priority treated as B for sort`() {
        val directiveNullTier = createDirective("NullTier", priorityTier = null, targetEndDate = LocalDate.of(2026, 5, 1))
        val directiveA = createDirective("A", priorityTier = PriorityTier.A, targetEndDate = LocalDate.of(2026, 10, 1))
        val directiveC = createDirective("C", priorityTier = PriorityTier.C, targetEndDate = LocalDate.of(2026, 1, 1))
        val directiveB = createDirective("B", priorityTier = PriorityTier.B, targetEndDate = LocalDate.of(2026, 6, 1))

        val list = listOf(directiveC, directiveNullTier, directiveC, directiveB, directiveA)
        val sorted = list.sortedWith(AscensionDirectiveComparator)

        // "A" comes first (Tier A).
        // "NullTier" and "B" both have effective Tier B. Since NullTier's date (May 1) is before B's date (June 1), NullTier comes before B.
        // "C" comes last (Tier C).
        val sortedIds = sorted.map { it.id }
        assertEquals("A", sortedIds[0])
        assertEquals("NullTier", sortedIds[1])
        assertEquals("B", sortedIds[2])
        assertEquals("C", sortedIds[3])
    }

    @Test
    fun `test soonest targetEndDate first within same tier`() {
        val directiveLater = createDirective("Later", priorityTier = PriorityTier.A, targetEndDate = LocalDate.of(2026, 12, 1))
        val directiveSooner = createDirective("Sooner", priorityTier = PriorityTier.A, targetEndDate = LocalDate.of(2026, 3, 15))
        val directiveMid = createDirective("Mid", priorityTier = PriorityTier.A, targetEndDate = LocalDate.of(2026, 8, 20))

        val list = listOf(directiveLater, directiveSooner, directiveMid)
        val sorted = list.sortedWith(AscensionDirectiveComparator)

        val sortedIds = sorted.map { it.id }
        assertEquals(listOf("Sooner", "Mid", "Later"), sortedIds)
    }

    @Test
    fun `test undated targetEndDate last within same tier`() {
        val directiveUndated = createDirective("Undated", priorityTier = PriorityTier.B, targetEndDate = null)
        val directiveDated = createDirective("Dated", priorityTier = PriorityTier.B, targetEndDate = LocalDate.of(2026, 11, 30))

        val list = listOf(directiveUndated, directiveDated)
        val sorted = list.sortedWith(AscensionDirectiveComparator)

        val sortedIds = sorted.map { it.id }
        assertEquals(listOf("Dated", "Undated"), sortedIds)
    }
}
