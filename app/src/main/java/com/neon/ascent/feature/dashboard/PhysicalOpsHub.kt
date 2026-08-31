package com.neon.ascent.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.ui.CyberCutShape
import com.neon.ascent.ui.SoftGridBackground
import com.neon.ascent.core.common.*
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.common.Vignette

@Composable
fun PhysicalOpsHub(
    onNavigateToWorkout: () -> Unit,
    onNavigateToStilljack: () -> Unit,
    onNavigateToHullPulse: () -> Unit,
    onNavigateToArchive: () -> Unit = {}
) {
    val theme = LocalNeonTheme.current
    val systemColor = theme.accent

    Box(modifier = Modifier.fillMaxSize().background(theme.canvas)) {
        if (theme.mode == VisualMode.CYBER) {
            SoftGridBackground()
            Vignette()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "PHYSICAL_OPERATIONS // HUB",
                color = systemColor,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            PhysicalOpCard(
                title = "NEURAL_ARCHIVE",
                subtitle = "MISSION LOGS & BIOMETRIC HISTORY",
                icon = Icons.Default.History,
                label = "OPEN ARCHIVE",
                color = systemColor,
                onClick = onNavigateToArchive
            )

            Spacer(Modifier.height(16.dp))

            PhysicalOpCard(
                title = "MEATWARE_TRAINING",
                subtitle = "PHYSICAL EXERTION & STRENGTH",
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                label = "INITIATE WORKOUT",
                color = systemColor,
                onClick = onNavigateToWorkout
            )

            Spacer(Modifier.height(16.dp))

            PhysicalOpCard(
                title = "STILLJACK",
                subtitle = "NEURAL CALM & MEDITATION",
                icon = Icons.Default.SelfImprovement,
                label = "INITIATE SESSION",
                color = if (theme.mode == VisualMode.STEVE) theme.ink else Color(0xFF00CCFF),
                onClick = onNavigateToStilljack
            )

            Spacer(Modifier.height(16.dp))

            PhysicalOpCard(
                title = "HULL_PULSE",
                subtitle = "PELVIC CORE & KEGEL REINFORCEMENT",
                icon = Icons.Default.MonitorHeart,
                label = "INITIATE PULSE",
                color = if (theme.mode == VisualMode.STEVE) theme.ink else Color(0xFFFF006E),
                onClick = onNavigateToHullPulse
            )

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun PhysicalOpCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.overlay, CyberCutShape)
            .border(1.dp, color.copy(alpha = 0.2f), CyberCutShape)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = color,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = theme.inkMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "$label >>",
                    color = color,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
