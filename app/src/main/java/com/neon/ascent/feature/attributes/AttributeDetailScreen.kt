package com.neon.ascent.feature.attributes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.*

@Composable
fun AttributeDetailScreen(
    attributeName: String,
    onBack: () -> Unit,
    onNavigateToDatabase: () -> Unit,
    viewModel: AttributeViewModel = hiltViewModel()
) {
    val attribute = AttributeData.attributes[attributeName.uppercase()] ?: return
    val userCharacter by viewModel.userCharacter.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    
    val systemOverride by viewModel.systemOverrideMessage.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        PerspectiveGrid()
        Scanlines(intensity = 0.1f)
        StaticNoise(intensity = 0.05f)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = attribute.accentColor)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    GlitchText(
                        text = attribute.name,
                        color = attribute.accentColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    CyberFrame(label = "OVERVIEW", borderColor = attribute.accentColor) {
                        Column {
                            Text(
                                text = attribute.description,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "REAL-WORLD_IMPACT:",
                                color = attribute.accentColor.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = attribute.lifeImportance,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                
                item {
                    Text(
                        "TRAINING_PROTOCOLS",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        attribute.quickGames.forEach { game ->
                            QuickGameCard(
                                game = game, 
                                accentColor = attribute.accentColor,
                                isGlitching = false,
                                onClick = {
                                    if (attribute.name == "LUCK" && game.name == "Crit Chance") {
                                        viewModel.onLuckButtonClick()
                                    }
                                }
                            )
                        }
                    }
                }
                
                if (userCharacter?.isSystemDatabaseUnlocked == true) {
                    item {
                        CyberActionButton(
                            label = "ACCESS_CORE_DATA",
                            color = Color(0xFF00FF9C),
                            onClick = onNavigateToDatabase
                        )
                    }
                }
                
                item {
                    CyberFrame(label = "SYSTEM_TIPS", borderColor = attribute.accentColor) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            attribute.tips.forEach { tip ->
                                Row {
                                    Text(">", color = attribute.accentColor, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Text(tip, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
                
                item {
                    Text(
                        "OPTIMIZED_TRAINING_TEMPLATES",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val userSomatotype = when {
                        (userCharacter?.somatotype ?: 5f) < 3.3f -> "Ectomorph"
                        (userCharacter?.somatotype ?: 5f) < 6.6f -> "Mesomorph"
                        else -> "Endomorph"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        templates.forEach { template ->
                            val isRecommended = template.somatotype == userSomatotype
                            
                            CyberFrame(
                                label = if (isRecommended) "RECOMMENDED_FOR_YOUR_FRAME" else "LEGACY_PROTOCOL",
                                borderColor = if (isRecommended) attribute.accentColor else Color.Gray.copy(alpha = 0.3f),
                                accentColor = if (isRecommended) Color(0xFFFF006E) else Color.Transparent
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = template.name,
                                            color = if (isRecommended) Color.White else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        
                                        Surface(
                                            color = if (isRecommended) attribute.accentColor.copy(alpha = 0.2f) else Color.Transparent,
                                            border = BorderStroke(1.dp, if (isRecommended) attribute.accentColor else Color.Gray.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = template.somatotype.uppercase(),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 9.sp,
                                                color = if (isRecommended) attribute.accentColor else Color.Gray,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(4.dp))
                                    
                                    Text(
                                        text = template.description,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    Spacer(Modifier.height(12.dp))
                                    
                                    // Target Attribute Level for this template
                                    val targetVal = when(attributeName.uppercase()) {
                                        "STRENGTH" -> template.strength
                                        "AGILITY" -> template.agility
                                        "ENDURANCE" -> template.endurance
                                        "INTELLIGENCE" -> template.intelligence
                                        "PERCEPTION" -> template.perception
                                        "CHARISMA" -> template.charisma
                                        "LUCK" -> template.luck
                                        else -> 0
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "TARGET_${attributeName.uppercase()}_LVL: ",
                                            color = attribute.accentColor.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            targetVal.toString(),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        
                                        Spacer(Modifier.weight(1f))
                                        
                                        Button(
                                            onClick = { /* In a future update, this could set a 'Goal' */ },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = attribute.accentColor.copy(alpha = 0.1f)),
                                            border = BorderStroke(1.dp, attribute.accentColor),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("LOAD_TEMPLATE", color = attribute.accentColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    AiChatSection(
                        attribute = attribute,
                        aiResponse = aiResponse,
                        isLoading = isChatLoading,
                        onSend = { viewModel.askAi(attribute, it) }
                    )
                }

                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // System Override Overlay
        if (systemOverride != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "!! SYSTEM OVERRIDE !!",
                        color = Color.Red,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(16.dp))
                    GlitchText(
                        text = systemOverride!!,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "LUCK_VARIABLE_MANIPULATED_SUCCESSFULLY...",
                        color = Color(0xFF00FF9C),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun QuickGameCard(
    game: QuickGame, 
    accentColor: Color,
    isGlitching: Boolean = false,
    onClick: () -> Unit = {}
) {
    val cardColor = if (isGlitching) Color.Magenta.copy(alpha = 0.3f) else Color(0xFF0A0A0A)
    val borderColor = if (isGlitching) Color.White else accentColor.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .clip(CyberButtonShape)
            .background(cardColor)
            .border(1.dp, borderColor, CyberButtonShape)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = game.name,
                color = if (isGlitching) Color.White else accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = game.description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f).padding(top = 4.dp)
            )
            Text(
                text = if (isGlitching) "!! LUCK_FLIP_READY !!" else game.actionLabel,
                color = if (isGlitching) Color.White else accentColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun AiChatSection(
    attribute: AttributeDetail,
    aiResponse: String,
    isLoading: Boolean,
    onSend: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    CyberFrame(label = "NEURAL_CONSULTANT: ${attribute.aiExpertName}", borderColor = attribute.accentColor) {
        Column {
            if (aiResponse.isNotEmpty()) {
                Text(
                    text = aiResponse,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(8.dp)
                )
                Spacer(Modifier.height(16.dp))
            } else {
                Text(
                    text = "Awaiting query... [Expert Personality: ${attribute.aiPersonalityDescription}]",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Consult ${attribute.aiExpertName}...", fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = attribute.accentColor,
                        unfocusedIndicatorColor = attribute.accentColor.copy(alpha = 0.5f),
                        cursorColor = attribute.accentColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSend(textInput)
                            textInput = ""
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = attribute.accentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = attribute.accentColor
                        )
                    }
                }
            }
        }
    }
}
