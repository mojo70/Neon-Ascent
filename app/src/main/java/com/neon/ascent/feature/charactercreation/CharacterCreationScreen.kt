package com.neon.ascent.feature.charactercreation

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.CyberButtonShape
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.core.common.CyberGridBackground
import com.neon.ascent.ui.CyberTabButton
import com.neon.ascent.core.common.GlitchOverlay
import com.neon.ascent.ui.theme.NeonAscentTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MorphologyIcon(value: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(100.dp)) {
        val strokeWidth = 2.dp.toPx()
        val color = Color(0xFF00FF9C)
        val center = Offset(size.width / 2, size.height / 2)
        
        // Body scaling factors based on ecto (0) -> meso (5) -> endo (10)
        val shoulderWidth = when {
            value <= 5f -> 40f + (value * 8f)
            else -> 80f + ((value - 5f) * 4f)
        }
        
        val waistWidth = when {
            value <= 5f -> 30f + (value * 2f)
            else -> 40f + ((value - 5f) * 12f)
        }

        val bodyPath = Path().apply {
            moveTo(center.x - 10f, 10f)
            lineTo(center.x + 10f, 10f)
            lineTo(center.x + 15f, 25f)
            lineTo(center.x, 35f)
            lineTo(center.x - 15f, 25f)
            close()

            moveTo(center.x - shoulderWidth/2, 40f)
            lineTo(center.x + shoulderWidth/2, 40f)
            lineTo(center.x + waistWidth/2, 90f)
            lineTo(center.x - waistWidth/2, 90f)
            close()
        }

        drawPath(bodyPath, color.copy(alpha = 0.3f))
        drawPath(bodyPath, color, style = Stroke(width = strokeWidth))
        
        drawLine(color, Offset(center.x, 40f), Offset(center.x, 90f), strokeWidth = 1f)
        drawLine(color, Offset(center.x - shoulderWidth/4, 50f), Offset(center.x - waistWidth/4, 80f), strokeWidth = 1f)
        drawLine(color, Offset(center.x + shoulderWidth/4, 50f), Offset(center.x + waistWidth/4, 80f), strokeWidth = 1f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationScreen(
    viewModel: CreationViewModel = hiltViewModel(),
    onAbort: () -> Unit,
    onCreationFinished: (String, String, String, String, String, Float, String?, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var dobValue by remember { mutableStateOf(TextFieldValue("")) }
    var units by remember { mutableStateOf("Imperial") }
    
    var heightFeet by remember { mutableStateOf("") }
    var heightInches by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    
    var weight by remember { mutableStateOf("") }
    var somatotype by remember { mutableStateOf(5f) }

    val userCharacter by viewModel.userCharacter.collectAsState()

    LaunchedEffect(userCharacter) {
        userCharacter?.let { char ->
            if (name.isEmpty()) name = char.name
            if (sex.isEmpty()) sex = char.sex
            if (dobValue.text.isEmpty()) dobValue = TextFieldValue(char.dob)
            // Units and others if they were already set might be better to keep, 
            // but for initial load from DB:
            if (weight.isEmpty()) weight = char.weight
            if (heightFeet.isEmpty()) heightFeet = char.heightFeet ?: ""
            if (heightInches.isEmpty()) heightInches = char.heightInches ?: ""
            if (heightCm.isEmpty()) heightCm = char.heightCm ?: ""
            // somatotype is usually not "empty" so maybe check if it's default 5f
            if (somatotype == 5f && char.somatotype != 0f) somatotype = char.somatotype
        }
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        CyberGridBackground()
        GlitchOverlay()
        
        // Scanline overlay effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val scanlineColor = Color.Black.copy(alpha = 0.15f)
                    for (i in 0 until size.height.toInt() step 3) {
                        drawLine(
                            color = scanlineColor,
                            start = Offset(0f, i.toFloat()),
                            end = Offset(size.width, i.toFloat()),
                            strokeWidth = 1.5f
                        )
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Abort Button at top right-ish
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    onClick = { 
                        viewModel.updateBasicInfo(name, sex, dobValue.text, units, weight, somatotype, heightFeet, heightInches, heightCm)
                        onAbort()
                    },
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
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // Header Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = ">>> NEURAL UPLINK INITIATED <<<",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFFF006E),
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "IDENTITY FORGE",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color(0xFF00FF9C),
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        letterSpacing = 8.sp,
                        shadow = Shadow(
                            color = Color(0xFF00FF9C).copy(alpha = 0.7f),
                            offset = Offset(0f, 0f),
                            blurRadius = 25f
                        )
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f).padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.height(2.dp).weight(1f).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF00FF9C)))))
                    Box(Modifier.size(8.dp).border(1.dp, Color(0xFF00FF9C)).padding(2.dp).background(Color(0xFFFF006E)))
                    Box(Modifier.height(2.dp).weight(1f).background(Brush.horizontalGradient(listOf(Color(0xFF00FF9C), Color.Transparent))))
                }
            }

            // Input Groups
            CyberFrame(label = "SUBJECT DESIGNATION [STR_ID]") {
                CyberInputTransparent(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "LOAD ALIAS_SEQUENCE..."
                )
            }

            CyberFrame(label = "BIOLOGICAL PROFILE [BIO_CAT]") {
                Row(
                    Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberTabButton(selected = sex == "Male", onClick = { sex = "Male" }, label = "XY_SEQUENCE", modifier = Modifier.weight(1f))
                    CyberTabButton(selected = sex == "Female", onClick = { sex = "Female" }, label = "XX_SEQUENCE", modifier = Modifier.weight(1f))
                }
            }

            CyberFrame(label = "TEMPORAL MARK [ORIGIN]") {
                TextField(
                    value = dobValue,
                    onValueChange = { newValue ->
                        val digits = newValue.text.filter { it.isDigit() }
                        if (digits.length <= 8) {
                            val formatted = StringBuilder()
                            for (i in digits.indices) {
                                formatted.append(digits[i])
                                if ((i == 3 || i == 5) && i != digits.lastIndex) {
                                    formatted.append(".")
                                }
                            }
                            val newText = formatted.toString()
                            
                            dobValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(newText.length)
                            )
                        }
                    },
                    placeholder = { Text("YYYY.MM.DD", color = Color.White.copy(alpha = 0.15f)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF080808),
                        unfocusedContainerColor = Color(0xFF050505),
                        focusedIndicatorColor = Color(0xFFFF006E),
                        unfocusedIndicatorColor = Color(0xFF00FF9C).copy(alpha = 0.3f),
                        cursorColor = Color(0xFFFF006E),
                        focusedTextColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 1.sp)
                )
            }

            CyberFrame(label = "SYSTEM CONFIG [UNITS]") {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CyberTabButton(selected = units == "Imperial", onClick = { units = "Imperial" }, label = "IMPERIAL", modifier = Modifier.weight(1f))
                        CyberTabButton(selected = units == "Metric", onClick = { units = "Metric" }, label = "METRIC", modifier = Modifier.weight(1f))
                    }
                    
                    if (units == "Imperial") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CyberInputTransparent(
                                value = heightFeet, 
                                onValueChange = { if (it.length <= 1) heightFeet = it }, 
                                placeholder = "FT", 
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            CyberInputTransparent(
                                value = heightInches, 
                                onValueChange = { if (it.length <= 2) heightInches = it }, 
                                placeholder = "IN", 
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    } else {
                        CyberInputTransparent(
                            value = heightCm, 
                            onValueChange = { heightCm = it }, 
                            placeholder = "CM",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            CyberFrame(label = "MASS INDEX [${if (units == "Imperial") "LBS" else "KG"}]") {
                CyberInputTransparent(
                    value = weight, 
                    onValueChange = { weight = it }, 
                    placeholder = "VAL_INPUT",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            CyberFrame(label = "MORPHOLOGY [SOMATOTYPE]") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    MorphologyIcon(value = somatotype)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ECTO", color = Color(0xFF00FF9C).copy(alpha = if(somatotype < 3) 1f else 0.4f), style = MaterialTheme.typography.labelSmall)
                        Text("MESO", color = Color(0xFF00FF9C).copy(alpha = if(somatotype in 3f..7f) 1f else 0.4f), style = MaterialTheme.typography.labelSmall)
                        Text("ENDO", color = Color(0xFF00FF9C).copy(alpha = if(somatotype > 7) 1f else 0.4f), style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Slider(
                        value = somatotype,
                        onValueChange = { somatotype = it },
                        valueRange = 0f..10f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF006E),
                            activeTrackColor = Color(0xFF00FF9C),
                            inactiveTrackColor = Color(0xFF151515)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Complex Submit Button
            Button(
                onClick = { 
                    onCreationFinished(name, sex, dobValue.text, units, weight, somatotype, heightFeet, heightInches, heightCm)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(CyberButtonShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF00FF9C), Color(0xFF00A364))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CyberButtonShape),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text(
                    "INITIALIZE CORE LINK",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                )
            }

            Text(
                "VERIFIED // ENCRYPTION ENABLED",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF00FF9C).copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun CyberInputTransparent(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.15f)) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF080808),
            unfocusedContainerColor = Color(0xFF050505),
            focusedIndicatorColor = Color(0xFFFF006E),
            unfocusedIndicatorColor = Color(0xFF00FF9C).copy(alpha = 0.3f),
            cursorColor = Color(0xFFFF006E),
            focusedTextColor = Color.White
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 1.sp)
    )
}


