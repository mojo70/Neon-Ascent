package com.neon.ascent

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neon.ascent.feature.charactercreation.CharacterCreationScreen
import com.neon.ascent.feature.charactercreation.NeuralScanScreen
import com.neon.ascent.feature.charactercreation.AvatarCaptureScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "creation") {
        composable("creation") {
            CharacterCreationScreen(
                onInitialize = { 
                    navController.navigate("neural_scan")
                }
            )
        }
        composable("neural_scan") {
            NeuralScanScreen(
                onComplete = { answers ->
                    navController.navigate("avatar_capture")
                }
            )
        }
        composable("avatar_capture") {
            AvatarCaptureScreen(
                onComplete = { bitmap ->
                    // For now, navigate back to start or stay
                    navController.popBackStack("creation", inclusive = false)
                }
            )
        }
    }
}
