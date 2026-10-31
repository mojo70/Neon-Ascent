package com.neon.ascent.feature.codex.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.neon.ascent.core.domain.labs.models.BiohackingProtocol
import com.neon.ascent.core.domain.labs.models.BiohackingProtocolOverride
import com.neon.ascent.core.domain.labs.models.FocusMatchLevel
import com.neon.ascent.core.domain.labs.models.matchLevel
import com.neon.ascent.core.domain.labs.models.rankItems

@Composable
fun ProtocolArchivePanel(
    uiState: CodexUiState,
    isCyber: Boolean,
    onFilterSelected: (String) -> Unit,
    onProtocolSelected: (BiohackingProtocol) -> Unit,
    onToggleActiveProtocol: (String) -> Unit,
    onUpdateOverride: (String, BiohackingProtocolOverride?) -> Unit,
    onDismissDetail: () -> Unit
) {
    val cyan = if (isCyber) Color(0xFF00F5FF) else MaterialTheme.colorScheme.primary
    val magenta = if (isCyber) Color(0xFFFF0088) else MaterialTheme.colorScheme.secondary
    val amber = Color(0xFFFFB300)

    val activeProtocolIds = remember(uiState.activeProtocols) {
        uiState.activeProtocols.map { it.id }.toSet()
    }

    val filteredProtocols = remember(uiState.allProtocols, uiState.selectedProtocolOutcomeDomainFilter, uiState.labsFocus) {
        val ranked = uiState.labsFocus.rankItems(uiState.allProtocols) { it.primaryOutcomeDomains }
        if (uiState.selectedProtocolOutcomeDomainFilter == "ALL") {
            ranked
        } else {
            ranked.filter { protocol ->
                protocol.primaryOutcomeDomains.contains(uiState.selectedProtocolOutcomeDomainFilter)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        // Active Biohacking Set Summary Banner
        ActiveProtocolsSummaryCard(
            activeProtocols = uiState.activeProtocols,
            protocolOverrides = uiState.protocolOverrides,
            isCyber = isCyber,
            cyan = cyan,
            magenta = magenta,
            amber = amber
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Outcome Domain Filter Bar Header
        Text(
            text = if (isCyber) "FILTER // OUTCOME_DOMAIN" else "Filter by Outcome Domain",
            color = if (isCyber) cyan else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OUTCOME_DOMAIN_FILTERS.forEach { filter ->
                val isSelected = uiState.selectedProtocolOutcomeDomainFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cyan.copy(alpha = 0.25f),
                        selectedLabelColor = cyan,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredProtocols.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCyber) "NO PROTOCOLS FOUND FOR FILTER" else "No protocols found for selected domain",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredProtocols.forEach { protocol ->
                    val isActive = activeProtocolIds.contains(protocol.id)
                    val override = uiState.protocolOverrides[protocol.id]

                    ProtocolCard(
                        protocol = protocol,
                        isActive = isActive,
                        override = override,
                        labsFocus = uiState.labsFocus,
                        isCyber = isCyber,
                        cyan = cyan,
                        magenta = magenta,
                        amber = amber,
                        onClick = { onProtocolSelected(protocol) },
                        onToggleActive = { onToggleActiveProtocol(protocol.id) }
                    )
                }
            }
        }
    }

    // Selected Protocol Detail Modal
    if (uiState.selectedProtocolDetail != null) {
        val selected = uiState.selectedProtocolDetail
        val isActive = activeProtocolIds.contains(selected.id)
        val currentOverride = uiState.protocolOverrides[selected.id]

        ProtocolDetailDialog(
            protocol = selected,
            isActive = isActive,
            override = currentOverride,
            isCyber = isCyber,
            cyan = cyan,
            magenta = magenta,
            amber = amber,
            onDismiss = onDismissDetail,
            onToggleActive = { onToggleActiveProtocol(selected.id) },
            onUpdateOverride = { override -> onUpdateOverride(selected.id, override) }
        )
    }
}

@Composable
private fun ProtocolCard(
    protocol: BiohackingProtocol,
    isActive: Boolean,
    override: BiohackingProtocolOverride?,
    labsFocus: com.neon.ascent.core.domain.labs.models.LabsFocus = com.neon.ascent.core.domain.labs.models.LabsFocus(),
    isCyber: Boolean,
    cyan: Color,
    magenta: Color,
    amber: Color,
    onClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    val match = labsFocus.matchLevel(protocol.primaryOutcomeDomains)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isCyber) {
                    Modifier.border(
                        width = 1.dp,
                        color = if (isActive) cyan else cyan.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCyber) Color(0xFF10141D) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isCyber) protocol.name.uppercase() else protocol.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (match == FocusMatchLevel.PRIMARY || match == FocusMatchLevel.SUPPORTING) {
                        val badgeText = if (match == FocusMatchLevel.PRIMARY) "PRIMARY" else "SUPPORTING"
                        val badgeColor = if (match == FocusMatchLevel.PRIMARY) magenta else cyan
                        Surface(
                            color = badgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val gradeColor = when (protocol.evidenceGrade.take(1)) {
                    "A" -> cyan
                    "B" -> Color(0xFF00E676)
                    "C" -> amber
                    else -> Color.Gray
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = gradeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, gradeColor)
                ) {
                    Text(
                        text = "GRADE ${protocol.evidenceGrade.take(1)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = protocol.protocolCategory,
                    fontSize = 12.sp,
                    color = magenta,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                if (protocol.status == "Research / Investigational") {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = amber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "INVESTIGATIONAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = amber,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Dosage/Parameters: ${protocol.typicalParameters}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isActive) "ACTIVE PROTOCOL" else "NOT IN ACTIVE SET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) cyan else Color.Gray,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                    if (override?.isPaused == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(PAUSED)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = amber
                        )
                    }
                }

                Button(
                    onClick = onToggleActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) cyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isActive) cyan else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isActive) "ACTIVE" else "+ ACTIVATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtocolDetailDialog(
    protocol: BiohackingProtocol,
    isActive: Boolean,
    override: BiohackingProtocolOverride?,
    isCyber: Boolean,
    cyan: Color,
    magenta: Color,
    amber: Color,
    onDismiss: () -> Unit,
    onToggleActive: () -> Unit,
    onUpdateOverride: (BiohackingProtocolOverride?) -> Unit
) {
    val context = LocalContext.current
    var editingFrequency by remember(override) { mutableStateOf(override?.preferredFrequency ?: "") }
    var editingDuration by remember(override) { mutableStateOf(override?.durationNote ?: "") }
    var isPaused by remember(override) { mutableStateOf(override?.isPaused ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCyber) Color(0xFF0D111A) else MaterialTheme.colorScheme.surface,
            border = if (isCyber) BorderStroke(1.dp, cyan) else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCyber) protocol.name.uppercase() else protocol.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${protocol.protocolCategory} • Grade ${protocol.evidenceGrade}",
                    fontSize = 12.sp,
                    color = magenta,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onToggleActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) cyan.copy(alpha = 0.3f) else cyan,
                        contentColor = if (isActive) cyan else Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isActive) "✔ ACTIVE PROTOCOL (CLICK TO REMOVE)" else "+ ADD TO ACTIVE PROTOCOLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = cyan.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                DetailSectionTitle("PRIMARY OUTCOME DOMAINS", isCyber, cyan)
                Text(
                    text = protocol.primaryOutcomeDomains.joinToString(", "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailSectionTitle("MECHANISMS OF ACTION", isCyber, cyan)
                Text(
                    text = protocol.mechanismTags.joinToString(", "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailSectionTitle("TYPICAL PARAMETERS & FREQUENCY", isCyber, cyan)
                Text(
                    text = protocol.typicalParameters,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailSectionTitle("BEST EVIDENCE CONTEXTS", isCyber, cyan)
                Text(
                    text = protocol.bestContexts,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailSectionTitle("ONSET & TIMELINE", isCyber, cyan)
                Text(
                    text = protocol.onset,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailSectionTitle("SAFETY & PRECAUTIONS", isCyber, cyan)
                Text(
                    text = protocol.safetyNotes,
                    fontSize = 12.sp,
                    color = if (protocol.safetyNotes.contains("caution", ignoreCase = true)) amber else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                if (protocol.keyLinks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailSectionTitle("EVIDENCE & RESEARCH LINKS", isCyber, cyan)
                    protocol.keyLinks.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = link,
                                fontSize = 11.sp,
                                color = cyan,
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Link",
                                tint = cyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (protocol.stackingNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailSectionTitle("STACKING & SYNERGY NOTES", isCyber, magenta)
                    protocol.stackingNotes.forEach { note ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCyber) Color(0xFF141923) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isCyber) BorderStroke(1.dp, magenta.copy(alpha = 0.5f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                                    )
                                    Text(
                                        text = note.evidenceGradeTag,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (note.evidenceGradeTag == "Caution") amber else magenta,
                                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = note.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                                )
                            }
                        }
                    }
                }

                if (isActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = cyan.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailSectionTitle("USER OVERRIDES & NOTES", isCyber, magenta)

                    OutlinedTextField(
                        value = editingFrequency,
                        onValueChange = { editingFrequency = it },
                        label = { Text("Preferred Frequency (e.g., 4x/week)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = editingDuration,
                        onValueChange = { editingDuration = it },
                        label = { Text("Duration / Session Note (e.g., 20 min)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pause Tracking",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isPaused,
                            onCheckedChange = { isPaused = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val newOverride = BiohackingProtocolOverride(
                                preferredFrequency = editingFrequency.ifBlank { null },
                                durationNote = editingDuration.ifBlank { null },
                                isPaused = isPaused
                            )
                            onUpdateOverride(newOverride)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = magenta),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE OVERRIDES")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CLOSE", color = cyan)
                }
            }
        }
    }
}

@Composable
private fun DetailSectionTitle(
    title: String,
    isCyber: Boolean,
    color: Color
) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
fun ActiveProtocolsSummaryCard(
    activeProtocols: List<BiohackingProtocol>,
    protocolOverrides: Map<String, BiohackingProtocolOverride>,
    isCyber: Boolean,
    cyan: Color,
    magenta: Color,
    amber: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = if (isCyber) Color(0xFF0F121C) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (activeProtocols.isNotEmpty()) cyan else Color.Gray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCyber) "ACTIVE_BIOHACKING_SET" else "Active Biohacking Set",
                    color = cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
                Surface(
                    color = magenta.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, magenta)
                ) {
                    Text(
                        text = "NON-WORKOUT PROTOCOLS",
                        color = magenta,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (activeProtocols.isNotEmpty()) {
                activeProtocols.forEach { protocol ->
                    val override = protocolOverrides[protocol.id]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "• ${protocol.name}",
                                color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (override?.isPaused == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[PAUSED]",
                                    color = amber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        override?.preferredFrequency?.let { freq ->
                            if (freq.isNotBlank()) {
                                Text(
                                    text = freq,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = if (isCyber) "NO_ACTIVE_BIOHACKING_PROTOCOLS // ACTIVATE_BELOW" else "No active biohacking protocols.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Workout lifting protocols (CyberCrapp, Linear, Wave) remain in Physical Ops.",
                color = Color.Gray.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
            )
        }
    }
}
