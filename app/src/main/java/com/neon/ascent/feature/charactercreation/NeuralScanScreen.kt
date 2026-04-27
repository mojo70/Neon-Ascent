package com.neon.ascent.feature.charactercreation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.ui.CyberGridBackground
import com.neon.ascent.ui.GlitchOverlay
import com.neon.ascent.ui.CyberButtonShape
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.util.derivePersonalityArchetype

@Composable
fun NeuralScanScreen(onComplete: (Map<String, String>) -> Unit) {
    var currentStep by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }
    
    val questions = listOf(
        QuizQuestion(
            id = "ENERGY_SOURCE", 
            title = "ENERGY_FLOW", 
            prompt = "WHERE IS YOUR PRIMARY POWER CELL DIRECTED?", 
            options = listOf("SOLO_CORE (INTRO)", "NET_LINK (EXTRA)")
        ),
        QuizQuestion(
            id = "INPUT_METHOD", 
            title = "DATA_RECEPTION", 
            prompt = "HOW DO YOU CAPTURE ENVIRONMENTAL DATA?", 
            options = listOf("SENSORY_HARDWARE (SENSING)", "PATTERN_INTUITION (INTUITION)")
        ),
        QuizQuestion(
            id = "LOGIC_GATE", 
            title = "EXECUTION_LOGIC", 
            prompt = "PRIMARY DECISION ARCHITECTURE?", 
            options = listOf("CYBER_LOGIC (THINKING)", "EMPATHY_BUFFER (FEELING)")
        ),
        QuizQuestion(
            id = "SYSTEM_EXECUTION", 
            title = "EXTERNAL_PROTOCOL", 
            prompt = "HOW DO YOU MAINTAIN SYSTEM STABILITY?", 
            options = listOf("STRICT_SCHEDULE (JUDGING)", "FLUID_ADAPT (PERCEIVING)")
        ),
        QuizQuestion(
            id = "OPERATIONAL_CODE", 
            title = "SOCIETAL_OVERRIDE", 
            prompt = "ATTITUDE TOWARDS NIGHT CITY PROTOCOLS?", 
            options = listOf("FOLLOW_CODE (LAWFUL)", "GREY_AREA (NEUTRAL)", "BREAK_SYSTEM (CHAOTIC)")
        ),
        QuizQuestion(
            id = "MORAL_COMPASS", 
            title = "CORE_INTEGRITY", 
            prompt = "PRIORITY DURING A SYSTEM CRITICAL FAILURE?", 
            options = listOf("RESCUE_OTHERS (GOOD)", "CALCULATED_NEUTRAL", "EXPLOIT_VOID (EVIL)")
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        CyberGridBackground()
        GlitchOverlay()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                "NEURAL SCAN IN PROGRESS: ${((currentStep.toFloat() / questions.size) * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFF006E)
            )
            
            LinearProgressIndicator(
                progress = { if (questions.isEmpty()) 0f else currentStep.toFloat() / questions.size },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(2.dp),
                color = Color(0xFF00FF9C),
                trackColor = Color.DarkGray
            )

            if (currentStep < questions.size) {
                val q = questions[currentStep]
                
                Text(
                    q.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF00FF9C),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 24.dp)
                )
                
                Text(
                    q.prompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                q.options.forEach { option ->
                    Button(
                        onClick = {
                            answers[q.id] = option
                            currentStep++
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(56.dp)
                            .clip(CyberButtonShape)
                            .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f), CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F))
                    ) {
                        Text(option, color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                ArchetypeResultView(answers) { onComplete(answers) }
            }
        }
    }
}

data class QuizQuestion(val id: String, val title: String, val prompt: String, val options: List<String>)

@Composable
fun ArchetypeResultView(answers: Map<String, String>, onFinish: () -> Unit) {
    val energy = if (answers["ENERGY_SOURCE"]?.contains("SOLO") == true) "I" else "E"
    val info = if (answers["INPUT_METHOD"]?.contains("SENSORY") == true) "S" else "N"
    val decision = if (answers["LOGIC_GATE"]?.contains("CYBER") == true) "T" else "F"
    val structure = if (answers["SYSTEM_EXECUTION"]?.contains("STRICT") == true) "J" else "P"
    val mbti = "$energy$info$decision$structure"
    
    val alignmentLaw = when {
        answers["OPERATIONAL_CODE"]?.contains("FOLLOW") == true -> "Lawful"
        answers["OPERATIONAL_CODE"]?.contains("BREAK") == true -> "Chaotic"
        else -> "Neutral"
    }
    val alignmentMorality = when {
        answers["MORAL_COMPASS"]?.contains("RESCUE") == true -> "Good"
        answers["MORAL_COMPASS"]?.contains("EXPLOIT") == true -> "Evil"
        else -> "Neutral"
    }
    val alignment = if (alignmentLaw == "Neutral" && alignmentMorality == "Neutral") "True Neutral" else "$alignmentLaw $alignmentMorality"

    val (archetype, description) = derivePersonalityArchetype(mbti, alignment)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SCAN COMPLETE", color = Color(0xFF00FF9C), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        
        CyberFrame(label = "DERIVED ARCHETYPE: $archetype") {
            Column {
                Text("MBTI_TAG: $mbti", color = Color.White, fontWeight = FontWeight.Bold)
                Text("ALIGNMENT: $alignment", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(description, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(CyberButtonShape)
                .background(Color(0xFF00FF9C))
                .border(1.dp, Color.White.copy(alpha = 0.5f), CyberButtonShape),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Text("SYNC BIOMETRICS & AVATAR", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}


