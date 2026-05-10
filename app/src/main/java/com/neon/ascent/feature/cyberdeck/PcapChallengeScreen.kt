package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.model.Challenge
import com.neon.ascent.model.Packet
import kotlinx.coroutines.delay

@Composable
fun PcapChallengeScreen(
    challenge: Challenge,
    onFlagSubmitted: (String) -> Unit,
    onFilterApplied: (String) -> Unit,   // You handle filtering logic in ViewModel
    onTimeout: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(challenge.timeLimitSeconds) }
    var bufferLeft by remember { mutableStateOf(challenge.bufferSize) }
    var currentFilter by remember { mutableStateOf("") }
    var filteredPackets by remember { mutableStateOf(challenge.packets) }
    var showSuccess by remember { mutableStateOf(false) }
    var flagInput by remember { mutableStateOf("") }

    // Timer
    LaunchedEffect(timeLeft) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
            if (timeLeft == 0) onTimeout()
        }
    }

    val glitchAlpha by animateFloatAsState(
        targetValue = if (timeLeft < 30) 0.3f else 0f,
        label = "glitch"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)) // Deep cyberpunk black
            .statusBarsPadding()
            .padding(12.dp)
    ) {
        // Header - Terminal Style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = challenge.title,
                color = Color(0xFF00FF9F), // Neon green
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "BUFFER: $bufferLeft/${challenge.bufferSize}",
                    color = if (bufferLeft > 3) Color(0xFF00FF9F) else Color.Red,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TIME: ${timeLeft}s",
                    color = if (timeLeft > 60) Color(0xFF00FF9F) else Color.Red,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Description / AI Core Flavor
        Text(
            text = challenge.description,
            color = Color(0xFF00B8FF), // Neon blue
            fontSize = 14.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = currentFilter,
                onValueChange = { currentFilter = it },
                modifier = Modifier.weight(1f),
                label = { Text("WIRESHARK FILTER", color = Color(0xFF00FF9F)) },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1A1A2E),
                    unfocusedContainerColor = Color(0xFF1A1A2E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (currentFilter.isNotBlank() && bufferLeft > 0) {
                            onFilterApplied(currentFilter)
                            bufferLeft--
                            // Simulate filtering (you can move real logic to VM)
                            filteredPackets = challenge.packets.filter { packet ->
                                packet.summary.contains(currentFilter, ignoreCase = true) ||
                                packet.payload.contains(currentFilter, ignoreCase = true)
                            }
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (currentFilter.isNotBlank() && bufferLeft > 0) {
                        onFilterApplied(currentFilter)
                        bufferLeft--
                        filteredPackets = challenge.packets.filter { packet ->
                            packet.summary.contains(currentFilter, ignoreCase = true) ||
                            packet.payload.contains(currentFilter, ignoreCase = true)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9F))
            ) {
                Text("APPLY", color = Color.Black, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Packets List - Scrollable Terminal
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF111118))
                .padding(8.dp)
        ) {
            items(filteredPackets) { packet ->
                PacketRow(packet = packet)
                HorizontalDivider(color = Color(0xFF00FF9F).copy(alpha = 0.1f), thickness = 1.dp)
            }
        }

        // Flag Input
        OutlinedTextField(
            value = flagInput,
            onValueChange = { flagInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ENTER EXTRACTED FLAG", color = Color(0xFFFF00AA)) },
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = Color.White),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1A1A2E),
                unfocusedContainerColor = Color(0xFF1A1A2E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { onFlagSubmitted(flagInput) }
            )
        )

        // Learning Point (shown after win or on hint request)
        if (showSuccess) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A))
            ) {
                Text(
                    text = "LEARNING POINT:\n${challenge.metadata["learningPoint"]}",
                    color = Color(0xFF00FF9F),
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    // Glitch overlay when time is low
    if (glitchAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red.copy(alpha = glitchAlpha))
        )
    }
}

@Composable
private fun PacketRow(packet: Packet) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row {
            Text(
                text = "[${packet.timestamp % 100000}] ",
                color = Color(0xFF666677),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "${packet.srcIp} → ${packet.dstIp}  ",
                color = Color(0xFF00B8FF),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            Text(
                text = packet.protocol.name,
                color = Color(0xFFFF00AA),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
        
        Text(
            text = packet.summary,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        
        if (packet.payload.isNotEmpty()) {
            Text(
                text = packet.payload.take(120) + if (packet.payload.length > 120) "..." else "",
                color = Color(0xFFAAAAAA),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
    }
}
