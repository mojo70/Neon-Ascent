package com.neon.ascent.core.domain.health

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NeonChargeMlFeaturePipeTest {

    @Test
    fun `extractFeatures produces expected feature set for day with sleep session and steps`() {
        val input = NeonChargeMlInput(
            wakeSeed = 75,
            hoursAwake = 10.0,
            sessionDrainRaw = 25.0,
            stepDrainRaw = 15.0,
            hrLoadMin = 10,
            napMin = 0,
            priorDayResidual = 40.0,
            sleepDebt3d = 0.0,
            rpe7dAvg = 7.0,
            hrLoad7dAvg = 15.0,
            activeKcalToday = 500.0,
            isHardProtocol = true,
            hourOfDay = 18,
            nextClearance = null
        )

        val features = NeonChargeMlFeaturePipe.extractFeatures(input)

        assertTrue(features.containsKey("wakeSeed"))
        assertTrue(features.containsKey("hoursAwake"))
        assertTrue(features.containsKey("sessionDrainRaw"))
        assertTrue(features.containsKey("stepDrainRaw"))
        assertTrue(features.containsKey("hrLoadMin"))
        assertTrue(features.containsKey("isHardProtocol"))
        assertTrue(features.containsKey("sessionDrainRaw_x_isHardProtocol"))
        assertTrue(features.containsKey("hrLoadMin_x_lateDay"))
        assertTrue(features.containsKey("wakeSeed_x_hoursAwake"))

        assertNotNull(features["sessionDrainRaw"])
    }
}
