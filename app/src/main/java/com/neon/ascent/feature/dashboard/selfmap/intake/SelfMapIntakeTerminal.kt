package com.neon.ascent.feature.dashboard.selfmap.intake

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
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
    onSkipCategory: () -> Unit,
    onDone: () -> Unit,
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
            onSkipCategory = onSkipCategory,
            onDone = onDone,
        )

        Spacer(modifier = Modifier.height(16.dp))

        val listState = rememberLazyListState()
        LaunchedEffect(uiState.messages.size) {
            if (uiState.messages.isNotEmpty()) {
                listState.animateScrollToItem(uiState.messages.lastIndex)
            }
        }

        LazyColumn(
            state = listState,
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
    onSkipCategory: () -> Unit,
    onDone: () -> Unit,
) {
    val title = "SELF MAP // TERMINAL"
    val phase = "PHASE ${stepIndex + 1} / $totalSteps"
    val switchLabel = "[ Switch to Form ]"
    val titleStyle = TextStyle(
        color = primaryColor,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    )
    val phaseStyle = TextStyle(
        color = Color.Gray,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
    )
    val switchStyle = TextStyle(
        color = secondaryColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
    )
    val measurer = rememberTextMeasurer()

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gap = 8.dp
            val maxWidthPx = constraints.maxWidth
            val gapPx = with(LocalDensity.current) { (gap * 2).roundToPx() }
            val needed = measurer.measure(title, titleStyle).size.width +
                measurer.measure(phase, phaseStyle).size.width +
                measurer.measure(switchLabel, switchStyle).size.width +
                gapPx
            val fits = needed <= maxWidthPx
            val lineModifier = if (fits) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.horizontalScroll(rememberScrollState())
            }
            Row(
                modifier = lineModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SingleLineLabel(title, titleStyle)
                Spacer(modifier = Modifier.width(gap))
                SingleLineLabel(phase, phaseStyle)
                Spacer(modifier = if (fits) Modifier.weight(1f) else Modifier.width(gap))
                TextButton(
                    onClick = { onSwitchToForm(null) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                ) {
                    SingleLineLabel(switchLabel, switchStyle)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onSkipCategory,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
            ) {
                SingleLineLabel(
                    "[ Skip this category ]",
                    TextStyle(color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(
                onClick = onDone,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
            ) {
                SingleLineLabel(
                    "[ I'm done ]",
                    TextStyle(color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                )
            }
        }
    }
}

@Composable
private fun SingleLineLabel(text: String, style: TextStyle) {
    Text(
        text = text,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
    )
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
            color = onCanvasColor(),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isGuide && message.proposals.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                message.proposals.forEach { proposal ->
                    if (IntakeProposalResolution.isActionable(
                            proposal,
                            uiState.acceptedIds,
                            uiState.rejectedIds,
                            uiState.dismissedIds,
                        )
                    ) {
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
                color = onCanvasColor(),
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
            text = proposal.category.displayName,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (!proposal.note.isNullOrBlank()) {
            Text(
                text = "Note: ${proposal.note}",
                color = mutedOnCanvasColor(Color.LightGray),
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
            textStyle = TextStyle(color = onCanvasColor(), fontFamily = FontFamily.Monospace),
            placeholder = { Text("> Reply to Guide...", color = mutedOnCanvasColor(), fontFamily = FontFamily.Monospace) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (text.isNotBlank()) {
                    onSendMessage(text)
                    text = ""
                }
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = primaryColor.copy(alpha = 0.7f),
                cursorColor = primaryColor,
                focusedTextColor = onCanvasColor(),
                unfocusedTextColor = onCanvasColor(),
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

@Composable
private fun onCanvasColor(): Color {
    return if (LocalVisualMode.current == VisualMode.STEVE) LocalNeonTheme.current.ink else Color.White
}

@Composable
private fun mutedOnCanvasColor(onDark: Color = Color.Gray): Color {
    return if (LocalVisualMode.current == VisualMode.STEVE) LocalNeonTheme.current.inkMuted else onDark
}
