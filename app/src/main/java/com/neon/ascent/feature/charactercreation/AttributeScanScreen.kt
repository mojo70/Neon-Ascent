package com.neon.ascent.feature.charactercreation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.*
import com.neon.ascent.util.CalculatedScores
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

@Composable
fun AttributeScanScreen(
    onComplete: () -> Unit,
    viewModel: AttributeScanViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val inputs by viewModel.inputs.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF010101))) {
        PerspectiveGrid()
        Scanlines(intensity = 0.2f)
        
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header with Progress and Abort
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStep in 1..7) {
                    Box(modifier = Modifier.weight(1f)) {
                        AttributeProgressBar(currentStep)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Surface(
                    onClick = { viewModel.abort(onComplete) },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF006E).copy(alpha = 0.5f)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "ABORT",
                            color = Color(0xFFFF006E),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "StepTransition",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> IntroStep(onBegin = { viewModel.nextStep() })
                    1 -> StrengthStep(
                        pushups = inputs.pushups ?: 0,
                        squats = inputs.squats ?: 0,
                        onUpdate = { p, s -> viewModel.updateInputs { it.copy(pushups = p, squats = s) } },
                        onNext = { viewModel.nextStep() }
                    )
                    2 -> EnduranceStep(
                        plankSeconds = inputs.plankSeconds ?: 0,
                        onUpdate = { value -> viewModel.updateInputs { it.copy(plankSeconds = value) } },
                        onNext = { viewModel.nextStep() }
                    )
                    3 -> AgilityStep(
                        balanceSeconds = inputs.balanceSeconds ?: 0,
                        onUpdate = { value -> viewModel.updateInputs { it.copy(balanceSeconds = value) } },
                        onNext = { viewModel.nextStep() }
                    )
                    4 -> PerceptionStep(
                        onReactionComplete = { ms -> 
                            viewModel.updateInputs { it.copy(reactionTimeMs = ms) }
                            viewModel.nextStep()
                        }
                    )
                    5 -> IntelligenceStep(
                        onComplete = { score ->
                            viewModel.updateInputs { it.copy(patternScore = score) }
                            viewModel.nextStep()
                        }
                    )
                    6 -> CharismaStep(
                        onComplete = { score ->
                            viewModel.updateInputs { it.copy(scenarioScore = score) }
                            viewModel.nextStep()
                        }
                    )
                    7 -> LuckStep(
                        onComplete = { heads ->
                            viewModel.updateInputs { it.copy(coinFlipsHeads = heads) }
                            viewModel.calculateResults { viewModel.nextStep() }
                        }
                    )
                    8 -> ResultsStep(
                        viewModel = viewModel,
                        result = scanResult,
                        onSave = {
                            viewModel.saveResults()
                            onComplete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AttributeProgressBar(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..7) {
            val isActive = i <= currentStep
            val color = if (isActive) Color(0xFF00FF9C) else Color.White.copy(alpha = 0.1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(color)
                    .then(if (isActive) Modifier.neonBorder(color, width = 1.dp, cornerRadius = 0.dp) else Modifier)
            )
        }
    }
}

@Composable
fun IntroStep(onBegin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CyberHelmetIcon(modifier = Modifier.size(200.dp), neuralLoad = 0.5f)
        Spacer(modifier = Modifier.height(32.dp))
        GlitchText("FORGE_YOUR_STATS", fontSize = 32.sp)
        Text(
            "INITIATING BIOMETRIC_SCAN_PROTOCOL_V2.0",
            color = Color.White.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))
        CyberActionButton("BEGIN_ACTIVATION", Color(0xFF00FF9C), onClick = onBegin)
    }
}

@Composable
fun StrengthStep(pushups: Int, squats: Int, onUpdate: (Int, Int) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("STRENGTH_PROTOCOL", color = Color(0xFFFF006E), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        
        CyberFrame(label = "QUICK_MODE: REPS_IN_60S", borderColor = Color(0xFFFF006E)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AttributeInputField(label = "MAX_PUSH_UPS", value = pushups.toString(), onValueChange = { onUpdate(it.toIntOrNull() ?: 0, squats) })
                AttributeInputField(label = "MAX_SQUATS", value = squats.toString(), onValueChange = { onUpdate(pushups, it.toIntOrNull() ?: 0) })
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        CyberActionButton("VALIDATE_STRENGTH", Color(0xFFFF006E), onClick = onNext)
    }
}

@Composable
fun EnduranceStep(plankSeconds: Int, onUpdate: (Int) -> Unit, onNext: () -> Unit) {
    var isTimerRunning by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(plankSeconds) }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                seconds++
                onUpdate(seconds)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("ENDURANCE_LINK", color = Color(0xFF00FFFF), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        
        CyberFrame(label = "CORE_STABILITY", borderColor = Color(0xFF00FFFF)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60),
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                
                CyberActionButton(
                    if (isTimerRunning) "STOP_CHRONO" else "START_PLANK_TIMER",
                    if (isTimerRunning) Color.Red else Color(0xFF00FFFF)
                ) {
                    isTimerRunning = !isTimerRunning
                }
                
                AttributeInputField(label = "MANUAL_ENTRY_SEC", value = seconds.toString(), onValueChange = { 
                    val v = it.toIntOrNull() ?: 0
                    seconds = v
                    onUpdate(v) 
                })
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        CyberActionButton("SYNC_ENDURANCE", Color(0xFF00FFFF), onClick = onNext)
    }
}

@Composable
fun AgilityStep(balanceSeconds: Int, onUpdate: (Int) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("AGILITY_CALIBRATION", color = Color(0xFFFFFF00), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        
        CyberFrame(label = "BALANCE_TEST", borderColor = Color(0xFFFFFF00)) {
            AttributeInputField(label = "SINGLE_LEG_BALANCE_SEC", value = balanceSeconds.toString(), onValueChange = { onUpdate(it.toIntOrNull() ?: 0) })
        }
        
        Spacer(modifier = Modifier.weight(1f))
        CyberActionButton("RECORD_AGILITY", Color(0xFFFFFF00), onClick = onNext)
    }
}

@Composable
fun PerceptionStep(onReactionComplete: (Int) -> Unit) {
    var gameState by remember { mutableStateOf("READY") } // READY, WAITING, CLICK, DONE
    var startTime by remember { mutableLongStateOf(0L) }
    var reactionTime by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("PERCEPTION_TEST", color = Color(0xFF00FF9C), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (gameState) {
                        "READY" -> Color.Gray.copy(alpha = 0.2f)
                        "WAITING" -> Color.Red.copy(alpha = 0.5f)
                        "CLICK" -> Color(0xFF00FF9C).copy(alpha = 0.8f)
                        else -> Color.Black
                    }
                )
                .clickable {
                    when (gameState) {
                        "READY" -> {
                            gameState = "WAITING"
                            // Set random delay
                        }
                        "WAITING" -> {
                            // Too early
                        }
                        "CLICK" -> {
                            reactionTime = (System.currentTimeMillis() - startTime).toInt()
                            gameState = "DONE"
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (gameState) {
                    "READY" -> "TAP TO START"
                    "WAITING" -> "WAIT FOR GREEN"
                    "CLICK" -> "TAP!"
                    else -> "${reactionTime}ms"
                },
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }

        if (gameState == "WAITING") {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay((1000..4000).random().toLong())
                gameState = "CLICK"
                startTime = System.currentTimeMillis()
            }
        }
        
        if (gameState == "DONE") {
            Spacer(modifier = Modifier.height(32.dp))
            CyberActionButton("NEXT_PROTOCOL", Color(0xFF00FF9C), onClick = { onReactionComplete(reactionTime) })
        }
    }
}

@Composable
fun IntelligenceStep(onComplete: (Int) -> Unit) {
    var score by remember { mutableIntStateOf(0) }
    var questionIndex by remember { mutableIntStateOf(0) }
    
    val questions = listOf(
        "Complete the pattern: 2, 4, 8, 16, ..." to listOf("24", "32", "64"),
        "A is to B as 1 is to ..." to listOf("2", "0", "A"),
        "Memory: Remember 7492. What was the 3rd digit?" to listOf("4", "9", "2")
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("INTELLIGENCE_MATRIX", color = Color(0xFF009CFF), style = MaterialTheme.typography.headlineSmall)
        
        if (questionIndex < questions.size) {
            val q = questions[questionIndex]
            CyberFrame(label = "QUESTION_${questionIndex + 1}", borderColor = Color(0xFF009CFF)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(q.first, color = Color.White)
                    q.second.forEachIndexed { idx, opt ->
                        CyberActionButton(opt, Color.White.copy(alpha = 0.6f)) {
                            if (idx == 1 && questionIndex == 0) score += 3 // 32
                            if (idx == 0 && questionIndex == 1) score += 3 // 2
                            if (idx == 1 && questionIndex == 2) score += 4 // 9
                            questionIndex++
                        }
                    }
                }
            }
        } else {
            CyberActionButton("COMPLETE_CALCULATION", Color(0xFF009CFF)) { onComplete(score) }
        }
    }
}

@Composable
fun CharismaStep(onComplete: (Int) -> Unit) {
    // Simplified scenario
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("CHARISMA_INTERFACE", color = Color(0xFFBC00FF), style = MaterialTheme.typography.headlineSmall)
        CyberFrame(label = "SCENARIO_ALPHA", borderColor = Color(0xFFBC00FF)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("A guard blocks your path to the mainframe. What do you do?", color = Color.White)
                CyberActionButton("BRIBE WITH 500 EDDIES", Color.White.copy(alpha = 0.6f)) { onComplete(8) }
                CyberActionButton("FAST-TALK YOUR WAY IN", Color.White.copy(alpha = 0.6f)) { onComplete(9) }
                CyberActionButton("SILENT TAKEDOWN", Color.White.copy(alpha = 0.6f)) { onComplete(4) }
            }
        }
    }
}

@Composable
fun LuckStep(onComplete: (Int) -> Unit) {
    var heads by remember { mutableIntStateOf(0) }
    var flips by remember { mutableIntStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("LUCK_FACTOR", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))
        
        if (flips < 10) {
            CyberActionButton("FLIP_COIN ($flips/10)", Color.White) {
                if (Random.nextBoolean()) heads++
                flips++
            }
        } else {
            Text("HEADS: $heads / 10", color = Color(0xFF00FF9C), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(32.dp))
            CyberActionButton("REVEAL_BUILD", Color(0xFF00FF9C)) { onComplete(heads) }
        }
    }
}

@Composable
fun ResultsStep(
    viewModel: AttributeScanViewModel,
    result: CalculatedScores?,
    onSave: () -> Unit
) {
    if (result == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF00FF9C))
                Spacer(modifier = Modifier.height(16.dp))
                Text("ANALYZING_BIOMETRIC_DATA...", color = Color(0xFF00FF9C), fontFamily = FontFamily.Monospace)
            }
        }
        return
    }
    
    val suggestedId by viewModel.suggestedTemplateId.collectAsState()
    val selectedId by viewModel.selectedTemplateId.collectAsState()
    val templates = viewModel.templates
    
    val sortedTemplates = remember(templates, suggestedId) {
        val suggested = templates.find { it.id == suggestedId }
        if (suggested != null) {
            listOf(suggested) + (templates - suggested)
        } else {
            templates
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        GlitchText("PROFILE_ACTIVATED", fontSize = 28.sp)
        
        RadarChart(
            scores = listOf(
                result.strength.toFloat(),
                result.endurance.toFloat(),
                result.agility.toFloat(),
                result.perception.toFloat(),
                result.intelligence.toFloat(),
                result.charisma.toFloat(),
                result.luck.toFloat()
            ),
            labels = listOf("STR", "END", "AGI", "PER", "INT", "CHA", "LCK"),
            modifier = Modifier.size(250.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AttributeResultRow("STRENGTH", result.strength, Color(0xFFFF006E))
            AttributeResultRow("ENDURANCE", result.endurance, Color(0xFF00FFFF))
            AttributeResultRow("AGILITY", result.agility, Color(0xFFFFFF00))
            AttributeResultRow("PERCEPTION", result.perception, Color(0xFF00FF9C))
            AttributeResultRow("INTELLIGENCE", result.intelligence, Color(0xFF009CFF))
            AttributeResultRow("CHARISMA", result.charisma, Color(0xFFBC00FF))
            AttributeResultRow("LUCK", result.luck, Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        CyberFrame(label = "ARCHETYPE_SELECTION", borderColor = Color(0xFF00FF9C)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Based on your biometric scan, the system suggests a specialized training path. Choose your archetype to finalize the link.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
                
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sortedTemplates) { template ->
                        TemplateCard(
                            template = template,
                            isSelected = selectedId == template.id,
                            isSuggested = suggestedId == template.id,
                            onClick = { viewModel.selectTemplate(template.id) }
                        )
                    }
                }
                
                selectedId?.let { id ->
                    val template = templates.find { it.id == id }
                    template?.let {
                        Text(
                            text = "DESC: ${it.description}",
                            color = Color(0xFF00FF9C),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.background(Color(0xFF00FF9C).copy(alpha = 0.05f)).padding(8.dp)
                        )
                    }
                }
            }
        }
        
        CyberActionButton("FINALIZE_AND_UPLOAD", Color(0xFF00FF9C), onClick = onSave)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AttributeResultRow(label: String, score: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = color, modifier = Modifier.width(100.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Box(modifier = Modifier.weight(1f).height(12.dp).background(Color.White.copy(alpha = 0.05f))) {
            Box(modifier = Modifier.fillMaxWidth(score / 10f).fillMaxHeight().background(color).neonBorder(color, width = 1.dp, cornerRadius = 0.dp))
        }
        Text(score.toString(), color = Color.White, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun AttributeInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                cursorColor = Color(0xFF00FF9C)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun RadarChart(
    scores: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * 0.8f
        val numAttributes = scores.size
        val angleStep = 2 * PI / numAttributes

        // Background circles
        for (i in 1..5) {
            val r = radius * (i / 5f)
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = r,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
        }

        // Axes and Labels
        for (i in 0 until numAttributes) {
            val angle = i * angleStep - PI / 2
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 1f
            )
            
            // Draw label
            val labelRadius = radius + 20.dp.toPx()
            // In a real implementation, you'd use drawText or a Box with nativeCanvas
            // For simplicity, we just leave it for now or use a basic approach.
        }

        // Shape
        val path = Path()
        for (i in 0 until numAttributes) {
            val angle = i * angleStep - PI / 2
            val scoreRatio = (scores[i] / 10f).coerceIn(0.1f, 1f)
            val x = centerX + radius * scoreRatio * cos(angle).toFloat()
            val y = centerY + radius * scoreRatio * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            color = Color(0xFF00FF9C).copy(alpha = 0.3f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        drawPath(
            path = path,
            color = Color(0xFF00FF9C),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
