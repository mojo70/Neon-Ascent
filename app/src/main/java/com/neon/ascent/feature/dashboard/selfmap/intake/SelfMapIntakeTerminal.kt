package com.neon.ascent.feature.dashboard.selfmap.intake

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.common.LocalVisualMode
import com.neon.ascent.feature.dashboard.selfmap.ChatTurnMessage
import com.neon.ascent.feature.dashboard.selfmap.MessageSender
import com.neon.ascent.feature.dashboard.selfmap.ProposedItemCandidate

@Composable
fun SelfMapIntakeTerminal(
    uiState: IntakeUiState,
    onSendMessage: (String) -> Unit,
    onSwitchToForm: (ProposedItemCandidate?) -> Unit,
    onAccept: (ProposedItemCandidate) -> Unit,
    onReject: (ProposedItemCandidate) -> Unit,
    onAcceptAndEdit: (ProposedItemCandidate) -> Unit,
    onMoreLikeThis: (ProposedItemCandidate) -> Unit,
    onSkipCategory: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val visualMode = LocalVisualMode.current
    val primaryColor = if (visualMode == VisualMode.STEVE) theme.ink else if (visualMode == VisualMode.PILOT) theme.accent else Color.Green
    val secondaryColor = if (visualMode == VisualMode.STEVE) theme.ink else if (visualMode == VisualMode.PILOT) theme.accent else Color.Cyan
    val accentColor = theme.accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.canvas)
            .padding(16.dp)
    ) {
        TerminalHeader(
            stepIndex = uiState.currentStepIndex,
            totalSteps = 5,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            onSwitchToForm = onSwitchToForm,
            onSkipCategory = onSkipCategory
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.messages) { message ->
                ChatBubble(
                    message = message, 
                    uiState = uiState, 
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    onAccept = onAccept, 
                    onReject = onReject, 
                    onAcceptAndEdit = onAcceptAndEdit, 
                    onMoreLikeThis = onMoreLikeThis
                )
            }
        }

        TerminalInputBar(onSendMessage = onSendMessage, primaryColor = primaryColor)
    }
}

@Composable
private fun TerminalHeader(
    stepIndex: Int,
    totalSteps: Int,
    primaryColor: Color,
    secondaryColor: Color,
    onSwitchToForm: (ProposedItemCandidate?) -> Unit,
    onSkipCategory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SELF MAP // TERMINAL",
                color = primaryColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "PHASE ${stepIndex + 1} / $totalSteps",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Row {
            TextButton(onClick = onSkipCategory) {
                Text(
                    text = "[ Skip Phase ]",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = { onSwitchToForm(null) }) {
                Text(
                    text = "[ Switch to Form ]",
                    color = secondaryColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatTurnMessage,
    uiState: IntakeUiState,
    primaryColor: Color,
    secondaryColor: Color,
    onAccept: (ProposedItemCandidate) -> Unit,
    onReject: (ProposedItemCandidate) -> Unit,
    onAcceptAndEdit: (ProposedItemCandidate) -> Unit,
    onMoreLikeThis: (ProposedItemCandidate) -> Unit
) {
    val isGuide = message.sender == MessageSender.GUIDE

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isGuide) Alignment.Start else Alignment.End
    ) {
        Text(
            text = if (isGuide) "GUIDE //" else "OPERATOR //",
            color = if (isGuide) primaryColor else secondaryColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = message.text,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isGuide && message.proposals.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                message.proposals.forEach { proposal ->
                    if (!uiState.acceptedIds.contains(proposal.id) && !uiState.rejectedIds.contains(proposal.name)) {
                        ProposalCard(
                            proposal = proposal,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            onAccept = { onAccept(proposal) },
                            onReject = { onReject(proposal) },
                            onAcceptAndEdit = { onAcceptAndEdit(proposal) },
                            onMoreLikeThis = { onMoreLikeThis(proposal) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: ProposedItemCandidate,
    primaryColor: Color,
    secondaryColor: Color,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onAcceptAndEdit: () -> Unit,
    onMoreLikeThis: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .border(1.dp, primaryColor, RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = proposal.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            Text(
                text = "${(proposal.suggestedImportance * 100).toInt()}%",
                color = secondaryColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Text(
            text = proposal.category.name,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (!proposal.note.isNullOrBlank()) {
            Text(
                text = "Note: ${proposal.note}",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (!proposal.rationale.isNullOrBlank()) {
            Text(
                text = "Rationale: ${proposal.rationale}",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onAccept,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Green)
            ) {
                Text("[ Accept ]", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            
            OutlinedButton(
                onClick = onAcceptAndEdit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Yellow)
            ) {
                Text("[ Edit ]", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("[ Reject ]", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        OutlinedButton(
            onClick = onMoreLikeThis,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Cyan)
        ) {
            Text("[ More Like This ]", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TerminalInputBar(
    onSendMessage: (String) -> Unit,
    primaryColor: Color
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
            placeholder = { Text("> Reply to Guide...", color = Color.Gray, fontFamily = FontFamily.Monospace) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (text.isNotBlank()) {
                    onSendMessage(text)
                    text = ""
                }
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.DarkGray,
                cursorColor = primaryColor
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSendMessage(text)
                    text = ""
                }
            },
            enabled = text.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) primaryColor else Color.DarkGray
            )
        }
    }
}
