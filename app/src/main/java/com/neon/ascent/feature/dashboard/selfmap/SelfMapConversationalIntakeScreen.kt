package com.neon.ascent.feature.dashboard.selfmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
            // --- TOP HEADER & ALWAYS-VISIBLE SKIP BUTTON ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SELF MAP // CONVERSATIONAL INTAKE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = cyan,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = "PHASE ${currentStep + 1} OF $totalSteps",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // ALWAYS-VISIBLE SKIP BUTTON
                OutlinedButton(
                    onClick = {
                        viewModel.skipConversationalIntake()
                        onSkipToManual()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text(
                        text = "Skip conversation — add manually",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            // Step Progress Indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1) / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = cyan,
                trackColor = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- MAIN CHAT & PROPOSALS SCROLLING CONTENT ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // GUIDE PROMPT CARD
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F141C), RoundedCornerShape(6.dp))
                            .border(1.dp, cyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🤖 NEON GUIDE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = cyan,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                if (uiState.isGeneratingIntakeResponse) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = cyan,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = uiState.intakeChatMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }

                // AI PROPOSED ITEMS CARDS
                if (uiState.pendingProposals.isNotEmpty()) {
                    item {
                        Text(
                            text = "PROPOSED ATTACHMENT NODES [ACCEPT, EDIT, OR REJECT]",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.pendingProposals, key = { it.id }) { proposal ->
                        ProposalCard(
                            proposal = proposal,
                            onAccept = { name, cat, imp, note ->
                                viewModel.acceptProposal(proposal.id, name, cat, imp, note)
                            },
                            onReject = {
                                viewModel.rejectProposal(proposal.id)
                            }
                        )
                    }
                }

                if (uiState.acceptedProposals.isNotEmpty()) {
                    item {
                        Text(
                            text = "ACCEPTED ATTACHMENTS [PLACED ON MAP: ${uiState.acceptedProposals.size}]",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF00FF9F),
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    items(uiState.acceptedProposals, key = { "accepted_${it.id}" }) { accepted ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1F14)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF00FF9F).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = accepted.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                    Text(
                                        text = "${accepted.category.displayName} | Importance: ${(accepted.suggestedImportance * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF00FF9F),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Accepted",
                                    tint = Color(0xFF00FF9F)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- OPERATOR CHAT TURN INPUT BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userTextInput,
                    onValueChange = { userTextInput = it },
                    placeholder = {
                        Text(
                            "Type your response to Guide...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
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

            Spacer(modifier = Modifier.height(12.dp))

            // --- BOTTOM NAVIGATION ACTION BAR ---
            Button(
                onClick = {
                    if (currentStep + 1 >= totalSteps) {
                        viewModel.finishConversationalIntake()
                        onFinishIntake()
                    } else {
                        viewModel.nextIntakeStep()
                    }
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
                Text(
                    text = if (currentStep + 1 >= totalSteps) "FINISH & LAND ON RADIAL CANVAS" else "NEXT PHASE [${currentStep + 2}/$totalSteps] ->",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
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
    onReject: () -> Unit
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
