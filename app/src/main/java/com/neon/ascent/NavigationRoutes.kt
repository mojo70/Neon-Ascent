package com.neon.ascent

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object Loading : Screen

    @Serializable
    data object MainHub : Screen

    @Serializable
    data class Biohacking(val focus: String? = null) : Screen

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
    data object NotificationPermission : Screen

    @Serializable
    data object NotificationPreferences : Screen

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
    data object DatabaseCore : Screen

    @Serializable
    data object AspirationCreation : Screen

    @Serializable
    data class AspirationDetail(val id: String) : Screen

    @Serializable
    data class MissionDetail(val id: String) : Screen

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
    data object GarminLogin : Screen

    @Serializable
    data class AttributeHistory(val attributeType: String) : Screen

    @Serializable
    data object Diagnostics : Screen

    @Serializable
    data object CognitiveTest : Screen

    @Serializable
    data object Lore : Screen

    @Serializable
    data object UserDossier : Screen

    @Serializable
    data object AscensionTerminal : Screen

    @Serializable
    data object ProtocolLibrary : Screen

    @Serializable
    data class DirectiveDetail(val id: String) : Screen

    @Serializable
    data class AscensionMissionDetail(val id: String) : Screen

    @Serializable
    data class TaskDetail(val id: String, val action: String? = null) : Screen

    @Serializable
    data class QuickCreateTask(val parentType: String? = null, val parentId: String? = null) : Screen

    @Serializable
    data class NeuralMentor(val contextJson: String? = null) : Screen

    @Serializable
    data class AscensionForge(
        val attribute: String? = null,
        val title: String? = null,
        val description: String? = null,
        val vision: String? = null,
        val biometrics: String? = null
    ) : Screen

    @Serializable
    data class AscensionReview(val directiveId: String) : Screen

    @Serializable
    data object TerminalRitual : Screen

    @Serializable
    data class NeonGuide(val initialMessage: String? = null) : Screen

    @Serializable
    data object DopamineMenu : Screen

    @Serializable
    data class WorkoutLog(val taskId: String? = null) : Screen
}
