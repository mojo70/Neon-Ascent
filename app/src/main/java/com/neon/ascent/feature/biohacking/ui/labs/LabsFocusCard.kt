package com.neon.ascent.feature.biohacking.ui.labs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.common.LocalVisualMode
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.domain.labs.models.LabsFocus
import com.neon.ascent.core.domain.labs.models.OutcomeDomain

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabsFocusCard(
    focus: LabsFocus,
    onUpdateFocus: (LabsFocus) -> Unit,
    neonCyan: Color,
    neonMagenta: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val visualMode = LocalVisualMode.current
    var isExpanded by remember { mutableStateOf(false) }

    val isSteve = visualMode == VisualMode.STEVE
    val cardBg = if (isSteve) theme.surface else Color(0xFF0D1117)
    val cardBorder = if (isSteve) theme.secondary.copy(alpha = 0.5f) else neonCyan.copy(alpha = 0.3f)
    val titleColor = if (isSteve) theme.ink else neonCyan
    val labelColor = if (isSteve) theme.secondary else neonMagenta

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LABS FOCUS",
                    color = titleColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isExpanded) "CLOSE" else "EDIT",
                    color = labelColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Active Summary (Tags or Empty State)
            val isEmpty = focus.primary == null && focus.supporting.isEmpty()
            if (isEmpty) {
                Text(
                    text = "Set a focus",
                    color = theme.inkMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontStyle = FontStyle.Italic
                )
            } else {
                val primaryDomain = focus.primary
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (primaryDomain != null) {
                        FocusTag(
                            label = "PRIMARY: ${primaryDomain.displayName}",
                            isPrimary = true,
                            neonCyan = neonCyan,
                            neonMagenta = neonMagenta,
                            isSteve = isSteve
                        )
                    }
                    focus.supporting.forEach { domain ->
                        FocusTag(
                            label = domain.displayName,
                            isPrimary = false,
                            neonCyan = neonCyan,
                            neonMagenta = neonMagenta,
                            isSteve = isSteve
                        )
                    }
                }
            }

            // Inline Expandable Selector Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = if (isSteve) theme.secondary.copy(alpha = 0.3f) else neonCyan.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Focus Selector
                    Text(
                        text = "PRIMARY FOCUS (SELECT 1)",
                        color = if (isSteve) theme.ink else neonMagenta,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Clear primary option
                        FilterChip(
                            selected = focus.primary == null,
                            onClick = {
                                val updated = LabsFocus.createSanitized(null, focus.supporting)
                                onUpdateFocus(updated)
                            },
                            label = {
                                Text(
                                    text = "None",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isSteve) theme.ink else neonMagenta.copy(alpha = 0.25f),
                                selectedLabelColor = if (isSteve) theme.surface else neonMagenta
                            )
                        )

                        OutcomeDomain.entries.forEach { domain ->
                            val isSelected = focus.primary == domain
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newPrimary = if (isSelected) null else domain
                                    val updated = LabsFocus.createSanitized(newPrimary, focus.supporting)
                                    onUpdateFocus(updated)
                                },
                                label = {
                                    Text(
                                        text = domain.displayName,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isSteve) theme.ink else neonMagenta.copy(alpha = 0.25f),
                                    selectedLabelColor = if (isSteve) theme.surface else neonMagenta
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Supporting Focus Selector
                    Text(
                        text = "SUPPORTING FOCUS (0–N)",
                        color = if (isSteve) theme.ink else neonCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutcomeDomain.entries.forEach { domain ->
                            val isPrimary = focus.primary == domain
                            val isSelected = domain in focus.supporting

                            FilterChip(
                                selected = isSelected,
                                enabled = !isPrimary,
                                onClick = {
                                    val newSupporting = if (isSelected) {
                                        focus.supporting - domain
                                    } else {
                                        focus.supporting + domain
                                    }
                                    val updated = LabsFocus.createSanitized(focus.primary, newSupporting)
                                    onUpdateFocus(updated)
                                },
                                label = {
                                    Text(
                                        text = if (isPrimary) "${domain.displayName} (Primary)" else domain.displayName,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isSteve) theme.secondary else neonCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = if (isSteve) theme.surface else neonCyan,
                                    disabledContainerColor = if (isSteve) theme.surface else Color.Transparent,
                                    disabledLabelColor = theme.inkMuted
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isExpanded = false }) {
                            Text(
                                text = "DONE",
                                color = if (isSteve) theme.ink else neonCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusTag(
    label: String,
    isPrimary: Boolean,
    neonCyan: Color,
    neonMagenta: Color,
    isSteve: Boolean
) {
    val theme = LocalNeonTheme.current
    val tagBg = when {
        isSteve && isPrimary -> theme.ink
        isSteve -> theme.surface
        isPrimary -> neonMagenta.copy(alpha = 0.2f)
        else -> neonCyan.copy(alpha = 0.15f)
    }
    val tagText = when {
        isSteve && isPrimary -> theme.surface
        isSteve -> theme.ink
        isPrimary -> neonMagenta
        else -> neonCyan
    }
    val tagBorder = when {
        isSteve -> theme.secondary.copy(alpha = 0.4f)
        isPrimary -> neonMagenta
        else -> neonCyan.copy(alpha = 0.4f)
    }

    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = tagBg),
        border = BorderStroke(1.dp, tagBorder)
    ) {
        Text(
            text = label,
            color = tagText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
