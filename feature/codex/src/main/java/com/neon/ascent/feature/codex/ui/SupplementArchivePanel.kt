package com.neon.ascent.feature.codex.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import com.neon.ascent.core.domain.labs.models.Supplement
import com.neon.ascent.core.domain.labs.models.SupplementOverride

@Composable
fun SupplementArchivePanel(
    uiState: CodexUiState,
    isCyber: Boolean,
    onFilterSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit = {},
    onSupplementSelected: (Supplement) -> Unit,
    onToggleActiveStack: (String) -> Unit,
    onUpdateOverride: (String, SupplementOverride?) -> Unit,
    onDismissDetail: () -> Unit
) {
    val cyan = if (isCyber) Color(0xFF00F5FF) else MaterialTheme.colorScheme.primary
    val magenta = if (isCyber) Color(0xFFFF0088) else MaterialTheme.colorScheme.secondary
    val amber = Color(0xFFFFB300)

    val activeStackIds = remember(uiState.activeSupplementStack) {
        uiState.activeSupplementStack.map { it.id }.toSet()
    }

    var searchQuery by remember(uiState.supplementSearchQuery) {
        mutableStateOf(uiState.supplementSearchQuery)
    }

    var isSearchExpanded by remember {
        mutableStateOf(uiState.supplementSearchQuery.isNotEmpty())
    }

    val filteredSupplements = remember(
        uiState.allSupplements,
        uiState.selectedSupplementOutcomeDomainFilter,
        searchQuery
    ) {
        uiState.allSupplements.filter { supplement ->
            val matchesDomain = if (uiState.selectedSupplementOutcomeDomainFilter == "ALL") {
                true
            } else {
                supplement.primaryOutcomeDomains.contains(uiState.selectedSupplementOutcomeDomainFilter)
            }

            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                supplement.name.lowercase().contains(q) ||
                    supplement.aliases.any { it.lowercase().contains(q) } ||
                    supplement.mechanismTags.any { it.lowercase().contains(q) } ||
                    supplement.primaryOutcomeDomains.any { it.lowercase().contains(q) } ||
                    supplement.status.lowercase().contains(q)
            }

            matchesDomain && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        // Active Supplement Stack Summary Banner
        ActiveStackSummaryCard(
            activeStack = uiState.activeSupplementStack,
            overrides = uiState.supplementOverrides,
            isCyber = isCyber,
            cyan = cyan,
            magenta = magenta,
            amber = amber
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Collapsible Search Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isSearchExpanded = !isSearchExpanded }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isCyber) cyan else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isCyber) "SEARCH // TYPE_AHEAD_INDEX" else "Search Supplements",
                    color = if (isCyber) cyan else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
                if (!isSearchExpanded && searchQuery.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = cyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "\"$searchQuery\"",
                            fontSize = 10.sp,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            color = cyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isSearchExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isSearchExpanded) "Collapse search" else "Expand search",
                    tint = if (isCyber) cyan else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expanded Search Field & Type-Ahead Suggestions
        AnimatedVisibility(visible = isSearchExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        onSearchQueryChanged(query)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = {
                        Text(
                            text = if (isCyber) "TYPE TO SEARCH (NAME, ALIAS, MECHANISM)..." else "Search by name, alias, mechanism...",
                            fontSize = 12.sp,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isCyber) cyan else MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    onSearchQueryChanged("")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = if (isCyber) cyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 13.sp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cyan,
                        unfocusedBorderColor = if (isCyber) cyan.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline,
                        focusedContainerColor = if (isCyber) Color(0xFF0D1117) else MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = if (isCyber) Color(0xFF0D1117) else MaterialTheme.colorScheme.surface
                    )
                )

                // Type-Ahead Quick Selection Suggestions
                if (searchQuery.isNotBlank() && filteredSupplements.isNotEmpty()) {
                    Text(
                        text = if (isCyber) "TYPE_AHEAD SUGGESTIONS [${filteredSupplements.size}]" else "Quick Select (${filteredSupplements.size} found)",
                        color = magenta,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredSupplements.take(8).forEach { supplement ->
                            AssistChip(
                                onClick = { onSupplementSelected(supplement) },
                                label = {
                                    Text(
                                        text = supplement.name,
                                        fontSize = 11.sp,
                                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = cyan
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = cyan.copy(alpha = 0.15f),
                                    labelColor = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = cyan.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
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
                val isSelected = uiState.selectedSupplementOutcomeDomainFilter == filter
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

        if (filteredSupplements.isEmpty()) {
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
                    text = if (isCyber) "NO SUPPLEMENTS MATCH FILTER" else "No supplements match selected filter",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredSupplements.forEach { supplement ->
                    val isInStack = activeStackIds.contains(supplement.id)
                    val override = uiState.supplementOverrides[supplement.id]

                    SupplementCard(
                        supplement = supplement,
                        isInStack = isInStack,
                        override = override,
                        isCyber = isCyber,
                        cyan = cyan,
                        magenta = magenta,
                        amber = amber,
                        onClick = { onSupplementSelected(supplement) },
                        onToggleStack = { onToggleActiveStack(supplement.id) }
                    )
                }
            }
        }
    }

    // Detail Dialog when supplement selected
    val selectedSupplement = uiState.selectedSupplementDetail
    if (selectedSupplement != null) {
        val isInStack = activeStackIds.contains(selectedSupplement.id)
        val override = uiState.supplementOverrides[selectedSupplement.id]

        SupplementDetailDialog(
            supplement = selectedSupplement,
            isInStack = isInStack,
            override = override,
            isCyber = isCyber,
            cyan = cyan,
            magenta = magenta,
            amber = amber,
            onToggleStack = { onToggleActiveStack(selectedSupplement.id) },
            onUpdateOverride = { newOverride -> onUpdateOverride(selectedSupplement.id, newOverride) },
            onDismiss = onDismissDetail
        )
    }
}

@Composable
private fun SupplementCard(
    supplement: Supplement,
    isInStack: Boolean,
    override: SupplementOverride?,
    isCyber: Boolean,
    cyan: Color,
    magenta: Color,
    amber: Color,
    onClick: () -> Unit,
    onToggleStack: () -> Unit
) {
    val isPaused = override?.isPaused == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isCyber && isInStack) Modifier.border(1.dp, cyan, RoundedCornerShape(8.dp)) else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCyber) Color(0xFF0D1117) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = supplement.name,
                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )

                    if (supplement.aliases.isNotEmpty()) {
                        Text(
                            text = supplement.aliases.joinToString(", "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Toggle Stack Button
                IconButton(
                    onClick = onToggleStack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isInStack) Icons.Default.Check else Icons.Default.PlayArrow,
                        contentDescription = if (isInStack) "Remove from stack" else "Add to stack",
                        tint = if (isInStack) cyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Badges row: Status | Evidence Grade | Active Stack Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                val (statusBg, statusText) = when {
                    supplement.status.equals("Prescription", ignoreCase = true) -> magenta.copy(alpha = 0.2f) to magenta
                    supplement.isResearchChemical || supplement.status.equals("Research", ignoreCase = true) -> amber.copy(alpha = 0.2f) to amber
                    else -> cyan.copy(alpha = 0.15f) to cyan
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusBg
                ) {
                    Text(
                        text = supplement.status.uppercase(),
                        color = statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Evidence Grade Pill
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "GRADE ${supplement.evidenceGrade}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isInStack) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPaused) amber.copy(alpha = 0.2f) else cyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isPaused) "PAUSED" else "ACTIVE IN STACK",
                            color = if (isPaused) amber else cyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Typical Dose summary
            Text(
                text = "Dose: ${override?.customDoseNote ?: supplement.typicalDose}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SupplementDetailDialog(
    supplement: Supplement,
    isInStack: Boolean,
    override: SupplementOverride?,
    isCyber: Boolean,
    cyan: Color,
    magenta: Color,
    amber: Color,
    onToggleStack: () -> Unit,
    onUpdateOverride: (SupplementOverride?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var customDose by remember(override) { mutableStateOf(override?.customDoseNote ?: "") }
    var scheduleNote by remember(override) { mutableStateOf(override?.scheduleNote ?: "") }
    var isPaused by remember(override) { mutableStateOf(override?.isPaused == true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCyber) Color(0xFF0D1117) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .then(
                    if (isCyber) Modifier.border(1.dp, cyan, RoundedCornerShape(12.dp)) else Modifier
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = supplement.name,
                        color = if (isCyber) cyan else MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (supplement.aliases.isNotEmpty()) {
                    Text(
                        text = "Aliases: ${supplement.aliases.joinToString(", ")}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Stack Toggle Button
                Button(
                    onClick = onToggleStack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInStack) cyan.copy(alpha = 0.2f) else cyan,
                        contentColor = if (isInStack) cyan else Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isInStack) Icons.Default.Check else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInStack) "ACTIVE IN STACK (TAP TO REMOVE)" else "+ ADD TO ACTIVE STACK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Classification & Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when {
                        supplement.status.equals("Prescription", ignoreCase = true) -> "PRESCRIPTION (Rx)"
                        supplement.isResearchChemical -> "RESEARCH CHEMICAL"
                        else -> supplement.status.uppercase()
                    }
                    val statusColor = when {
                        supplement.status.equals("Prescription", ignoreCase = true) -> magenta
                        supplement.isResearchChemical -> amber
                        else -> cyan
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "EVIDENCE: GRADE ${supplement.evidenceGrade}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Outcomes & Mechanisms
                DetailSectionTitle("PRIMARY OUTCOME DOMAINS", isCyber, cyan)
                supplement.primaryOutcomeDomains.forEach { domain ->
                    Text(
                        text = "• $domain",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                DetailSectionTitle("MECHANISM TAGS", isCyber, cyan)
                supplement.mechanismTags.forEach { tag ->
                    Text(
                        text = "• $tag",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dose, Onset & Best Contexts
                DetailSectionTitle("TYPICAL DOSE & ONSET", isCyber, cyan)
                Text(
                    text = "Dose: ${supplement.typicalDose}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
                Text(
                    text = "Onset: ${supplement.onset}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                if (supplement.bestContexts.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailSectionTitle("BEST EVIDENCE CONTEXTS", isCyber, cyan)
                    Text(
                        text = supplement.bestContexts,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Safety & Notes
                DetailSectionTitle("SAFETY & NOTES", isCyber, cyan)
                Text(
                    text = supplement.safetyNotes,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )

                // Stacking & Interaction Notes
                if (supplement.stackingNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailSectionTitle("STACKING & INTERACTION NOTES", isCyber, cyan)
                    supplement.stackingNotes.forEach { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCyber) cyan else MaterialTheme.colorScheme.onSurface,
                                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val tagColor = if (note.evidenceGradeTag.contains("Caution", ignoreCase = true)) amber else cyan
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = tagColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = note.evidenceGradeTag.uppercase(),
                                            color = tagColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
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

                // Key PubMed links
                if (supplement.keyLinks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailSectionTitle("PUBMED RESEARCH LINKS", isCyber, cyan)
                    supplement.keyLinks.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = cyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = link,
                                fontSize = 11.sp,
                                color = cyan,
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }

                // Custom Override Editor Section (if in stack)
                if (isInStack) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = cyan.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailSectionTitle("USER OVERRIDES (STACK CUSTOMIZATION)", isCyber, cyan)

                    OutlinedTextField(
                        value = customDose,
                        onValueChange = { customDose = it },
                        label = { Text("Custom Dose Note (e.g., 100 mg AM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = scheduleNote,
                        onValueChange = { scheduleNote = it },
                        label = { Text("Schedule Note (e.g., Mon-Fri Morning)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pause Supplement in Stack",
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
                            val newOverride = if (customDose.isBlank() && scheduleNote.isBlank() && !isPaused) {
                                null
                            } else {
                                SupplementOverride(
                                    customDoseNote = customDose.ifBlank { null },
                                    scheduleNote = scheduleNote.ifBlank { null },
                                    isPaused = isPaused
                                )
                            }
                            onUpdateOverride(newOverride)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE OVERRIDES", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionTitle(title: String, isCyber: Boolean, cyan: Color) {
    Text(
        text = title,
        color = if (isCyber) cyan else MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun ActiveStackSummaryCard(
    activeStack: List<Supplement>,
    overrides: Map<String, SupplementOverride>,
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
        border = BorderStroke(1.dp, if (activeStack.isNotEmpty()) cyan else Color.Gray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCyber) "ACTIVE_SUPPLEMENT_STACK [${activeStack.size}]" else "Active Supplement Stack (${activeStack.size})",
                    color = cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
                Surface(
                    color = cyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, cyan)
                ) {
                    Text(
                        text = "LIVE IN LABS",
                        color = cyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (activeStack.isNotEmpty()) {
                activeStack.forEach { supplement ->
                    val override = overrides[supplement.id]
                    val isPaused = override?.isPaused == true
                    val doseText = override?.customDoseNote ?: supplement.typicalDose

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "• ${supplement.name}",
                                    color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (supplement.status.equals("Prescription", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("[Rx]", color = magenta, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                } else if (supplement.isResearchChemical) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("[RESEARCH]", color = amber, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                if (isPaused) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("[PAUSED]", color = amber, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        if (doseText.isNotBlank()) {
                            Text(
                                text = doseText,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = if (isCyber) "NO_SUPPLEMENTS_IN_ACTIVE_STACK // ADD_FROM_CATALOG" else "No supplements in active stack.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Live daily stack checklist and synergy analysis evaluated in LABS.",
                color = Color.Gray.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
            )
        }
    }
}
