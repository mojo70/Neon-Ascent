package com.neon.ascent.feature.biohacking

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.domain.model.DopamineCategory
import com.neon.ascent.core.domain.model.DopamineMenuItem
import com.neon.ascent.core.domain.model.EnergyLevel
import com.neon.ascent.ui.CyberActionButton
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.GlitchText

@Composable
fun DopamineMenuScreen(
    onBack: () -> Unit,
    viewModel: DopamineMenuViewModel = hiltViewModel()
) {
    val items by viewModel.menuItems.collectAsState()
    val selectedEnergy by viewModel.selectedEnergy.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val neonCyan = Color(0xFF00F5FF)
    val neonMagenta = Color(0xFFFF0088)
    val voidBg = Color(0xFF0A0F14)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(voidBg).statusBarsPadding().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = neonCyan)
                    }
                    GlitchText(
                        text = "DOPAMINE_MENU",
                        color = neonCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = { viewModel.generateNewSuggestions() }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Suggestions", tint = neonMagenta)
                    }
                }
            }
        },
        containerColor = voidBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Filters
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnergyLevel.values().forEach { energy ->
                    FilterChip(
                        selected = selectedEnergy == energy,
                        onClick = { viewModel.setEnergyFilter(if (selectedEnergy == energy) null else energy) },
                        label = { Text(energy.name, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = neonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = neonCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NO_PROTOCOLS_FOUND",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CyberActionButton("SEED_INITIAL_DATA", neonCyan) {
                            viewModel.seedDefaultItems()
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { item ->
                        DopamineItemCard(item, neonCyan, neonMagenta) {
                            viewModel.logCompletion(item)
                        }
                    }
                }
            }

            if (isGenerating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    color = neonMagenta,
                    trackColor = neonMagenta.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun DopamineItemCard(
    item: DopamineMenuItem,
    cyan: Color,
    magenta: Color,
    onComplete: () -> Unit
) {
    CyberFrame(
        label = item.category.name,
        borderColor = cyan.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                color = Color.Gray,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.durationMinutes}M",
                    color = cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Complete", tint = magenta, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
