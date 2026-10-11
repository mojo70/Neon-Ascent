package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.BriefService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfMapNeuralBriefShadesTest {

    @Test
    fun testItemReviewShade_NamesSpecificItemAndIncludesDeepLinkAction() {
        val itemName = "Physical Health"
        val copy = SelfMapBriefCopyWriter.write(
            shadeType = SelfMapShadeType.ITEM_REVIEW,
            itemName = itemName,
            daysUntouched = 14
        )

        assertEquals("SELF MAP // ATTACHMENT REVIEW", copy.shadeHeadline)
        assertTrue(copy.shadeBody.contains("Physical Health"))
        assertTrue(copy.shadeBody.contains("14 days"))
        assertEquals(1, copy.actions.size)
        assertEquals(BriefService.ACTION_OPEN_SELF_MAP, copy.actions.first().actionName)
        assertEquals("SELF_MAP_ITEM", copy.actions.first().type)
    }

    @Test
    fun testLoadChangedShade_DisplaysCoreLoadPercentage() {
        val copy = SelfMapBriefCopyWriter.write(
            shadeType = SelfMapShadeType.LOAD_CHANGED,
            coreLoadPercentage = 85f
        )

        assertEquals("SELF MAP // ATTACHMENT LOAD", copy.shadeHeadline)
        assertTrue(copy.shadeBody.contains("85%"))
        assertEquals(1, copy.actions.size)
        assertEquals(BriefService.ACTION_OPEN_SELF_MAP, copy.actions.first().actionName)
    }

    @Test
    fun testLogReactionShade_PromptForEquationOfEmotion() {
        val copy = SelfMapBriefCopyWriter.write(
            shadeType = SelfMapShadeType.LOG_REACTION
        )

        assertEquals("SELF MAP // EQUATION OF EMOTION", copy.shadeHeadline)
        assertTrue(copy.shadeBody.contains("expectation (EP) against perception (P)"))
        assertEquals(1, copy.actions.size)
        assertEquals(BriefService.ACTION_OPEN_SELF_MAP, copy.actions.first().actionName)
        assertEquals("SELF_MAP_EOE", copy.actions.first().type)
    }
}
