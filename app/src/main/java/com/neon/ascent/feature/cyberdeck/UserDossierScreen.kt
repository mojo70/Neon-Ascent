package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.feature.dashboard.DashboardViewModel
import com.neon.ascent.ui.CyberFrame

@Composable
fun UserDossierScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val userCharacter by viewModel.userCharacter.collectAsState()
    val state by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020508))) {
        PerspectiveGrid()
        Scanlines(intensity = 0.1f)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF00FF9F))
                }
                Column {
                    Text(
                        "// NETWATCH_SURVEILLANCE_DOSSIER",
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        userCharacter?.netrunnerName?.uppercase() ?: "RUNNER_UNKNOWN",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Identity Section
                item {
                    DossierSection(
                        title = "IDENT_CONFIRMATION",
                        content = "NAME: ${userCharacter?.name}\nDOB: ${userCharacter?.dob}\nARCHETYPE: ${userCharacter?.archetype}\nLEVEL: ${userCharacter?.level}",
                        isSealed = false,
                        accentColor = Color(0xFF00FF9F)
                    )
                }

                // Story / Bio Section
                item {
                    DossierSection(
                        title = "NEURAL_NARRATIVE",
                        content = state.userStory.cyberLore,
                        isSealed = false,
                        accentColor = Color(0xFF00FFFF)
                    )
                }

                // Accomplishments Section (Sealed if level < 5)
                item {
                    DossierSection(
                        title = "CRIMINAL_RECORD // ACCOMPLISHMENTS",
                        content = if (state.terminalFeed.isEmpty()) "NO_DATA_FOUND" else state.terminalFeed.joinToString("\n") { "[${it.type}] ${it.title}: ${it.status}" },
                        isSealed = (userCharacter?.level ?: 1) < 5,
                        accentColor = Color.Yellow
                    )
                }

                // Netwatch Observation (Always sealed for now)
                item {
                    DossierSection(
                        title = "NETWATCH_INTERNAL_NOTES",
                        content = "Priority Alpha target. Displays erratic neural patterns. Use of local AI confirmed. Recommend immediate isolation if ICE level reaches 10.",
                        isSealed = true,
                        accentColor = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun DossierSection(
    title: String,
    content: String,
    isSealed: Boolean,
    accentColor: Color
) {
    CyberFrame(
        label = if (isSealed) "SEALED_BY_NETWATCH" else title,
        borderColor = if (isSealed) Color.Red else accentColor.copy(alpha = 0.6f)
    ) {
        if (isSealed) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "CLASSIFIED_DATA_LOCKED",
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Insufficient clearance to view this encrypted sector.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Text(
                text = content,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}
