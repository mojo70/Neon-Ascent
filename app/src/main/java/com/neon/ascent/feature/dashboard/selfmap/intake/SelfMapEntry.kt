package com.neon.ascent.feature.dashboard.selfmap.intake

import com.neon.ascent.core.domain.selfmap.SelfMapIntakeMode

/**
 * Where an open of Self Map should land.
 * Returning users stay on the canvas unless they choose Guided start.
 * First open and the LABS card follow [SelfMapIntakeMode]. Settings FORM is that preference.
 */
object SelfMapEntry {
    enum class Reason { FIRST_OPEN, GUIDED_START, LABS }

    enum class Surface { CANVAS, CHAT, FORM }

    fun surface(intakeSeen: Boolean, preference: SelfMapIntakeMode, reason: Reason): Surface {
        if (reason == Reason.GUIDED_START) return Surface.CHAT
        if (intakeSeen) return Surface.CANVAS
        return if (preference == SelfMapIntakeMode.FORM) Surface.FORM else Surface.CHAT
    }
}
