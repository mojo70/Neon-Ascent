package com.neon.ascent.feature.workout.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.domain.workout.models.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
    ) {
        // Neural Progress Header
        NeuralProgressHeader(
            currentStep = uiState.currentStep,
            totalSteps = 5,
            onBack = { viewModel.prevStep() }
        )

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> StepSyncing(uiState)
                    2 -> StepExperience(uiState, onSelect = { viewModel.updateExperience(it) })
                    3 -> StepAttributeCalibration(uiState)
                    4 -> StepHardwareCheck(uiState, onToggle = { viewModel.toggleInjury(it) })
                    5 -> StepProtocolSynthesis(uiState)
                }
            }
        }

        // Bottom Navigation
        OnboardingBottomBar(
            uiState = uiState,
            onNext = { viewModel.nextStep() }
        )
    }
}

@Composable
fun NeuralProgressHeader(currentStep: Int, totalSteps: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentStep > 1) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "NEURAL INITIALIZATION",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            if (currentStep > 1) Box(Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..totalSteps) {
                val color = when {
                    i < currentStep -> Color(0xFF00FF9C)
                    i == currentStep -> Color(0xFF00CCFF)
                    else -> Color.DarkGray
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(color, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

@Composable
fun StepSyncing(state: OnboardingUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Sync,
            contentDescription = null,
            tint = Color(0xFF00CCFF),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "SYNCING BIOMETRICS",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "Establishing neural link with primary intake data...",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Profile Preview
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00CCFF).copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                SyncRow("AGE", "${state.profile.age}")
                SyncRow("WEIGHT", "${"%.1f".format(state.profile.weightKg)} KG")
                SyncRow("SOMATOTYPE", state.profile.somatotype.name)
                SyncRow("UNITS", state.profile.unitSystem.name)
            }
        }
    }
}

@Composable
fun SyncRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color(0xFF00CCFF), fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun StepExperience(state: OnboardingUiState, onSelect: (ExperienceLevel) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "NEURAL EXPERIENCE",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "Calibrate protocol complexity based on training history.",
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        ExperienceCard(
            level = ExperienceLevel.NOVICE,
            title = "NOVICE",
            desc = "< 1 year consistent training. Focus on motor learning and neural pathways.",
            isSelected = state.profile.experienceLevel == ExperienceLevel.NOVICE,
            onClick = { onSelect(ExperienceLevel.NOVICE) }
        )
        ExperienceCard(
            level = ExperienceLevel.INTERMEDIATE,
            title = "INTERMEDIATE",
            desc = "1-3 years. Foundational strength established. Ready for advanced protocols.",
            isSelected = state.profile.experienceLevel == ExperienceLevel.INTERMEDIATE,
            onClick = { onSelect(ExperienceLevel.INTERMEDIATE) }
        )
        ExperienceCard(
            level = ExperienceLevel.ADVANCED,
            title = "ADVANCED",
            desc = "3+ years. High work capacity. CyberCrapp optimization recommended.",
            isSelected = state.profile.experienceLevel == ExperienceLevel.ADVANCED,
            onClick = { onSelect(ExperienceLevel.ADVANCED) }
        )
    }
}

@Composable
fun ExperienceCard(level: ExperienceLevel, title: String, desc: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        color = if (isSelected) Color(0xFF00CCFF).copy(alpha = 0.1f) else Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00CCFF) else Color.Transparent)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00CCFF)))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(desc, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun StepAttributeCalibration(state: OnboardingUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "ATTRIBUTE CALIBRATION",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Analyzing biometric performance markers...",
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp).fillMaxWidth()
        )

        if (state.hasScanData) {
            AttributeSyncPanel(state)
        } else {
            NoScanPanel()
        }
    }
}

@Composable
fun AttributeSyncPanel(state: OnboardingUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FF9C))
                Spacer(modifier = Modifier.width(12.dp))
                Text("SCAN DATA DETECTED", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            AttributeBar("STRENGTH", state.scanStrength ?: 0)
            AttributeBar("ENDURANCE", state.scanEndurance ?: 0)
            AttributeBar("AGILITY", state.scanAgility ?: 0)
        }
    }
}

@Composable
fun NoScanPanel() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1C1C1E).copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("NO SCAN DATA FOUND", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Primary attribute levels are unknown. Manual estimates will be less accurate.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { /* Navigate to Scan */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF))
        ) {
            Text("PERFORM ATTRIBUTE SCAN", color = Color.Black, fontWeight = FontWeight.Black)
        }
        TextButton(onClick = { /* Task Logic */ }, modifier = Modifier.padding(top = 16.dp)) {
            Text("REMIND ME TOMORROW", color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AttributeBar(label: String, value: Int) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("$value", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(2.dp).clip(CircleShape),
            color = Color(0xFF00FF9C),
            trackColor = Color.DarkGray
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepHardwareCheck(state: OnboardingUiState, onToggle: (String) -> Unit) {
    val bodyParts = listOf("Left Shoulder", "Right Shoulder", "Lower Back", "Left Knee", "Right Knee", "Neck", "Wrist")
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "HARDWARE CHECK",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "Flag existing physical constraints for auto-substitution.",
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            bodyParts.forEach { part ->
                val isSelected = state.profile.injuries.contains(part)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(part) },
                    label = { Text(part.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Red.copy(alpha = 0.2f),
                        selectedLabelColor = Color.Red,
                        labelColor = Color.Gray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        selectedBorderColor = Color.Red,
                        borderColor = Color.DarkGray
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepProtocolSynthesis(state: OnboardingUiState) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "PROTOCOL SYNTHESIS",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Optimizing training parameters for your morphology.",
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp).fillMaxWidth()
        )

        state.recommendation?.let { routine ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFF00FF9C))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("RECOMMENDED CORE", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text(routine.name.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    routine.description?.let {
                        Text(it, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("NEURAL MODIFIERS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    ModifierBadge("CYBERCRAPP ENABLED", Color(0xFF00FFAA))
                    if (state.profile.somatotype == Somatotype.ECTOMORPH) ModifierBadge("+10% VOLUME NUDGE", Color(0xFF00CCFF))
                    if (state.profile.injuries.isNotEmpty()) ModifierBadge("AUTO-SUBSTITUTIONS ACTIVE", Color.Red)
                }
            }
        }
    }
}

@Composable
fun ModifierBadge(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun OnboardingBottomBar(uiState: OnboardingUiState, onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (uiState.currentStep == 5) "INITIALIZE UPLINK" else "PROCEED",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black)
            }
        }
    }
}
