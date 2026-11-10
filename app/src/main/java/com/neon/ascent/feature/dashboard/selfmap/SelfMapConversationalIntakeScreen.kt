package com.neon.ascent.feature.dashboard.selfmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SkipNext
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
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.core.domain.selfmap.SelfMapIntakeMode
import com.neon.ascent.ui.SoftGridBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfMapConversationalIntakeScreen(
    viewModel: SelfMapViewModel,
    onFinishIntake: () -> Unit,
    onSkipToManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent
    val danger = theme.accentDanger
    val uiState by viewModel.uiState.collectAsState()

    var userTextInput by remember { mutableStateOf("") }

    val currentStep = uiState.currentIntakeStepIndex
    val totalSteps = 5

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        SoftGridBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // --- TOP HEADER & LIGHT PHASE CRUMB ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SELF MAP // INTAKE STREAM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = cyan,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = "PHASE // ${currentStep + 1} OF $totalSteps",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // IN-SESSION MODE SWITCH BUTTON (Switch to Form)
                OutlinedButton(
                    onClick = {
                        viewModel.switchSessionIntakeMode(SelfMapIntakeMode.FORM)
                        onSkipToManual()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = "Switch to Form",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- MAIN SCROLLING MESSAGE THREAD (PRIMARY CANVAS) ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.chatTurns, key = { it.id }) { turn ->
                    if (turn.sender == MessageSender.GUIDE) {
                        // Guide message card with compact inline proposal cards inside the thread
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F141C), RoundedCornerShape(6.dp))
                                .border(1.dp, cyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🤖 NEON GUIDE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = cyan,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                if (uiState.isGeneratingIntakeResponse && turn == uiState.chatTurns.last()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = cyan,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            Text(
                                text = turn.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 20.sp
                                )
                            )

                            // Inline Proposals inside the thread
                            if (turn.proposals.isNotEmpty()) {
                                for (proposal in turn.proposals) {
                                    ProposalCard(
                                        proposal = proposal,
                                        onAccept = { name, cat, imp, note ->
                                            viewModel.acceptProposal(proposal.id, name, cat, imp, note)
                                        },
                                        onReject = {
                                            viewModel.rejectProposal(proposal.id)
                                        },
                                        onMoreLikeThis = {
                                            viewModel.requestMoreProposals(proposal.category)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Operator reply bubble inside the thread
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp)
                                .background(Color(0xFF16221D), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF00FF9F).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "OPERATOR // REPLIED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF00FF9F),
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = turn.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- TERMINAL INPUT BAR (> Reply to Guide…) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userTextInput,
                    onValueChange = { userTextInput = it },
                    placeholder = {
                        Text(
                            "> Reply to Guide…",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cyan,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (userTextInput.isNotBlank()) {
                            viewModel.sendConversationalTurn(userTextInput)
                            userTextInput = ""
                        }
                    },
                    modifier = Modifier
                        .background(cyan, RoundedCornerShape(4.dp))
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Response",
                        tint = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- COMPACT TERMINAL FOOTER (Skip / Finish without heavy buttons) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { viewModel.skipCurrentCategory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("[ SKIP CATEGORY ]", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }

                TextButton(
                    onClick = {
                        if (currentStep + 1 >= totalSteps) {
                            viewModel.finishConversationalIntake()
                            onFinishIntake()
                        } else {
                            viewModel.nextIntakeStep()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = cyan)
                ) {
                    Text(
                        text = if (currentStep + 1 >= totalSteps) "[ FINISH -> CANVAS ]" else "[ NEXT PHASE -> ]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Interactive Proposal Card allowing user to accept (with custom edits) or reject an AI-suggested SelfItem candidate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalCard(
    proposal: ProposedItemCandidate,
    onAccept: (name: String, cat: SelfMapCategory, importance: Float, note: String?) -> Unit,
    onReject: () -> Unit,
    onMoreLikeThis: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent
    val danger = theme.accentDanger

    var isEditing by remember { mutableStateOf(false) }
    var nameInput by remember(proposal) { mutableStateOf(proposal.name) }
    var catInput by remember(proposal) { mutableStateOf(proposal.category) }
    var importanceInput by remember(proposal) { mutableFloatStateOf(proposal.suggestedImportance) }
    var noteInput by remember(proposal) { mutableStateOf(proposal.note ?: "") }

    val borderColor = when {
        proposal.isAccepted -> Color(0xFF00FF9F)
        proposal.isRejected -> danger.copy(alpha = 0.5f)
        else -> cyan.copy(alpha = 0.7f)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                proposal.isAccepted -> Color(0xFF081C12)
                proposal.isRejected -> Color(0xFF1F0A0A)
                else -> Color(0xFF101620)
            }
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (!isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = proposal.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${proposal.category.displayName} | Importance: ${(proposal.suggestedImportance * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = getCategoryColor(proposal.category),
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        if (!proposal.note.isNullOrBlank()) {
                            Text(
                                text = proposal.note,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (!proposal.rationale.isNullOrBlank()) {
                            Text(
                                text = "Rationale: ${proposal.rationale}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF00FF9F).copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    if (!proposal.isAccepted && !proposal.isRejected) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Proposal",
                                tint = cyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                if (!proposal.isAccepted && !proposal.isRejected) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onAccept(nameInput, catInput, importanceInput, noteInput.ifBlank { null })
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00FF9F),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ACCEPT NODE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = onReject,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = danger),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("REJECT", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onMoreLikeThis,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = cyan),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("MORE LIKE THIS", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                } else if (proposal.isAccepted) {
                    Text(
                        text = "✓ ACCEPTED & PLACED ON MAP",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF00FF9F),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                } else if (proposal.isRejected) {
                    Text(
                        text = "✕ REJECTED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = danger,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            } else {
                // EDIT MODE
                Text(
                    text = "EDIT PROPOSAL BEFORE ACCEPTING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = cyan,
                        fontFamily = FontFamily.Monospace
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Item Name", fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Selector
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SelfMapCategory.entries) { cat ->
                        FilterChip(
                            selected = catInput == cat,
                            onClick = { catInput = cat },
                            label = { Text(cat.displayName, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getCategoryColor(cat),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "IMPORTANCE: ${(importanceInput * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                )
                Slider(
                    value = importanceInput,
                    onValueChange = { importanceInput = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = cyan, activeTrackColor = cyan)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isEditing = false }) {
                        Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = {
                            isEditing = false
                            onAccept(nameInput, catInput, importanceInput, noteInput.ifBlank { null })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9F), contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("SAVE & ACCEPT", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}


