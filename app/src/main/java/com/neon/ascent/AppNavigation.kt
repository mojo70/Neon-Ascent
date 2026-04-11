package com.neon.ascent

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neon.ascent.feature.biohacking.BiohackingScreen
import com.neon.ascent.feature.charactercreation.AvatarCaptureScreen
import com.neon.ascent.feature.charactercreation.CharacterCreationScreen
import com.neon.ascent.feature.charactercreation.CreationViewModel
import com.neon.ascent.feature.charactercreation.NeuralScanScreen
import com.neon.ascent.feature.charactercreation.deriveArchetype
import com.neon.ascent.feature.cyberdeck.CyberdeckScreen
import com.neon.ascent.feature.dashboard.DashboardScreen
import com.neon.ascent.feature.dashboard.DashboardViewModel
import com.neon.ascent.feature.dashboard.CoreDashboardScreen
import com.neon.ascent.feature.dashboard.HolographicAvatarHub
import com.neon.ascent.feature.games.IceBreachScreen
import com.neon.ascent.feature.games.BlackIceBreachScreen
import com.neon.ascent.feature.journal.JournalScreen
import com.neon.ascent.feature.journal.JournalViewModel
import com.neon.ascent.feature.journal.StoryScreen
import com.neon.ascent.feature.loading.LoadingScreen
import com.neon.ascent.feature.library.EReaderScreen
import com.neon.ascent.feature.settings.DeepNodeScreen
import com.neon.ascent.feature.settings.SettingsScreen
import com.neon.ascent.feature.wallet.EurodollarWalletScreen
import com.neon.ascent.ui.cyberGlitch
import kotlin.random.Random

@Composable
fun AppNavigation(
    creationViewModel: CreationViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val userCharacter by dashboardViewModel.userCharacter.collectAsState()
    val tickerMessages by dashboardViewModel.tickerMessages.collectAsState()

    NavHost(
        navController = navController, 
        startDestination = "loading",
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable("loading") {
            LoadingScreen(
                onLoadingFinished = {
                    val target = if (userCharacter?.isCreationComplete == true) "main_hub" else "creation"
                    navController.navigate(target) {
                        popUpTo("loading") { inclusive = true }
                    }
                }
            )
        }
        
        composable("main_hub") {
            val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 1)
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> CyberdeckScreen(
                        onWalletClick = { navController.navigate("wallet") },
                        onDatabaseClick = { navController.navigate("journal") },
                        onIceBreachClick = { 
                            navController.navigate("ice_breach/ROOT")
                        },
                        onCoreClick = {
                            navController.navigate("core_dashboard")
                        },
                        tickerMessages = tickerMessages
                    )
                    1 -> DashboardScreen(
                        onAvatarClick = { navController.navigate("holographic_hub") },
                        onAttributeSetClick = { navController.navigate("attribute_scan") },
                        onStoryClick = { navController.navigate("story") },
                        onGoalSetClick = { navController.navigate("goals") },
                        onSettingsClick = { navController.navigate("settings") },
                        onDeusExMachinaClick = { navController.navigate("deep_node/DEUS_EX_MACHINA") }
                    )
                    2 -> BiohackingScreen(onBack = { /* Handled by pager */ })
                }
            }
        }

        composable("holographic_hub") {
            HolographicAvatarHub(
                onBack = { navController.popBackStack() },
                onUpgradeClick = { /* TODO: Implement upgrades */ },
                onHacksClick = { /* TODO: Navigate to biohacking page in pager */ },
                onAttributeScanClick = { navController.navigate("attribute_scan") },
                onStoryClick = { navController.navigate("story") },
                onGoalSettingClick = { navController.navigate("goals") }
            )
        }

        composable(
            route = "ice_breach/{context}",
            arguments = listOf(navArgument("context") { type = NavType.StringType; defaultValue = "ROOT" })
        ) { backStackEntry ->
            val context = backStackEntry.arguments?.getString("context") ?: "ROOT"
            IceBreachScreen(
                onBreachSuccess = {
                    if (context == "ROOT") {
                        // Check if we came from dashboard or cyberdeck
                        val prevRoute = navController.previousBackStackEntry?.destination?.route
                        if (prevRoute?.contains("core_dashboard") == true) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("core_dashboard") {
                                popUpTo("ice_breach/$context") { inclusive = true }
                            }
                        }
                    } else {
                        // Set result and pop back to dashboard
                        navController.previousBackStackEntry?.savedStateHandle?.set("unlocked_section", context)
                        navController.popBackStack()
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable("system_breach") {
            val journalViewModel: JournalViewModel = hiltViewModel()
            BlackIceBreachScreen(
                onBreachSuccess = {
                    journalViewModel.setSystemDatabaseHacked(true)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = "core_dashboard",
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
            
            // Custom glitch transition effect
            var glitchIntensity by remember { mutableFloatStateOf(1f) }
            LaunchedEffect(Unit) {
                animate(1f, 0f, animationSpec = tween(600, easing = LinearOutSlowInEasing)) { value, _ ->
                    glitchIntensity = value
                }
            }

            Box(modifier = Modifier.fillMaxSize().cyberGlitch(glitchIntensity)) {
                CoreDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onTriggerHack = { context -> navController.navigate("ice_breach/$context") },
                    unlockedSectionFromResult = unlockedSection,
                    onUnlockConsumed = {
                        backStackEntry.savedStateHandle.remove<String>("unlocked_section")
                    }
                )
            }
        }

        composable("journal") {
            JournalScreen(
                onEntryClick = { /* TODO: Navigate to entry detail */ },
                onStoryClick = { navController.navigate("story") },
                onBack = { navController.popBackStack() },
                onHackingRequired = { navController.navigate("system_breach") }
            )
        }

        composable("story") {
            StoryScreen(
                onBack = { navController.popBackStack() },
                onHackingRequired = { navController.navigate("system_breach") }
            )
        }

        composable("creation") {
            CharacterCreationScreen(
                onCreationFinished = { name, sex, dob, units, weight, somatotype, hFeet, hInches, hCm ->
                    creationViewModel.updateBasicInfo(name, sex, dob, units, weight, somatotype, hFeet, hInches, hCm)
                    navController.navigate("personality_intake")
                }
            )
        }

        composable("personality_intake") {
            NeuralScanScreen(
                onComplete = { answers ->
                    // Derive and update personality info
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

                    val (archetype, _) = deriveArchetype(mbti, alignment)
                    creationViewModel.updatePersonality(mbti, alignment, archetype)
                    navController.navigate("avatar_capture")
                }
            )
        }

        composable("avatar_capture") {
            AvatarCaptureScreen(
                onComplete = { bitmap ->
                    creationViewModel.completeCreation(bitmap)
                    navController.navigate("main_hub") {
                        popUpTo("creation") { inclusive = true }
                    }
                }
            )
        }

        composable("attribute_scan") {
            NeuralScanScreen(onComplete = { navController.popBackStack() })
        }

        composable("wallet") {
            EurodollarWalletScreen(onBack = { navController.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onResetComplete = {
                    navController.navigate("loading") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeepNodeUnlock = {
                    navController.navigate("deep_node/ROOT")
                }
            )
        }

        composable("deep_node/{nodeType}") { backStackEntry ->
            val nodeType = backStackEntry.arguments?.getString("nodeType") ?: "DEUS_EX_MACHINA"
            DeepNodeScreen(
                initialSubScreen = nodeType,
                onBack = { navController.popBackStack() },
                onReaderNavigate = { id, path ->
                    navController.navigate("e_reader/$id?assetPath=$path")
                }
            )
        }

        composable("character_bio") {
            AvatarCaptureScreen(onComplete = { navController.popBackStack() })
        }

        composable(
            route = "e_reader/{bookId}?assetPath={assetPath}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("assetPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val assetPath = backStackEntry.arguments?.getString("assetPath") ?: ""
            EReaderScreen(
                bookId = bookId,
                bookAssetPath = assetPath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
