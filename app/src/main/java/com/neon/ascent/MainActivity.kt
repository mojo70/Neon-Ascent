package com.neon.ascent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.neon.ascent.feature.charactercreation.CharacterCreationScreen
import com.neon.ascent.ui.theme.NeonAscentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeonAscentTheme {
                CharacterCreationScreen()
            }
        }
    }
}
