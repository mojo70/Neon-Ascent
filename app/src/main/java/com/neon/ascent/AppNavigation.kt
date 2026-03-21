package com.neon.ascent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neon.ascent.feature.charactercreation.CharacterCreationScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "creation") {
            composable("creation") {
                CharacterCreationScreen()
            }
            // Add more routes: "quiz", "tests", "sheet"
        }
    }
}
