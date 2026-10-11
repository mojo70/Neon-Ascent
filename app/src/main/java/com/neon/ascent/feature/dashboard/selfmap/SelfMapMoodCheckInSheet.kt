package com.neon.ascent.feature.dashboard.selfmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfMapMoodCheckInSheet(
    viewModel: SelfMapViewModel,
    eoeId: Long? = null,
    onDismiss: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent

    var moodScore by remember { mutableFloatStateOf(7.0f) }
    var noteInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10141A),
        scrimColor = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "SELF MAP // MOOD CHECK-IN",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = cyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Record your baseline mood / felt capacity right now.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Score Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your reported mood:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "${moodScore.toInt()}/10",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = cyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = moodScore,
                onValueChange = { moodScore = it },
                valueRange = 1.0f..10.0f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = cyan,
                    activeTrackColor = cyan,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1 [HEAVY / LOW]", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp))
                Text("5 [STEADY]", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp))
                Text("10 [HIGH CAPACITY]", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = { Text("Optional reflection note", fontFamily = FontFamily.Monospace) },
                placeholder = { Text("Notice any attachment shift or reaction?", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    viewModel.logMood(score = moodScore.toInt(), note = noteInput.ifBlank { null }, eoeId = eoeId)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = cyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOG MOOD CHECK-IN",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
