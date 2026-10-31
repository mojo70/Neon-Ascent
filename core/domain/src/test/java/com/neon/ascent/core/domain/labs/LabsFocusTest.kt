package com.neon.ascent.core.domain.labs

import com.neon.ascent.core.domain.labs.models.FocusMatchLevel
import com.neon.ascent.core.domain.labs.models.LabsFocus
import com.neon.ascent.core.domain.labs.models.OutcomeDomain
import com.neon.ascent.core.domain.labs.models.matchLevel
import com.neon.ascent.core.domain.labs.models.rankItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabsFocusTest {

    @Test
    fun defaultLabsFocus_isEmpty() {
        val focus = LabsFocus()
        assertNull(focus.primary)
        assertTrue(focus.supporting.isEmpty())
    }

    @Test
    fun validLabsFocus_constructsWithoutError() {
        val focus = LabsFocus(
            primary = OutcomeDomain.LONGEVITY_HEALTHSPAN,
            supporting = setOf(OutcomeDomain.SLEEP_ARCHITECTURE, OutcomeDomain.ATTENTION_FOCUS)
        )
        assertEquals(OutcomeDomain.LONGEVITY_HEALTHSPAN, focus.primary)
        assertEquals(setOf(OutcomeDomain.SLEEP_ARCHITECTURE, OutcomeDomain.ATTENTION_FOCUS), focus.supporting)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invariantEnforcement_primaryInSupporting_throwsException() {
        LabsFocus(
            primary = OutcomeDomain.LONGEVITY_HEALTHSPAN,
            supporting = setOf(OutcomeDomain.LONGEVITY_HEALTHSPAN, OutcomeDomain.SLEEP_ARCHITECTURE)
        )
    }

    @Test
    fun createSanitized_removesPrimaryFromSupporting() {
        val sanitized = LabsFocus.createSanitized(
            primary = OutcomeDomain.LONGEVITY_HEALTHSPAN,
            supporting = setOf(OutcomeDomain.LONGEVITY_HEALTHSPAN, OutcomeDomain.SLEEP_ARCHITECTURE)
        )
        assertEquals(OutcomeDomain.LONGEVITY_HEALTHSPAN, sanitized.primary)
        assertEquals(setOf(OutcomeDomain.SLEEP_ARCHITECTURE), sanitized.supporting)
    }

    @Test
    fun outcomeDomain_fromDisplayName_matchesCorrectly() {
        assertEquals(OutcomeDomain.LONGEVITY_HEALTHSPAN, OutcomeDomain.fromDisplayName("Longevity / Healthspan"))
        assertEquals(OutcomeDomain.PHYSICAL_PERFORMANCE_RECOVERY, OutcomeDomain.fromDisplayName("Physical Performance / Recovery"))
        assertEquals(OutcomeDomain.MENTAL_ENERGY_FATIGUE, OutcomeDomain.fromDisplayName("Mental Energy / Fatigue Resistance"))
        assertEquals(OutcomeDomain.SLEEP_ARCHITECTURE, OutcomeDomain.fromDisplayName("Sleep Architecture"))
        assertEquals(OutcomeDomain.STRESS_RESILIENCE_MOOD, OutcomeDomain.fromDisplayName("Stress Resilience / Mood"))
        assertEquals(OutcomeDomain.ATTENTION_FOCUS, OutcomeDomain.fromDisplayName("Attention / Focus"))
        assertNull(OutcomeDomain.fromDisplayName("Unknown Domain"))
        assertNull(OutcomeDomain.fromDisplayName(null))
    }

    @Test
    fun matchLevel_evaluatesPrimaryAndSupportingMatches() {
        val focus = LabsFocus(
            primary = OutcomeDomain.LONGEVITY_HEALTHSPAN,
            supporting = setOf(OutcomeDomain.SLEEP_ARCHITECTURE)
        )

        assertEquals(FocusMatchLevel.PRIMARY, focus.matchLevel(listOf("Longevity / Healthspan")))
        assertEquals(FocusMatchLevel.PRIMARY, focus.matchLevel(listOf("Longevity / Healthspan", "Sleep Architecture")))
        assertEquals(FocusMatchLevel.SUPPORTING, focus.matchLevel(listOf("Sleep Architecture")))
        assertEquals(FocusMatchLevel.NONE, focus.matchLevel(listOf("Attention / Focus")))
    }

    @Test
    fun rankItems_boostsPrimaryFirst_thenSupporting_keepsNonMatching() {
        val focus = LabsFocus(
            primary = OutcomeDomain.LONGEVITY_HEALTHSPAN,
            supporting = setOf(OutcomeDomain.SLEEP_ARCHITECTURE)
        )

        data class SampleItem(val name: String, val domains: List<String>)

        val itemNone = SampleItem("ItemNone", listOf("Attention / Focus"))
        val itemSupporting = SampleItem("ItemSupporting", listOf("Sleep Architecture"))
        val itemPrimary = SampleItem("ItemPrimary", listOf("Longevity / Healthspan"))

        val rawList = listOf(itemNone, itemSupporting, itemPrimary)
        val ranked = focus.rankItems(rawList) { it.domains }

        assertEquals(listOf(itemPrimary, itemSupporting, itemNone), ranked)
    }
}
