package com.neon.ascent.core.common

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ResonanceType {
    STABILITY, STRENGTH, FOCUS, CHARISMA, AGILITY, LUCK;
    
    fun getColor(): Color = when(this) {
        STRENGTH -> Color(0xFFFF006E) // Pink/Red
        FOCUS -> Color(0xFF00F5FF)   // Intelligence Cyan
        AGILITY -> Color(0xFF00FF9C) // Green
        CHARISMA -> Color(0xFFFFFF00) // Yellow
        STABILITY -> Color(0xFF00FF9F) // Default
        LUCK -> Color(0xFF00FFFF)
    }
}

data class OperatorIdentity(
    val title: String = "SUBJECT_UNKNOWN",
    val resonance: ResonanceType = ResonanceType.STABILITY,
    val resonanceIntensity: Float = 0.2f
)

@Singleton
class IdentityCoordinator @Inject constructor() {
    private val _identity = MutableStateFlow(OperatorIdentity())
    val identity = _identity.asStateFlow()

    fun updateIdentity(topAttribute: String?, streakCount: Int) {
        val newResonance = when(topAttribute?.uppercase()) {
            "STRENGTH" -> ResonanceType.STRENGTH
            "INTELLIGENCE" -> ResonanceType.FOCUS
            "AGILITY" -> ResonanceType.AGILITY
            "CHARISMA" -> ResonanceType.CHARISMA
            else -> ResonanceType.STABILITY
        }
        
        val newTitle = generateTitle(topAttribute, streakCount)
        val intensity = (0.2f + (streakCount * 0.05f)).coerceAtMost(1f)
        
        _identity.value = OperatorIdentity(
            title = newTitle,
            resonance = newResonance,
            resonanceIntensity = intensity
        )
    }

    private fun generateTitle(topAttr: String?, streak: Int): String = when {
        streak > 15 -> "LEGENDARY_OPERATOR"
        streak > 7 -> when(topAttr?.uppercase()) {
            "STRENGTH" -> "IRON_FRAME_VETERAN"
            "INTELLIGENCE" -> "DEEP_DECK_ARCHITECT"
            "AGILITY" -> "GHOST_RUNNER"
            else -> "ELITE_SUBJECT"
        }
        streak > 3 -> when(topAttr?.uppercase()) {
            "STRENGTH" -> "REINFORCED_RUNNER"
            "INTELLIGENCE" -> "SYNCED_NETRUNNER"
            else -> "ACTIVE_OPERATIVE"
        }
        else -> "SUBJECT_0${(1000..9999).random()}"
    }
}
