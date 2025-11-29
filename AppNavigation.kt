package com.neon.ascent

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neon.ascent.feature.charactercreation.CharacterCreationScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "creation") {
        composable("creation") {
            CharacterCreationScreen()
        }
        // Add more routes: "quiz", "tests", "sheet"
    }
}
