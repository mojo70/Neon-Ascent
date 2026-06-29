package com.neon.ascent

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.neon.ascent.feature.biohacking.BiohackingScreen
import com.neon.ascent.feature.charactercreation.AttributeScanScreen
import com.neon.ascent.feature.charactercreation.AvatarCaptureScreen
import com.neon.ascent.feature.charactercreation.CharacterCreationScreen
import com.neon.ascent.feature.charactercreation.CreationViewModel
import com.neon.ascent.feature.charactercreation.NeuralScanScreen
import com.neon.ascent.feature.cyberdeck.CyberdeckScreen
import com.neon.ascent.feature.cyberdeck.ExploitsScreen
import com.neon.ascent.feature.cyberdeck.NetworkHubScreen
import com.neon.ascent.feature.cyberdeck.UserDossierScreen
import com.neon.ascent.feature.dashboard.DashboardScreen
import com.neon.ascent.feature.dashboard.DashboardViewModel
import com.neon.ascent.feature.dashboard.CoreDashboardScreen
import com.neon.ascent.feature.dashboard.HolographicAvatarHub
import com.neon.ascent.feature.games.IceBreachScreen
import com.neon.ascent.feature.games.BlackIceBreachScreen
import com.neon.ascent.feature.games.CyberChessScreen
import com.neon.ascent.feature.games.CyberPongScreen
import com.neon.ascent.feature.journal.JournalViewModel
import com.neon.ascent.feature.attributes.AttributeDetailScreen
import com.neon.ascent.feature.journal.StoryScreen
import com.neon.ascent.feature.goals.GoalIntakeScreen
import com.neon.ascent.feature.goals.AspirationsScreen
import com.neon.ascent.feature.goals.ui.AspirationCreationScreen
import com.neon.ascent.feature.goals.ui.AspirationDetailScreen
import com.neon.ascent.feature.goals.ui.MissionDetailScreen
import com.neon.ascent.feature.goals.ui.DatabaseCoreScreen
import com.neon.ascent.feature.goals.ui.ascension.AscensionTerminalScreen
import com.neon.ascent.feature.goals.ui.ascension.AscensionForgeScreen
import com.neon.ascent.feature.goals.ui.ascension.AscensionReviewScreen
import com.neon.ascent.feature.goals.ui.ascension.AscensionTaskDetailScreen
import com.neon.ascent.feature.goals.ui.ascension.AscensionDirectiveDetailScreen
import com.neon.ascent.feature.goals.ui.ascension.AscensionMissionDetailScreen
import com.neon.ascent.feature.goals.ui.ascension.TerminalRitualScreen
import com.neon.ascent.feature.goals.ui.ascension.NeuralMentorScreen
import com.neon.ascent.feature.goals.ui.ascension.QuickTaskBottomSheet
import com.neon.ascent.feature.story.StoryIntakeScreen
import com.neon.ascent.feature.loading.LoadingScreen
import com.neon.ascent.feature.library.EReaderScreen
import com.neon.ascent.feature.settings.DeepNodeScreen
import com.neon.ascent.feature.settings.SettingsScreen
import com.neon.ascent.feature.health.ui.HealthPreferencesScreen
import com.neon.ascent.feature.health.ui.GarminLoginScreen
import com.neon.ascent.feature.health.data.remote.GarminAuthManager
import com.neon.ascent.feature.terminal.ui.AttributeHistoryScreen
import com.neon.ascent.feature.terminal.ui.DiagnosticsScreen
import com.neon.ascent.feature.terminal.ui.CognitiveTestScreen
import com.neon.ascent.feature.lore.LoreScreen
import com.neon.ascent.feature.notifications.ui.NeuralPingPermissionScreen
import com.neon.ascent.feature.notifications.ui.NotificationPermissionViewModel
import com.neon.ascent.feature.notifications.ui.NotificationPreferencesScreen
import com.neon.ascent.feature.wallet.EurodollarWalletScreen
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.common.cyberGlitch
import com.neon.ascent.util.derivePersonalityArchetype

@Composable
fun AppNavigation(
    creationViewModel: CreationViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    notificationViewModel: NotificationPermissionViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val userCharacter by dashboardViewModel.userCharacter.collectAsState()
    val tickerMessages by dashboardViewModel.tickerMessages.collectAsState()
    val showRationale by notificationViewModel.showRationale.collectAsState()

    LaunchedEffect(showRationale) {
        if (showRationale) {
            navController.navigate(Screen.NotificationPermission)
        }
    }

    NavHost(
        navController = navController, 
        startDestination = Screen.Loading,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable<Screen.Loading> {
            LoadingScreen(
                onLoadingFinished = {
                    val target = if (userCharacter?.isCreationComplete == true) Screen.MainHub else Screen.Creation
                    navController.navigate(target) {
                        popUpTo(Screen.Loading) { inclusive = true }
                    }
                }
            )
        }
        
        composable<Screen.MainHub> {
            val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 1)
            val coroutineScope = rememberCoroutineScope()
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> CyberdeckScreen(
                        onWalletClick = { navController.navigate(Screen.Wallet) },
                        onDatabaseClick = { navController.navigate(Screen.DatabaseCore) },
                        onIceBreachClick = { 
                            navController.navigate(Screen.IceBreach("ROOT"))
                        },
                        onCoreClick = {
                            navController.navigate(Screen.CoreDashboard)
                        },
                        onNetworkClick = {
                            navController.navigate(Screen.NetworkHub)
                        },
                        onExploitsClick = {
                            navController.navigate(Screen.Forge)
                        },
                        tickerMessages = tickerMessages
                    )
                    1 -> DashboardScreen(
                        onAvatarClick = { navController.navigate(Screen.HolographicHub) },
                        onAttributeSetClick = { navController.navigate(Screen.AttributeScan) },
                        onStoryClick = {
                            val target = if (dashboardViewModel.uiState.value.userStory.bio.isNotBlank()) {
                                Screen.Lore
                            } else {
                                Screen.StoryIntake
                            }
                            navController.navigate(target)
                        },
                        onGoalSetClick = { navController.navigate(Screen.AscensionTerminal) },
                        onTaskClick = { id -> navController.navigate(Screen.TaskDetail(id)) },
                        onSettingsClick = { navController.navigate(Screen.Settings) },
                        onDeusExMachinaClick = { navController.navigate(Screen.DeepNode("DEUS_EX_MACHINA")) },
                        onNavigateToBiohacking = { focus ->
                            // Since Biohacking is in the pager, we navigate to page 2
                            // and can pass the focus via a shared ViewModel or SavedStateHandle if needed
                            // For now, just scroll to the page
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                    2 -> BiohackingScreen(onBack = { /* Handled by pager */ })
                }
            }
        }

        composable<Screen.Biohacking> { backStackEntry ->
            val bio = backStackEntry.toRoute<Screen.Biohacking>()
            BiohackingScreen(
                focus = bio.focus,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.HolographicHub> {
            HolographicAvatarHub(
                onBack = { navController.popBackStack() },
                onNavigateToBiohacking = { focus ->
                    navController.navigate(Screen.Biohacking(focus))
                },
                onUpgradeClick = { attrName ->
                    navController.navigate(Screen.AttributeDetail(attrName))
                },
                onNavigateToDiagnostics = {
                    navController.navigate(Screen.Diagnostics)
                },
                onLoreClick = { navController.navigate(Screen.Lore) },
                onNavigateToForge = { type, title, desc ->
                    navController.navigate(Screen.AscensionForge(type.name, title, desc))
                }
            )
        }

        composable<Screen.IceBreach> { backStackEntry ->
            val iceBreach = backStackEntry.toRoute<Screen.IceBreach>()
            val context = iceBreach.context
            IceBreachScreen(
                onBreachSuccess = {
                    if (context == "ROOT") {
                        val prevRoute = navController.previousBackStackEntry?.destination?.route
                        if (prevRoute?.contains("CoreDashboard") == true) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.CoreDashboard) {
                                popUpTo(Screen.IceBreach(context)) { inclusive = true }
                            }
                        }
                    } else {
                        navController.previousBackStackEntry?.savedStateHandle?.set("unlocked_section", context)
                        navController.popBackStack()
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable<Screen.SystemBreach> {
            val journalViewModel: JournalViewModel = hiltViewModel()
            BlackIceBreachScreen(
                onBreachSuccess = {
                    journalViewModel.setSystemDatabaseHacked(true)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable<Screen.CoreDashboard>(
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400, easing = LinearEasing)) +
                        fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400, easing = LinearEasing)) +
                        fadeOut(animationSpec = tween(400))
            }
        ) { backStackEntry ->
            val unlockedSection = backStackEntry.savedStateHandle.get<String>("unlocked_section")
            
            var glitchIntensity by remember { mutableFloatStateOf(1f) }
            LaunchedEffect(Unit) {
                animate(1f, 0f, animationSpec = tween(600, easing = LinearOutSlowInEasing)) { value, _ ->
                    glitchIntensity = value
                }
            }

            Box(modifier = Modifier.fillMaxSize().cyberGlitch(glitchIntensity)) {
                CoreDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onTriggerHack = { context -> navController.navigate(Screen.IceBreach(context)) },
                    unlockedSectionFromResult = unlockedSection,
                    onUnlockConsumed = {
                        backStackEntry.savedStateHandle.remove<String>("unlocked_section")
                    }
                )
            }
        }

        composable<Screen.Journal> {
            DatabaseCoreScreen(
                navController = navController,
                onEntryClick = { /* TODO: Navigate to entry detail */ },
                onStoryClick = { navController.navigate(Screen.Story) },
                onBack = { navController.popBackStack() },
                onHackingRequired = { navController.navigate(Screen.SystemBreach) }
            )
        }

        composable<Screen.Story> {
            StoryScreen(
                onBack = { navController.popBackStack() },
                onHackingRequired = { navController.navigate(Screen.SystemBreach) }
            )
        }

        composable<Screen.StoryIntake> {
            StoryIntakeScreen(
                onComplete = { navController.popBackStack() }
            )
        }

        composable<Screen.Creation> {
            CharacterCreationScreen(
                onAbort = { navController.popBackStack() },
                onCreationFinished = { name, sex, dob, units, weight, somatotype, hFeet, hInches, hCm ->
                    creationViewModel.updateBasicInfo(name, sex, dob, units, weight, somatotype, hFeet, hInches, hCm)
                    navController.navigate(Screen.PersonalityIntake)
                }
            )
        }

        composable<Screen.PersonalityIntake> {
            NeuralScanScreen(
                onComplete = { answers ->
                    val energy = if (answers["ENERGY_SOURCE"]?.contains("SOLO") == true) "I" else "E"
                    val info = if (answers["INPUT_METHOD"]?.contains("SENSORY") == true) "S" else "N"
                    val decision = if (answers["LOGIC_GATE"]?.contains("CYBER") == true) "T" else "F"
                    val structure = if (answers["SYSTEM_EXECUTION"]?.contains("STRICT") == true) "J" else "P"
                    val mbti = "$energy$info$decision$structure"
                    
                    val alignmentLaw = when {
                        answers["OPERATIONAL_CODE"]?.contains("FOLLOW") == true -> "Lawful"
                        answers["OPERATIONAL_CODE"]?.contains("BREAK") == true -> "Chaotic"
                        else -> "Neutral"
                    }
                    val alignmentMorality = when {
                        answers["MORAL_COMPASS"]?.contains("RESCUE") == true -> "Good"
                        answers["MORAL_COMPASS"]?.contains("EXPLOIT") == true -> "Evil"
                        else -> "Neutral"
                    }
                    val alignment = if (alignmentLaw == "Neutral" && alignmentMorality == "Neutral") "True Neutral" else "$alignmentLaw $alignmentMorality"

                    val (archetype, _) = derivePersonalityArchetype(mbti, alignment)
                    creationViewModel.updatePersonality(mbti, alignment, archetype)
                    navController.navigate(Screen.AvatarCapture)
                }
            )
        }

        composable<Screen.AvatarCapture> {
            AvatarCaptureScreen(
                onComplete = { bitmap ->
                    creationViewModel.completeCreation(bitmap)
                    navController.navigate(Screen.MainHub) {
                        popUpTo(Screen.Creation) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.AttributeScan> {
            AttributeScanScreen(onComplete = { navController.popBackStack() })
        }

        composable<Screen.NotificationPermission> {
            NeuralPingPermissionScreen(
                onGranted = { navController.popBackStack() },
                onDismiss = { navController.popBackStack() },
                viewModel = notificationViewModel
            )
        }

        composable<Screen.NotificationPreferences> {
            NotificationPreferencesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Wallet> {
            EurodollarWalletScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.Goals> {
            GoalIntakeScreen(
                onGoalCreated = { navController.popBackStack() },
                onManageAspirations = { navController.navigate(Screen.Aspirations) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable<Screen.Aspirations> {
            AspirationsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.DatabaseCore> {
            DatabaseCoreScreen(
                navController = navController,
                onEntryClick = { /* TODO: Navigate to entry detail */ },
                onStoryClick = { navController.navigate(Screen.Story) },
                onBack = { navController.popBackStack() },
                onHackingRequired = { navController.navigate(Screen.SystemBreach) }
            )
        }

        composable<Screen.AspirationCreation> {
            AspirationCreationScreen(
                onCreated = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable<Screen.AspirationDetail> { backStackEntry ->
            val detail = backStackEntry.toRoute<Screen.AspirationDetail>()
            AspirationDetailScreen(
                aspirationId = detail.id,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.MissionDetail> { backStackEntry ->
            val detail = backStackEntry.toRoute<Screen.MissionDetail>()
            MissionDetailScreen(
                missionId = detail.id,
                onBack = { navController.popBackStack() },
                onCompleteMission = { navController.popBackStack() }
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onResetComplete = {
                    navController.navigate(Screen.Loading) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeepNodeUnlock = {
                    navController.navigate(Screen.DeepNode("ROOT"))
                },
                onNavigateToHealthPreferences = {
                    navController.navigate(Screen.HealthPreferences)
                },
                onNavigateToNotificationPreferences = {
                    navController.navigate(Screen.NotificationPreferences)
                }
            )
        }

        composable<Screen.HealthPreferences> {
            HealthPreferencesScreen(
                onNavigateToGarminLogin = { navController.navigate(Screen.GarminLogin) }
            )
        }

        composable<Screen.GarminLogin> {
            val healthViewModel: com.neon.ascent.feature.health.ui.HealthViewModel = hiltViewModel()
            GarminLoginScreen(
                authManager = healthViewModel.garminAuthManager,
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.AttributeHistory> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.AttributeHistory>()
            val type = SpecialType.valueOf(route.attributeType)
            AttributeHistoryScreen(
                attributeType = type,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Diagnostics> {
            DiagnosticsScreen(
                onNavigateToHistory = { historyType ->
                    navController.navigate(Screen.AttributeHistory(historyType.name))
                },
                onRunDiagnostic = {
                    navController.navigate(Screen.CognitiveTest)
                },
                onReCalibrateAttributes = {
                    navController.navigate(Screen.AttributeScan)
                }
            )
        }

        composable<Screen.CognitiveTest> {
            CognitiveTestScreen(
                onTestComplete = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.Lore> {
            LoreScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.UserDossier> {
            UserDossierScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.AscensionTerminal> {
            AscensionTerminalScreen(
                onBack = { navController.popBackStack() },
                onDirectiveClick = { id -> navController.navigate(Screen.DirectiveDetail(id)) },
                onTaskClick = { id -> navController.navigate(Screen.TaskDetail(id)) },
                onForgeClick = { navController.navigate(Screen.AscensionForge) },
                onReviewClick = { id -> navController.navigate(Screen.AscensionReview(id)) },
                onRitualClick = { navController.navigate(Screen.TerminalRitual) }
            )
        }

        composable<Screen.AscensionForge> { backStackEntry ->
            val forge = backStackEntry.toRoute<Screen.AscensionForge>()
            AscensionForgeScreen(
                onBack = { navController.popBackStack() },
                prefilledAttribute = forge.attribute,
                prefilledTitle = forge.title,
                prefilledDescription = forge.description
            )
        }

        composable<Screen.DirectiveDetail>(
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "neon-ascent://directive/{id}"
                }
            )
        ) { backStackEntry ->
            val directiveDetail = backStackEntry.toRoute<Screen.DirectiveDetail>()
            AscensionDirectiveDetailScreen(
                directiveId = directiveDetail.id,
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.MainHub) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onMissionClick = { id -> navController.navigate(Screen.AscensionMissionDetail(id)) }
            )
        }

        composable<Screen.AscensionMissionDetail>(
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "neon-ascent://mission/{id}"
                }
            )
        ) { backStackEntry ->
            val missionDetail = backStackEntry.toRoute<Screen.AscensionMissionDetail>()
            AscensionMissionDetailScreen(
                missionId = missionDetail.id,
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.MainHub) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onTaskClick = { id -> navController.navigate(Screen.TaskDetail(id)) },
                onDirectiveClick = { id -> navController.navigate(Screen.DirectiveDetail(id)) }
            )
        }

        composable<Screen.AscensionReview> { backStackEntry ->
            val review = backStackEntry.toRoute<Screen.AscensionReview>()
            AscensionReviewScreen(
                directiveId = review.directiveId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.TaskDetail>(
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "neon-ascent://task/{id}"
                },
                navDeepLink {
                    uriPattern = "neon-ascent://task/{id}?action={action}"
                }
            )
        ) { backStackEntry ->
            val taskDetail = backStackEntry.toRoute<Screen.TaskDetail>()
            AscensionTaskDetailScreen(
                taskId = taskDetail.id,
                prefillAction = taskDetail.action,
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.MainHub) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<Screen.QuickCreateTask> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.QuickCreateTask>()
            QuickTaskBottomSheet(
                prefilledParentId = args.parentId,
                onDismiss = { navController.popBackStack() }
            )
        }

        composable<Screen.NeuralMentor> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.NeuralMentor>()
            NeuralMentorScreen(
                initialContextJson = args.contextJson,
                navController = navController
            )
        }

        composable<Screen.TerminalRitual>(
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "neon-ascent://quarterly_review"
                }
            )
        ) {
            TerminalRitualScreen(
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.MainHub) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<Screen.DeepNode> { backStackEntry ->
            val deepNode = backStackEntry.toRoute<Screen.DeepNode>()
            DeepNodeScreen(
                initialSubScreen = deepNode.nodeType,
                onBack = { navController.popBackStack() },
                onGameSelect = { game ->
                    when (game) {
                        "CHESS" -> navController.navigate(Screen.CyberChess(returnToDopamine = true))
                        "PONG" -> navController.navigate(Screen.CyberPong(returnToDopamine = true))
                    }
                },
                onReaderNavigate = { id, path ->
                    navController.navigate(Screen.EReader(id, path))
                }
            )
        }

        composable<Screen.CyberChess> { backStackEntry ->
            val cyberChess = backStackEntry.toRoute<Screen.CyberChess>()
            CyberChessScreen(
                onBack = {
                    if (cyberChess.returnToDopamine) {
                        navController.navigate(Screen.DeepNode("DOPAMINE")) {
                            popUpTo<Screen.CyberChess> { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<Screen.CyberPong> { backStackEntry ->
            val cyberPong = backStackEntry.toRoute<Screen.CyberPong>()
            CyberPongScreen(
                onBack = {
                    if (cyberPong.returnToDopamine) {
                        navController.navigate(Screen.DeepNode("DOPAMINE")) {
                            popUpTo<Screen.CyberPong> { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<Screen.CharacterBio> {
            AvatarCaptureScreen(onComplete = { navController.popBackStack() })
        }

        composable<Screen.NetworkHub> {
            NetworkHubScreen(
                onBack = { navController.popBackStack() },
                onChessClick = { navController.navigate(Screen.CyberChess(false)) },
                onPersonalDossierClick = { navController.navigate(Screen.UserDossier) },
                viewModel = hiltViewModel()
            )
        }

        composable<Screen.Forge> {
            ExploitsScreen(
                onBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }

        composable<Screen.EReader> { backStackEntry ->
            val eReader = backStackEntry.toRoute<Screen.EReader>()
            EReaderScreen(
                bookId = eReader.bookId,
                bookAssetPath = eReader.assetPath,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Screen.AttributeDetail> { backStackEntry ->
            val attr = backStackEntry.toRoute<Screen.AttributeDetail>()
            AttributeDetailScreen(
                attributeName = attr.attributeName,
                onBack = { navController.popBackStack() },
                onNavigateToDatabase = { navController.navigate(Screen.Journal) }
            )
        }
    }
}
