package com.neon.ascent.feature.biohacking

import com.neon.ascent.feature.biohacking.ui.labs.LabsSection
import com.neon.ascent.feature.biohacking.ui.labs.labsSectionForFocus
import org.junit.Assert.assertEquals
import org.junit.Test

class LabsFocusTest {

    @Test
    fun focusStringsLandOnThePlannedSection() {
        val expected = mapOf(
            null to LabsSection.Today,
            "vitals" to LabsSection.Today,
            "nutrition" to LabsSection.Today,
            "macros" to LabsSection.Today,
            "hrv" to LabsSection.Today,
            "biometrics" to LabsSection.Today,
            "body_log" to LabsSection.Log,
            "calculators" to LabsSection.Tools,
            "uplink" to LabsSection.Connect,
            "lifestyle" to LabsSection.CheckIn,
            "recovery" to LabsSection.CheckIn,
            "goals" to LabsSection.CheckIn,
            "baselines" to LabsSection.Records,
            "trends" to LabsSection.Records,
            "hologram" to LabsSection.Analyze,
            "insight" to LabsSection.Analyze,
            "attributes" to LabsSection.Analyze
        )
        expected.forEach { (focus, section) ->
            assertEquals(focus ?: "null", section, labsSectionForFocus(focus))
        }
    }

    @Test
    fun unknownFocusFallsBackToToday() {
        assertEquals(LabsSection.Today, labsSectionForFocus("nope"))
        assertEquals(LabsSection.Today, labsSectionForFocus(""))
        assertEquals(LabsSection.Today, labsSectionForFocus("HOLOGRAM"))
    }
}
