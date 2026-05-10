package com.neon.ascent.core.common

import androidx.compose.ui.graphics.Color

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import com.neon.ascent.core.domain.model.SpecialType

val NeonCyan = Color(0xFF00FFFF)
val NeonPink = Color(0xFFFF006E)
val NeonPurple = Color(0xFFA020F0)
val NeonGreen = Color(0xFF00FF9C)
val NeonRed = Color(0xFFFF3131)
val NeonBlue = Color(0xFF0088FF)
val NeonYellow = Color(0xFFFFD700)
val NeonOrange = Color(0xFFFF8C00)

fun getNeonColorForAttribute(type: SpecialType): Color = when (type) {
    SpecialType.STRENGTH -> NeonRed
    SpecialType.PERCEPTION -> NeonPurple
    SpecialType.ENDURANCE -> NeonGreen
    SpecialType.CHARISMA -> NeonPink
    SpecialType.INTELLIGENCE -> NeonCyan
    SpecialType.AGILITY -> NeonBlue
    SpecialType.LUCK -> NeonYellow
}

@androidx.compose.runtime.Composable
fun neonTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
    focusedLabelColor = NeonCyan,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
    cursorColor = NeonPink
)
