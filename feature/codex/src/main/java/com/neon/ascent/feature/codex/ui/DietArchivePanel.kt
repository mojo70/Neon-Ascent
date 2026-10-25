package com.neon.ascent.feature.codex.ui

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.neon.ascent.core.domain.labs.models.Diet

val OUTCOME_DOMAIN_FILTERS = listOf(
    "ALL",
    "Physical Performance / Recovery",
    "Longevity / Healthspan",
    "Mental Energy / Fatigue Resistance",
    "Attention / Focus",
    "Memory",
    "Stress Resilience / Mood",
    "Sleep Architecture",
    "Foundational / Nutrient Status"
)

@Composable
fun DietArchivePanel(
    uiState: CodexUiState,
    isCyber: Boolean,
    onFilterSelected: (String) -> Unit,
    onDietSelected: (Diet) -> Unit,
    onSetActiveDiet: (String?) -> Unit,
    onDismissDetail: () -> Unit
) {
    val cyan = if (isCyber) Color(0xFF00F5FF) else MaterialTheme.colorScheme.primary
    val magenta = if (isCyber) Color(0xFFFF0088) else MaterialTheme.colorScheme.secondary

    val filteredDiets = if (uiState.selectedOutcomeDomainFilter == "ALL") {
        uiState.allDiets
    } else {
        uiState.allDiets.filter { diet ->
            diet.primaryOutcomeDomains.contains(uiState.selectedOutcomeDomainFilter)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Outcome Domain Filters Header
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
                val isSelected = uiState.selectedOutcomeDomainFilter == filter
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

        if (filteredDiets.isEmpty()) {
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
                    text = if (isCyber) "NO_MATCHING_DIET_PROTOCOLS" else "No matching diet protocols found.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredDiets.forEach { diet ->
                    val isActive = diet.id == uiState.activeDiet?.id
                    DietCatalogItemCard(
                        diet = diet,
                        isActive = isActive,
                        isCyber = isCyber,
                        cyan = cyan,
                        magenta = magenta,
                        onCardClick = { onDietSelected(diet) },
                        onToggleActive = {
                            if (isActive) onSetActiveDiet(null) else onSetActiveDiet(diet.id)
                        }
                    )
                }
            }
        }
    }

    // Diet Detail Dialog
    uiState.selectedDietDetail?.let { diet ->
        DietDetailModal(
            diet = diet,
            isActive = diet.id == uiState.activeDiet?.id,
            isCyber = isCyber,
            cyan = cyan,
            magenta = magenta,
            onDismiss = onDismissDetail,
            onToggleActive = {
                if (diet.id == uiState.activeDiet?.id) {
                    onSetActiveDiet(null)
                } else {
                    onSetActiveDiet(diet.id)
                }
            }
        )
    }
}

@Composable
fun DietCatalogItemCard(
    diet: Diet,
    isActive: Boolean,
    isCyber: Boolean,
    cyan: Color,
    magenta: Color,
    onCardClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    if (isCyber) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardClick() }
                .border(
                    BorderStroke(
                        if (isActive) 1.5.dp else 1.dp,
                        if (isActive) cyan else cyan.copy(alpha = 0.4f)
                    ),
                    RoundedCornerShape(4.dp)
                ),
            color = Color(0xFF0A0C14),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = diet.name.uppercase(),
                            color = if (isActive) cyan else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (isActive) {
                            Surface(
                                color = cyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, cyan)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = cyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        color = magenta.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "GRADE ${diet.evidenceGrade.take(1)}",
                            color = magenta,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Category: ${diet.dietCategory}",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (diet.primaryOutcomeDomains.isNotEmpty()) {
                    Text(
                        text = "Outcomes: ${diet.primaryOutcomeDomains.joinToString()}",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DETAILS →",
                        color = cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = onToggleActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) Color.Red.copy(alpha = 0.8f) else cyan,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isActive) "CLEAR ACTIVE" else "SET ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = diet.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isActive) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(text = "ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    AssistChip(
                        onClick = { },
                        label = { Text("Grade ${diet.evidenceGrade.take(1)}", fontSize = 10.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = diet.dietCategory,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Text(
                    text = diet.primaryOutcomeDomains.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCardClick) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "View Specs")
                    }

                    Button(
                        onClick = onToggleActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = if (isActive) "Clear Active" else "Set Active")
                    }
                }
            }
        }
    }
}

@Composable
fun DietDetailModal(
    diet: Diet,
    isActive: Boolean,
    isCyber: Boolean,
    cyan: Color,
    magenta: Color,
    onDismiss: () -> Unit,
    onToggleActive: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCyber) Color(0xFF0D0E15) else MaterialTheme.colorScheme.surface,
            border = if (isCyber) BorderStroke(1.5.dp, cyan) else null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCyber) diet.name.uppercase() else diet.name,
                            color = if (isCyber) cyan else MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        )
                        Text(
                            text = "Status: ${diet.status} ${if (diet.isAppSpecific) "(App-Specific Protocol)" else ""}",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "CLOSE",
                            tint = if (isCyber) cyan else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Protocol Toggle
                Button(
                    onClick = onToggleActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color.Red.copy(alpha = 0.85f) else cyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Check else Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isActive) "CLEAR ACTIVE PROTOCOL" else "SET AS ACTIVE PROTOCOL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Evidence Grade
                DietDetailSection(
                    title = "EVIDENCE GRADE",
                    isCyber = isCyber,
                    color = magenta
                ) {
                    Text(
                        text = diet.evidenceGrade,
                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                // Primary Outcome Domains
                DietDetailSection(
                    title = "PRIMARY OUTCOME DOMAINS",
                    isCyber = isCyber,
                    color = cyan
                ) {
                    Text(
                        text = diet.primaryOutcomeDomains.joinToString("\n• ", prefix = "• "),
                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                // Diet Category & Mechanisms
                DietDetailSection(
                    title = "CATEGORY & MECHANISMS",
                    isCyber = isCyber,
                    color = cyan
                ) {
                    Text(
                        text = "Category: ${diet.dietCategory}\nMechanisms: ${diet.mechanismTags.joinToString()}",
                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                // Full Parameters
                DietDetailSection(
                    title = "TYPICAL PARAMETERS",
                    isCyber = isCyber,
                    color = cyan
                ) {
                    val p = diet.typicalParameters
                    val textBuilder = StringBuilder()

                    if (p.proteinGramsPerKgMin != null) {
                        textBuilder.append("• Protein Target: ${p.proteinGramsPerKgMin}–${p.proteinGramsPerKgMax} g/kg body weight daily\n")
                    }
                    if (p.baselineNetCarbsGramsMin != null) {
                        textBuilder.append("• Baseline Net Carbs: ~${p.baselineNetCarbsGramsMin}–${p.baselineNetCarbsGramsMax} g net carbs most days\n")
                    }
                    if (p.higherCarbMealsPerWeekMin != null) {
                        textBuilder.append("• Higher-Carb Refeeds: ${p.higherCarbMealsPerWeekMin}–${p.higherCarbMealsPerWeekMax} meals/week (${p.higherCarbTimingPreference ?: ""})\n")
                    }
                    if (p.eatingWindowHours != null) {
                        textBuilder.append("• Fasting / Eating Window: ${p.eatingWindowHours}h eating / ${p.fastingWindowHours ?: 16}h fast\n")
                    }
                    if (!p.freeVegetablesDescription.isNullOrBlank()) {
                        textBuilder.append("• Free Vegetable Rule: ${p.freeVegetablesDescription}\n")
                    }
                    if (!p.netCarbCalculationRule.isNullOrBlank()) {
                        textBuilder.append("• Net Carb Rule: ${p.netCarbCalculationRule}\n")
                    }
                    if (!p.rawParametersDescription.isNullOrBlank()) {
                        textBuilder.append("• Overview: ${p.rawParametersDescription}")
                    }

                    Text(
                        text = textBuilder.toString().trim(),
                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                // Onset / Timeline
                DietDetailSection(
                    title = "ONSET & TIMELINE",
                    isCyber = isCyber,
                    color = cyan
                ) {
                    Text(
                        text = diet.onset,
                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                // Safety & Context Notes
                DietDetailSection(
                    title = "SAFETY & NOTES",
                    isCyber = isCyber,
                    color = magenta
                ) {
                    Text(
                        text = diet.safetyNotes,
                        color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                // Stacking & Interaction Notes
                if (diet.stackingNotes.isNotEmpty()) {
                    DietDetailSection(
                        title = "STACKING & INTERACTION NOTES",
                        isCyber = isCyber,
                        color = cyan
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            diet.stackingNotes.forEach { note ->
                                Surface(
                                    color = if (isCyber) cyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = if (isCyber) BorderStroke(1.dp, cyan.copy(alpha = 0.4f)) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = note.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isCyber) cyan else MaterialTheme.colorScheme.onSurface,
                                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                                            )
                                            Surface(
                                                color = magenta.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = note.evidenceGradeTag,
                                                    color = magenta,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.description,
                                            fontSize = 11.sp,
                                            color = if (isCyber) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                                        )
                                        if (!note.relatedDietOrProtocol.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Related Protocol: ${note.relatedDietOrProtocol}",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Key Links
                if (diet.keyLinks.isNotEmpty()) {
                    DietDetailSection(
                        title = "KEY LINKS & LITERATURE",
                        isCyber = isCyber,
                        color = cyan
                    ) {
                        Text(
                            text = diet.keyLinks.joinToString("\n• ", prefix = "• "),
                            color = if (isCyber) cyan else MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DietDetailSection(
    title: String,
    isCyber: Boolean,
    color: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}
