package com.neon.ascent.feature.story

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.Scanlines
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryIntakeScreen(
    viewModel: StoryIntakeViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val step by viewModel.currentStep.collectAsState()
    val answers by viewModel.answers.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020508))) {
        Scanlines()
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "YOUR_CYBER_LORE // STEP $step.4", 
                            color = Color(0xFF00FFAA),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp
                        ) 
                    },
                    navigationIcon = {
                        if (step > 1) {
                            IconButton(onClick = { viewModel.previousStep() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF00FFAA))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    when (step) {
                        1 -> Step1_Origin(viewModel, answers)
                        2 -> Step2_Present(viewModel, answers)
                        3 -> Step3_Aspirations(viewModel, answers)
                        4 -> Step4_CyberFlavor(viewModel, answers)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { viewModel.previousStep() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FFAA)),
                            border = BorderStroke(1.dp, Color(0xFF00FFAA))
                        ) {
                            Text("BACK", fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (step < 4) viewModel.nextStep()
                            else {
                                scope.launch {
                                    if (viewModel.finishAndSave()) onComplete()
                                }
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FFAA),
                            contentColor = Color.Black
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                        } else {
                            Text(
                                if (step == 4) "FORGE_MY_LEGEND" else "CONTINUE",
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step1_Origin(viewModel: StoryIntakeViewModel, answers: Map<Int, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Where did your journey begin?", 
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        OutlinedTextField(
            value = answers[1] ?: "",
            onValueChange = { viewModel.saveAnswer(1, it) },
            label = { Text("Origin (e.g. Neon District, Corporate Highrise...)", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FFAA),
                unfocusedBorderColor = Color.Gray
            )
        )
    }
}

@Composable
fun Step2_Present(viewModel: StoryIntakeViewModel, answers: Map<Int, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "What was the turning point in your life?", 
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        OutlinedTextField(
            value = answers[3] ?: "",
            onValueChange = { viewModel.saveAnswer(3, it) },
            label = { Text("Turning Point (e.g. System Crash, Neural Awakening...)", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FFAA),
                unfocusedBorderColor = Color.Gray
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3_Aspirations(viewModel: StoryIntakeViewModel, answers: Map<Int, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "What is your grandest aspiration?", 
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        OutlinedTextField(
            value = answers[7] ?: "",
            onValueChange = { viewModel.saveAnswer(7, it) },
            label = { Text("Future Vision (e.g. Become enlightened, Master meditation...)", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FFAA),
                unfocusedBorderColor = Color.Gray
            )
        )

        // Quick chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Enlightened", "Meditation Master", "Unbreakable Focus", "Biological Immortal", "Cybernetic Legend").forEach { chip ->
                SuggestionChip(
                    onClick = { 
                        val current = answers[7] ?: ""
                        val newValue = if (current.isEmpty()) chip else "$current, $chip"
                        viewModel.saveAnswer(7, newValue) 
                    },
                    label = { Text(chip, fontFamily = FontFamily.Monospace) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        labelColor = Color(0xFF00FFAA)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFF00FFAA).copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun Step4_CyberFlavor(viewModel: StoryIntakeViewModel, answers: Map<Int, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Current path and proudest moment?", 
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        OutlinedTextField(
            value = answers[4] ?: "",
            onValueChange = { viewModel.saveAnswer(4, it) },
            label = { Text("Current Path (e.g. Freelance Hacker, Zen Nomad...)", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FFAA),
                unfocusedBorderColor = Color.Gray
            )
        )

        OutlinedTextField(
            value = answers[5] ?: "",
            onValueChange = { viewModel.saveAnswer(5, it) },
            label = { Text("Proudest Moment", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FFAA),
                unfocusedBorderColor = Color.Gray
            )
        )
    }
}
