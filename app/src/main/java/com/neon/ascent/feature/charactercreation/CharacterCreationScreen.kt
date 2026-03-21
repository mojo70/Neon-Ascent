package com.neon.ascent.feature.charactercreation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.ui.theme.NeonAscentTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

// Custom shape for cyberpunk aesthetic
val CyberButtonShape = GenericShape { size, _ ->
    moveTo(0f, 12f)
    lineTo(12f, 0f)
    lineTo(size.width - 24f, 0f)
    lineTo(size.width, 24f)
    lineTo(size.width, size.height - 12f)
    lineTo(size.width - 12f, size.height)
    lineTo(24f, size.height)
    lineTo(0f, size.height - 24f)
    close()
}

@Composable
fun GlitchOverlay() {
    var glitchTrigger by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(Random.nextLong(2000, 5000))
            glitchTrigger++
            delay(Random.nextLong(50, 150))
            glitchTrigger++
        }
    }

    if (glitchTrigger % 2 != 0) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = Random.nextInt(3, 8)
            repeat(count) {
                val y = Random.nextFloat() * size.height
                val height = Random.nextFloat() * 20f + 2f
                val width = size.width * (Random.nextFloat() * 0.5f + 0.2f)
                val x = if (Random.nextBoolean()) 0f else size.width - width
                
                val color = when(Random.nextInt(3)) {
                    0 -> Color(0xFF00FF9C).copy(alpha = 0.4f)
                    1 -> Color(0xFFFF006E).copy(alpha = 0.4f)
                    else -> Color.White.copy(alpha = 0.3f)
                }
                
                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(width, height)
                )
            }
        }
    }
}

@Composable
fun CyberGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSpacing = 30.dp.toPx()
        val color = Color(0xFF00FF9C).copy(alpha = 0.08f)
        
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += gridSpacing
        }
        
        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += gridSpacing
        }
    }
}

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
    onInitialize: (String, String, String, String, String, Float, String?, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("Imperial") }
    
    var heightFeet by remember { mutableStateOf("") }
    var heightInches by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    
    var weight by remember { mutableStateOf("") }
    var somatotype by remember { mutableStateOf(5f) }

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
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

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
                CyberInputTransparent(
                    value = dob,
                    onValueChange = { dob = it },
                    placeholder = "YYYY.MM.DD"
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
                            CyberInputTransparent(value = heightFeet, onValueChange = { if (it.length <= 1) heightFeet = it }, placeholder = "FT", modifier = Modifier.weight(1f))
                            CyberInputTransparent(value = heightInches, onValueChange = { if (it.length <= 2) heightInches = it }, placeholder = "IN", modifier = Modifier.weight(1f))
                        }
                    } else {
                        CyberInputTransparent(value = heightCm, onValueChange = { heightCm = it }, placeholder = "CM")
                    }
                }
            }

            CyberFrame(label = "MASS INDEX [${if (units == "Imperial") "LBS" else "KG"}]") {
                CyberInputTransparent(value = weight, onValueChange = { weight = it }, placeholder = "VAL_INPUT")
            }

            CyberFrame(label = "MORPHOLOGY [SOMATOTYPE]") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                    onInitialize(name, sex, dob, units, weight, somatotype, heightFeet, heightInches, heightCm) 
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
fun CyberFrame(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp, 16.dp).background(Color(0xFFFF006E)))
            Text(
                text = label,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF00FF9C),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00FF9C).copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = RectangleShape
                )
                .background(Color(0xFF00FF9C).copy(alpha = 0.02f))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun CyberInputTransparent(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.15f)) },
        modifier = modifier.fillMaxWidth(),
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

@Composable
fun CyberTabButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(CyberButtonShape)
            .background(if (selected) Color(0xFFFF006E) else Color(0xFF0A0A0A))
            .border(
                width = 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.4f) else Color(0xFF00FF9C).copy(alpha = 0.2f),
                shape = CyberButtonShape
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF00FF9C).copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        )
    }
}
