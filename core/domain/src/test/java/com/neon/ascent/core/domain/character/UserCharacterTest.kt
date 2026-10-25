package com.neon.ascent.core.domain.character

import com.neon.ascent.core.domain.character.models.UserCharacter
import org.junit.Assert.assertEquals
import org.junit.Test

class UserCharacterTest {

    @Test
    fun testMetricToImperialConversion() {
        val metricChar = UserCharacter(
            name = "TEST",
            sex = "MALE",
            dob = "1990.01.01",
            units = "Metric",
            weight = "75.0",
            heightCm = "178",
            somatotype = 5.0f
        )

        assertEquals("178 CM", metricChar.formattedHeight("Metric"))
        assertEquals("75.0 KG", metricChar.formattedWeight("Metric"))

        assertEquals("5'10\"", metricChar.formattedHeight("Imperial"))
        assertEquals("165.3 LBS", metricChar.formattedWeight("Imperial"))

        val converted = metricChar.convertUnits("Imperial")
        assertEquals("Imperial", converted.units)
        assertEquals("165.3", converted.weight)
        assertEquals("5", converted.heightFeet)
        assertEquals("10", converted.heightInches)
    }

    @Test
    fun testImperialToMetricConversion() {
        val imperialChar = UserCharacter(
            name = "TEST",
            sex = "MALE",
            dob = "1990.01.01",
            units = "Imperial",
            weight = "165.3",
            heightFeet = "5",
            heightInches = "10",
            somatotype = 5.0f
        )

        assertEquals("5'10\"", imperialChar.formattedHeight("Imperial"))
        assertEquals("165.3 LBS", imperialChar.formattedWeight("Imperial"))

        assertEquals("178 CM", imperialChar.formattedHeight("Metric"))
        assertEquals("75.0 KG", imperialChar.formattedWeight("Metric"))

        val converted = imperialChar.convertUnits("Metric")
        assertEquals("Metric", converted.units)
        assertEquals("75.0", converted.weight)
        assertEquals("178", converted.heightCm)
    }
}
