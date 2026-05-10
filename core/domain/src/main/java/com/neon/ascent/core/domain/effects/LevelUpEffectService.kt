package com.neon.ascent.core.domain.effects

import com.neon.ascent.core.domain.model.SpecialType

interface LevelUpEffectService {
    fun triggerLevelUp(type: SpecialType, xpGained: Int)
    fun triggerLevelUp(delta: Int)
}
