package com.neon.ascent.feature.goals.ui.ascension

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.goals.models.Protocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolLibraryScreen(
    onBack: () -> Unit,
    onProtocolClick: (Protocol) -> Unit,
    viewModel: ProtocolLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalNeonTheme.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PROTOCOLS // CANONICAL_INDEX", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.accent)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = theme.canvas,
                    titleContentColor = theme.accent
                )
            )
        },
        containerColor = theme.canvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("SEARCH_PROTOCOLS...", color = theme.inkMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.accent) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = theme.inkMuted.copy(alpha = 0.5f),
                    unfocusedContainerColor = theme.surface,
                    focusedContainerColor = theme.surface
                ),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categories
            val categories = listOf("All", "Biohacking", "Habit Formation", "Spiritual", "Strength", "Recovery")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    val isSelected = (category == "All" && uiState.selectedCategory == null) || (category == uiState.selectedCategory)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(if (category == "All") null else category) },
                        label = { Text(category.uppercase(), fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.accent.copy(alpha = 0.2f),
                            selectedLabelColor = theme.accent,
                            containerColor = theme.surface,
                            labelColor = theme.inkMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = theme.accent)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.protocols) { protocol ->
                        ProtocolCard(protocol = protocol, onClick = { onProtocolClick(protocol) })
                    }
                }
            }
        }
    }
}

@Composable
fun ProtocolCard(protocol: Protocol, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, theme.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = protocol.title,
                    color = theme.ink,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = theme.secondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.secondary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = protocol.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = theme.secondary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = protocol.description,
                color = theme.inkMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            
            if (protocol.canonicalSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "CANONICAL_STEPS:",
                    color = theme.accent.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                protocol.canonicalSteps.take(3).forEach { step ->
                    Text(
                        text = "• $step",
                        color = theme.ink.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SOURCE: ${protocol.source}",
                    color = theme.accent.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Button(
                    onClick = onClick,
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.canvas),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("ADAPT_FOR_ME", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
