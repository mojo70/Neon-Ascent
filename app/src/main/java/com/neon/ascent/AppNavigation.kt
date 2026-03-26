package com.neon.ascent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neon.ascent.feature.charactercreation.CharacterCreationScreen
import com.neon.ascent.feature.charactercreation.NeuralScanScreen
import com.neon.ascent.feature.charactercreation.AvatarCaptureScreen
import com.neon.ascent.feature.charactercreation.CreationViewModel
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import com.neon.ascent.feature.dashboard.DashboardScreen
import com.neon.ascent.feature.dashboard.DashboardViewModel
import com.neon.ascent.feature.dashboard.HolographicAvatarHub
import com.neon.ascent.feature.games.CyberPongScreen
import com.neon.ascent.feature.settings.SettingsScreen
import com.neon.ascent.feature.settings.DeepNodeScreen
import com.neon.ascent.feature.loading.LoadingScreen
import com.neon.ascent.feature.cyberdeck.CyberdeckScreen
import com.neon.ascent.feature.biohacking.BiohackingScreen
import com.neon.ascent.feature.wallet.EurodollarWalletScreen
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNavigation(
    creationViewModel: CreationViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val userCharacter by dashboardViewModel.userCharacter.collectAsState()
    val tickerMessages by dashboardViewModel.tickerMessages.collectAsState()

    NavHost(navController = navController, startDestination = "loading") {
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
                        tickerMessages = tickerMessages
                    )
                    1 -> DashboardScreen(
                        onAvatarClick = { navController.navigate("character_bio") },
                        onAttributeSetClick = { navController.navigate("attribute_scan") },
                        onStoryClick = { navController.navigate("story") },
                        onGoalSetClick = { navController.navigate("goals") },
                        onSettingsClick = { navController.navigate("settings") },
                        onReligionClick = { navController.navigate("deep_node/RELIGION") }
                    )
                    2 -> BiohackingScreen(onBack = { /* Handled by pager */ })
                }
            }
        }

        composable("creation") {
            CharacterCreationScreen(
                onInitialize = { name, sex, dob, units, weight, somatotype, ft, inches, cm ->
                    creationViewModel.updateBasicInfo(name, sex, dob, units, weight, somatotype, ft, inches, cm)
                    navController.navigate("neural_scan")
                }
            )
        }
        composable("neural_scan") {
            NeuralScanScreen(
                onComplete = { answers ->
                    val mbti = deriveMbti(answers)
                    val alignment = deriveAlignment(answers)
                    val archetype = deriveArchetype(mbti, alignment).first
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
        
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onResetComplete = {
                    navController.navigate("creation") {
                        popUpTo("main_hub") { inclusive = true }
                    }
                },
                onDeepNodeUnlock = {
                    navController.navigate("deep_node/ROOT")
                }
            )
        }

        composable(
            route = "deep_node/{subScreen}",
            arguments = listOf(navArgument("subScreen") { type = NavType.StringType })
        ) { backStackEntry ->
            val subScreen = backStackEntry.arguments?.getString("subScreen") ?: "ROOT"
            DeepNodeScreen(
                initialSubScreen = subScreen,
                onBack = { navController.popBackStack() },
                onGameSelect = { gameId ->
                    if (gameId == "PONG") navController.navigate("cyber_pong")
                },
                onRebirthSuccess = {
                    navController.navigate("main_hub") {
                        popUpTo("deep_node/{subScreen}") { inclusive = true }
                    }
                }
            )
        }

        composable("cyber_pong") {
            CyberPongScreen(onBack = { navController.popBackStack() })
        }
        
        composable("wallet") {
            EurodollarWalletScreen(onBack = { navController.popBackStack() })
        }
        
        composable("character_bio") { 
            HolographicAvatarHub(
                onBack = { navController.popBackStack() },
                onUpgradeClick = { sector ->
                    navController.navigate("goals")
                },
                onHacksClick = {
                    // Biohacking is now a main tab, but keeping this for deep linking if needed
                },
                onAttributeScanClick = {
                    navController.navigate("attribute_scan")
                },
                onStoryClick = {
                    navController.navigate("story")
                },
                onGoalSettingClick = {
                    navController.navigate("goals")
                }
            )
        }

        composable("attribute_scan") { PlaceholderScreen("ATTRIBUTE SCAN", navController::popBackStack) }
        composable("story") { PlaceholderScreen("YOUR STORY", navController::popBackStack) }
        composable("goals") { PlaceholderScreen("GOAL SETTING", navController::popBackStack) }
    }
}

@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CyberGridBackground()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color(0xFF00FF9C), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("RETURN")
            }
        }
    }
}

private fun deriveMbti(answers: Map<String, String>): String {
    val energy = if (answers["ENERGY_SOURCE"]?.contains("SOLO") == true) "I" else "E"
    val info = if (answers["INPUT_METHOD"]?.contains("SENSORY") == true) "S" else "N"
    val decision = if (answers["LOGIC_GATE"]?.contains("CYBER") == true) "T" else "F"
    val structure = if (answers["SYSTEM_EXECUTION"]?.contains("STRICT") == true) "J" else "P"
    return "$energy$info$decision$structure"
}

private fun deriveAlignment(answers: Map<String, String>): String {
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
    return if (alignmentLaw == "Neutral" && alignmentMorality == "Neutral") "True Neutral" else "$alignmentLaw $alignmentMorality"
}

private fun deriveArchetype(mbti: String, alignment: String): Pair<String, String> {
    return when {
        mbti.startsWith("INF") && alignment.contains("Good") -> 
            "THE IDEALIST" to "Driven by strong values and a desire to help others."
        mbti.startsWith("INT") -> 
            "THE STRATEGIST" to "Analytical and goal-oriented."
        mbti.contains("ENF") && alignment.contains("Chaotic") -> 
            "THE ADVOCATE" to "Enthusiastic and inspiring."
        mbti.contains("IST") -> 
            "THE PRAGMATIST" to "Observant and adaptable."
        else -> 
            "THE EDGE-RUNNER" to "A versatile survivalist."
    }
}
