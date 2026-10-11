package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.mapper.toDomain
import com.neon.ascent.core.data.mapper.toEntity
import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.AscensionMission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SelfMapAscensionAnchoringTest {

    @Test
    fun testDirectiveAnchoring_PreservesAnchoredSelfItemNameAndId() {
        val directive = AscensionDirective(
            id = "dir_101",
            title = "STABILIZE_PHYSICAL_VESSEL",
            description = "Daily physical training protocol",
            anchoredSelfItemId = 5L,
            anchoredSelfItemName = "Physical Health"
        )

        assertEquals(5L, directive.anchoredSelfItemId)
        assertEquals("Physical Health", directive.anchoredSelfItemName)

        val entity = directive.toEntity()
        assertEquals(5L, entity.anchoredSelfItemId)
        assertEquals("Physical Health", entity.anchoredSelfItemName)

        val domainBack = entity.toDomain()
        assertEquals(5L, domainBack.anchoredSelfItemId)
        assertEquals("Physical Health", domainBack.anchoredSelfItemName)
    }

    @Test
    fun testMissionAnchoring_PreservesAnchoredSelfItemNameAndId() {
        val mission = AscensionMission(
            id = "mis_202",
            directiveId = "dir_101",
            title = "CORE_CARDIO_HYGIENE",
            description = "Run 5km 3x weekly",
            anchoredSelfItemId = 5L,
            anchoredSelfItemName = "Physical Health"
        )

        assertEquals(5L, mission.anchoredSelfItemId)
        assertEquals("Physical Health", mission.anchoredSelfItemName)

        val entity = mission.toEntity()
        assertEquals(5L, entity.anchoredSelfItemId)
        assertEquals("Physical Health", entity.anchoredSelfItemName)

        val domainBack = entity.toDomain()
        assertEquals(5L, domainBack.anchoredSelfItemId)
        assertEquals("Physical Health", domainBack.anchoredSelfItemName)
    }
}
