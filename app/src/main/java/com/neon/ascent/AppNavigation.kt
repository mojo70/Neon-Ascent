package com.neon.ascent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neon.ascent.feature.biohacking.BiohackingScreen
import com.neon.ascent.feature.charactercreation.AvatarCaptureScreen
import com.neon.ascent.feature.charactercreation.CharacterCreationScreen
import com.neon.ascent.feature.charactercreation.CreationViewModel
import com.neon.ascent.feature.charactercreation.NeuralScanScreen
import com.neon.ascent.feature.cyberdeck.CyberdeckScreen
import com.neon.ascent.feature.dashboard.DashboardScreen
import com.neon.ascent.feature.dashboard.DashboardViewModel
import com.neon.ascent.feature.dashboard.CoreDashboardScreen
import com.neon.ascent.feature.games.IceBreachScreen
import com.neon.ascent.feature.games.BlackIceBreachScreen
import com.neon.ascent.feature.journal.JournalScreen
import com.neon.ascent.feature.loading.LoadingScreen
import com.neon.ascent.feature.settings.DeepNodeScreen
import com.neon.ascent.feature.settings.SettingsScreen
import com.neon.ascent.feature.wallet.EurodollarWalletScreen
import kotlin.random.Random

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
                        onDatabaseClick = { navController.navigate("journal") },
                        onIceBreachClick = { 
                            val gameType = if (Random.nextBoolean()) "ice_breach" else "black_ice"
                            navController.navigate(gameType)
                        },
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

        composable("ice_breach") {
            IceBreachScreen(
                onBreachSuccess = {
                    navController.navigate("core_dashboard") {
                        popUpTo("ice_breach") { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable("black_ice") {
            BlackIceBreachScreen(
                onBreachSuccess = {
                    navController.navigate("core_dashboard") {
                        popUpTo("black_ice") { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable("core_dashboard") {
            CoreDashboardScreen(onBack = { navController.popBackStack() })
        }

        composable("journal") {
            JournalScreen(onBack = { navController.popBackStack() })
        }

        composable("creation") {
            CharacterCreationScreen(
                onCreationFinished = { name, sex, dob, units, weight, somatotype, hFeet, hInches, hCm ->
                    creationViewModel.updateBasicInfo(name, sex, dob, units, weight, somatotype, hFeet, hInches, hCm)
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
            val nodeType = backStackEntry.arguments?.getString("nodeType") ?: "RELIGION"
            DeepNodeScreen(initialSubScreen = nodeType, onBack = { navController.popBackStack() })
        }

        composable("character_bio") {
            AvatarCaptureScreen(onComplete = { navController.popBackStack() })
        }
    }
}
