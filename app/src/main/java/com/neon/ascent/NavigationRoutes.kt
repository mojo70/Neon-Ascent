package com.neon.ascent

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object Loading : Screen

    @Serializable
    data object MainHub : Screen

    @Serializable
    data object HolographicHub : Screen

    @Serializable
    data class IceBreach(val context: String = "ROOT") : Screen

    @Serializable
    data object SystemBreach : Screen

    @Serializable
    data object CoreDashboard : Screen

    @Serializable
    data object Journal : Screen

    @Serializable
    data object Story : Screen

    @Serializable
    data object StoryIntake : Screen

    @Serializable
    data object Creation : Screen

    @Serializable
    data object PersonalityIntake : Screen

    @Serializable
    data object AvatarCapture : Screen

    @Serializable
    data object AttributeScan : Screen

    @Serializable
    data object Wallet : Screen

    @Serializable
    data class CyberChess(val returnToDopamine: Boolean = false) : Screen

    @Serializable
    data class CyberPong(val returnToDopamine: Boolean = true) : Screen

    @Serializable
    data object Goals : Screen

    @Serializable
    data object Aspirations : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object NetworkHub : Screen

    @Serializable
    data object Forge : Screen

    @Serializable
    data class DeepNode(val nodeType: String = "DEUS_EX_MACHINA") : Screen

    @Serializable
    data object CharacterBio : Screen

    @Serializable
    data class EReader(val bookId: String, val assetPath: String) : Screen

    @Serializable
    data class AttributeDetail(val attributeName: String) : Screen

    @Serializable
    data object HealthPreferences : Screen

    @Serializable
    data class AttributeHistory(val attributeType: String) : Screen

    @Serializable
    data object Diagnostics : Screen
}
