package com.neon.ascent.feature.sanctuary

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.sanctuary.models.SanctuaryPassage
import com.neon.ascent.ui.CyberCutShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanctuarySheet(
    targetPassageId: String? = null,
    onDismissRequest: () -> Unit,
    viewModel: SanctuaryViewModel = hiltViewModel()
) {
    LaunchedEffect(targetPassageId) {
        if (!targetPassageId.isNullOrBlank()) {
            viewModel.highlightPassage(targetPassageId)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.75f)
    ) {
        SanctuaryScreen(
            viewModel = viewModel,
            onClose = onDismissRequest
        )
    }
}

@Composable
fun SanctuaryScreen(
    viewModel: SanctuaryViewModel = hiltViewModel(),
    onClose: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalNeonTheme.current
    val visualMode = LocalVisualMode.current
    val isCyber = visualMode == VisualMode.CYBER

    val primaryAccent = if (isCyber) Color(0xFF00FF9C) else theme.ink
    val secondaryAccent = if (isCyber) Color(0xFFFF0088) else theme.secondary
    val cardBg = if (isCyber) Color(0xFF0F141A) else theme.overlay
    val surfaceBg = if (isCyber) Color(0xFF080B0E) else theme.canvas
    val textPrimary = if (isCyber) Color.White else theme.ink
    val textSecondary = if (isCyber) Color(0xFFA0B0C0) else theme.ink.copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceBg)
    ) {
        if (isCyber) {
            CyberGridBackground(color = primaryAccent.copy(alpha = 0.03f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isCyber) "SANCTUARY // SCRIPTURE_SEED" else "Sanctuary",
                        fontSize = if (isCyber) 20.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Serif
                    )
                    Text(
                        text = if (isCyber) "DEFENSE // WARFARE // HEALING // AUTHORITY" else "Protection & Authority Scripture Seed",
                        fontSize = 11.sp,
                        color = textSecondary,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }

                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Default Deck vs All Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.isDefaultDeckOnly,
                    onClick = { viewModel.toggleDefaultDeckOnly() },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (uiState.isDefaultDeckOnly) primaryAccent else textSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCyber) "DEFAULT_DECK [3]" else "First-Open Deck",
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryAccent.copy(alpha = 0.2f),
                        selectedLabelColor = primaryAccent,
                        containerColor = cardBg,
                        labelColor = textSecondary
                    )
                )

                if (uiState.selectedTheme != null || uiState.selectedTag != null || uiState.searchQuery.isNotBlank() || uiState.isDefaultDeckOnly) {
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text(
                            text = if (isCyber) "RESET_FILTERS" else "Clear Filters",
                            fontSize = 12.sp,
                            color = secondaryAccent,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        text = if (isCyber) "SEARCH // REF_OR_KEYWORDS..." else "Search scripture, theme or reference...",
                        fontSize = 12.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = primaryAccent
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = textPrimary,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryAccent,
                    unfocusedBorderColor = primaryAccent.copy(alpha = 0.3f),
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Theme Filters Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.themes) { themeName ->
                    val isSelected = uiState.selectedTheme.equals(themeName, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectTheme(themeName) },
                        label = {
                            Text(
                                text = if (isCyber) "#$themeName" else themeName,
                                fontSize = 11.sp,
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryAccent.copy(alpha = 0.25f),
                            selectedLabelColor = primaryAccent,
                            containerColor = cardBg,
                            labelColor = textSecondary
                        )
                    )
                }
            }

            // Tag Filters Row
            if (uiState.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.tags) { tag ->
                        val isSelected = uiState.selectedTag.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectTag(tag) },
                            label = {
                                Text(
                                    text = if (isCyber) "TAG:$tag" else tag,
                                    fontSize = 10.sp,
                                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = secondaryAccent.copy(alpha = 0.25f),
                                selectedLabelColor = secondaryAccent,
                                containerColor = cardBg.copy(alpha = 0.7f),
                                labelColor = textSecondary.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Passages List
            if (uiState.passages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCyber) "NO_SIGNAL // NO PASSAGES MATCH CRITERIA" else "No scripture passages match the active filters.",
                        color = textSecondary,
                        fontSize = 13.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.passages, key = { it.id }) { passage ->
                        val isHighlighted = uiState.highlightedPassageId == passage.id
                        SanctuaryPassageCard(
                            passage = passage,
                            isCyber = isCyber,
                            isHighlighted = isHighlighted,
                            primaryAccent = primaryAccent,
                            secondaryAccent = secondaryAccent,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SanctuaryPassageCard(
    passage: SanctuaryPassage,
    isCyber: Boolean,
    isHighlighted: Boolean,
    primaryAccent: Color,
    secondaryAccent: Color,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val borderColor = when {
        isHighlighted -> secondaryAccent
        passage.tags.contains("biggs-priority") -> primaryAccent
        else -> primaryAccent.copy(alpha = 0.25f)
    }

    val cardShape = if (isCyber) CyberCutShape else RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(if (isHighlighted) secondaryAccent.copy(alpha = 0.08f) else cardBg)
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = borderColor,
                shape = cardShape
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (passage.isFavoriteDefault) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Default Deck",
                            tint = primaryAccent,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = passage.reference,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = primaryAccent,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Serif
                    )
                }

                Text(
                    text = passage.translation,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
            }

            // Title if present
            val passageTitle = passage.title
            if (!passageTitle.isNullOrBlank()) {
                Text(
                    text = passageTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary.copy(alpha = 0.9f),
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
            }

            // Text Body
            Text(
                text = passage.text,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = textPrimary,
                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Serif
            )

            // Themes & Tags Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                passage.themes.take(3).forEach { theme ->
                    Surface(
                        color = primaryAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "#$theme",
                            fontSize = 9.sp,
                            color = primaryAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        )
                    }
                }

                passage.tags.filter { it.endsWith("-core") || it == "biggs-priority" }.forEach { tag ->
                    Surface(
                        color = secondaryAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            color = secondaryAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        )
                    }
                }
            }
        }
    }
}
