package com.neon.ascent.feature.codex.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.Scanlines
import com.neon.ascent.core.domain.codex.models.BiomarkerKeys
import com.neon.ascent.core.domain.codex.models.BiomarkerSample
import com.neon.ascent.core.domain.codex.models.BiomarkerStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CodexScreen(
    onBack: () -> Unit,
    chronicleContent: @Composable () -> Unit,
    viewModel: CodexViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.selectedExerciseId != null) {
        ExerciseDossierPane(
            uiState = uiState,
            onBack = { viewModel.selectExercise(null) }
        )
    } else if (uiState.selectedMarkerKey != null) {
        MarkerDossierPane(
            uiState = uiState,
            onBack = { viewModel.selectMarker(null) },
            onAddSample = { value, date, notes ->
                val marker = uiState.latestBiomarkers.find { it.latest.markerKey == uiState.selectedMarkerKey }?.latest
                if (marker != null) {
                    viewModel.addBiomarkerSample(
                        marker.markerKey,
                        marker.displayName,
                        value,
                        marker.unit,
                        date,
                        notes
                    )
                }
            },
            onDeleteSample = { viewModel.deleteBiomarkerSample(it) }
        )
    } else {
        Scaffold(
            topBar = {
                var showMenu by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(48.dp)) // Spacer for center alignment

                    Text(
                        text = "NEURAL_ARCHIVE",
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp
                    )

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "OPTIONS", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("EXPORT_LOGS (.JSON)", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    viewModel.exportHistory()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Wing Switcher
                WingSwitcher(
                    selectedWing = uiState.activeWing,
                    onWingSelected = { viewModel.selectWing(it) }
                )

                Box(modifier = Modifier.weight(1f)) {
                    Scanlines(intensity = 0.05f)
                    
                    when (uiState.activeWing) {
                        CodexWing.OPS_LOG -> OpsLogWing(
                            period = uiState.selectedPeriod,
                            sessionCount = uiState.sessionCount,
                            uiState = uiState,
                            isLoading = uiState.isLoading,
                            onPeriodSelected = { viewModel.selectPeriod(it) },
                            onExerciseSelected = { viewModel.selectExercise(it) },
                            onQueryChanged = { viewModel.updateSearchQuery(it) }
                        )
                        CodexWing.VITALS -> VitalsWing(
                            uiState = uiState,
                            isLoading = uiState.isLoading,
                            onTypeSelected = { viewModel.selectVitalsType(it) },
                            onPeriodSelected = { viewModel.selectPeriod(it) }
                        )
                        CodexWing.SERUM -> SerumWing(
                            uiState = uiState,
                            isLoading = uiState.isLoading,
                            onMarkerSelected = { viewModel.selectMarker(it) },
                            onAddSample = { key, name, value, unit, date, notes ->
                                viewModel.addBiomarkerSample(key, name, value, unit, date, notes)
                            }
                        )
                        CodexWing.CHRONICLE -> chronicleContent()
                    }
                }
            }
        }
    }
}

@Composable
fun WingSwitcher(
    selectedWing: CodexWing,
    onWingSelected: (CodexWing) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .padding(2.dp)
            .semantics { contentDescription = "ARCHIVE_WING_SWITCHER" }
    ) {
        CodexWing.entries.forEach { wing ->
            val isSelected = selectedWing == wing
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(2.dp)
                    )
                    .clickable(
                        onClick = { onWingSelected(wing) },
                        onClickLabel = "SWITCH_TO_${wing.name}"
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = wing.name.replace("_", " "),
                    color = if (isSelected) MaterialTheme.colorScheme.background else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun OpsLogWing(
    period: CodexPeriod,
    sessionCount: Int,
    uiState: CodexUiState,
    isLoading: Boolean = false,
    onPeriodSelected: (CodexPeriod) -> Unit,
    onExerciseSelected: (String) -> Unit,
    onQueryChanged: (String) -> Unit
) {
    if (isLoading) {
        LoadingWing("SCANNING_OPS_LOG")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Period Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(CodexPeriod.entries) { p ->
                    val isSelected = period == p
                    Surface(
                        onClick = { onPeriodSelected(p) },
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) Color(0xFF00CCFF).copy(alpha = 0.2f) else Color.Transparent,
                        border = BorderStroke(
                            1.dp, 
                            if (isSelected) Color(0xFF00CCFF) else Color.DarkGray
                        ),
                        modifier = Modifier.semantics { contentDescription = "PERIOD_${p.label}" }
                    ) {
                        Text(
                            text = p.label,
                            color = if (isSelected) Color(0xFF00CCFF) else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            var showPicker by remember { mutableStateOf(false) }
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Search, contentDescription = "PICKER", tint = MaterialTheme.colorScheme.primary)
            }
            
            if (showPicker) {
                ExercisePicker(
                    uiState = uiState,
                    onDismiss = { showPicker = false },
                    onSelect = { 
                        onExerciseSelected(it)
                        showPicker = false
                    },
                    onQueryChanged = onQueryChanged
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Heatmap
        SectionHeader("SESSION_HEATMAP")
        SessionHeatmap(uiState.sessionSummaries, period)

        Spacer(modifier = Modifier.height(24.dp))

        // Protocol Metrics
        SectionHeader("PROTOCOL_SYNC")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProtocolMetric("HIT_RATE", "${(uiState.hitRate * 100).toInt()}%", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            ProtocolMetric("ROTATION", "${uiState.sessionSummaries.filter { it.date.isAfter(LocalDate.now().minusDays(9)) }.size}/3", Color(0xFF00CCFF), Modifier.weight(1f))
        }
        Text(
            "Target: ~2x per muscle / 8-9 days",
            color = Color.DarkGray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Muscle Frequency
        if (uiState.muscleFrequency.isNotEmpty()) {
            SectionHeader("MUSCLE_FOCUS // LAST_9D")
            MuscleFrequencyRow(uiState.muscleFrequency)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Summary Stats
        SectionHeader("SUMMARY_ANALYSIS")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBox(
                label = "STREAK",
                value = "${uiState.streak}",
                subValue = "DAYS",
                icon = Icons.Default.Whatshot,
                color = Color(0xFFFF5F1F),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "TOTAL_VOL",
                value = formatVolume(uiState.totalVolume),
                subValue = "LBS",
                icon = Icons.Default.FitnessCenter,
                color = Color(0xFF00CCFF),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Calendar Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalendarStat("WEEK", uiState.weekCount, Modifier.weight(1f))
            CalendarStat("MONTH", uiState.monthCount, Modifier.weight(1f))
            CalendarStat("YEAR", uiState.yearCount, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // PR Rail
        if (uiState.prs.isNotEmpty()) {
            SectionHeader("PERSONAL_RECORDS // ARCHIVE")
            PrRail(uiState.prs, onSelect = onExerciseSelected)
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Session History List
        if (uiState.sessionSummaries.isNotEmpty()) {
            SectionHeader("SESSION_HISTORY // ACTIVITY_LOG")
            uiState.sessionSummaries.forEach { summary ->
                SessionRow(summary)
            }
        }

        if (sessionCount == 0) {
            Spacer(modifier = Modifier.height(64.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History, 
                        contentDescription = null, 
                        tint = Color.DarkGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "0 sessions in this window.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Open OPS and the archive writes itself.",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun VitalsWing(
    uiState: CodexUiState,
    isLoading: Boolean = false,
    onTypeSelected: (VitalsType) -> Unit,
    onPeriodSelected: (CodexPeriod) -> Unit
) {
    if (isLoading) {
        LoadingWing("SYNCING_BIOMETRICS")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Period Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CodexPeriod.entries) { p ->
                val isSelected = uiState.selectedPeriod == p
                Surface(
                    onClick = { onPeriodSelected(p) },
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) Color(0xFF00CCFF).copy(alpha = 0.2f) else Color.Transparent,
                    border = BorderStroke(
                        1.dp, 
                        if (isSelected) Color(0xFF00CCFF) else Color.DarkGray
                    ),
                    modifier = Modifier.semantics { contentDescription = "PERIOD_${p.label}" }
                ) {
                    Text(
                        text = p.label,
                        color = if (isSelected) Color(0xFF00CCFF) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Type Selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(VitalsType.entries) { type ->
                val isSelected = uiState.vitalsType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelected(type) },
                    label = { 
                        Text(
                            type.label, 
                            fontSize = 10.sp, 
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        labelColor = Color.Gray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.DarkGray,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Chart
        if (uiState.vitalsData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "NO_ROLLUPS_YET",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "SYNC_HEALTH_FROM_LABS",
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            SectionHeader("${uiState.vitalsType.name}_TIMELINE")
            VitalsChart(uiState.vitalsData, uiState.sessionSummaries)
        }

        // Recovery & Volume Sparkline
        Spacer(modifier = Modifier.height(32.dp))
        SectionHeader("RECOVERY_ANALYSIS")
        RecoverySparklineRow(uiState)

        // Socratic Insight
        uiState.latestInsight?.let {
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("NEURAL_ANALYSIS")
            Surface(
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(0xFFFF006E).copy(alpha = 0.2f))
            ) {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SerumWing(
    uiState: CodexUiState,
    isLoading: Boolean = false,
    onMarkerSelected: (String) -> Unit,
    onAddSample: (String, String, Double, String, Instant, String?) -> Unit
) {
    if (isLoading) {
        LoadingWing("EXTRACTING_SERUM_DATA")
        return
    }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("BIOMARKER_ARCHIVE")
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "ADD_SAMPLE", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (uiState.latestBiomarkers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No biomarker data. Initiate entry in SERUM.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            uiState.latestBiomarkers.forEach { status ->
                MarkerRow(status, onMarkerSelected)
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showAddDialog) {
        AddBiomarkerDialog(
            onDismiss = { showAddDialog = false },
            onSave = { key, name, value, unit, date, notes ->
                onAddSample(key, name, value, unit, date, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MarkerRow(status: BiomarkerStatus, onClick: (String) -> Unit) {
    val latest = status.latest
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick(latest.markerKey) },
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(latest.displayName.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("${latest.value} ${latest.unit}", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }

            if (status.delta != null) {
                val delta = status.delta!!
                val color = if (delta > 0) MaterialTheme.colorScheme.primary else Color(0xFFFF006E)
                val sign = if (delta > 0) "+" else ""
                Text(
                    text = "$sign${"%.2f".format(delta)}",
                    color = color.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Tiny Sparkline
            Box(modifier = Modifier.size(60.dp, 24.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val history = status.history.reversed()
                    if (history.size >= 2) {
                        val max = history.maxOf { it.value }.toFloat()
                        val min = history.minOf { it.value }.toFloat()
                        val range = (max - min).coerceAtLeast(1f)
                        val path = Path()
                        history.forEachIndexed { i, sample ->
                            val x = i * (size.width / (history.size - 1))
                            val y = size.height - ((sample.value.toFloat() - min) / range * size.height)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFF00CCFF).copy(alpha = 0.4f), style = Stroke(1.dp.toPx()))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerDossierPane(
    uiState: CodexUiState,
    onBack: () -> Unit,
    onAddSample: (Double, Instant, String?) -> Unit,
    onDeleteSample: (String) -> Unit
) {
    val history = uiState.selectedMarkerHistory
    val marker = uiState.latestBiomarkers.find { it.latest.markerKey == uiState.selectedMarkerKey }?.latest
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(marker?.displayName?.uppercase() ?: "BIOMARKER", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "BACK")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "ADD", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (history.size >= 2) {
                SectionHeader("HISTORICAL_TRACE")
                MarkerChart(history)
            }

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader("SAMPLE_HISTORY")
            history.forEach { sample ->
                SampleRow(sample, formatter, onDeleteSample)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showAddDialog && marker != null) {
        AddSampleQuickDialog(
            markerName = marker.displayName,
            unit = marker.unit,
            onDismiss = { showAddDialog = false },
            onSave = { value, date, notes ->
                onAddSample(value, date, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MarkerChart(history: List<BiomarkerSample>) {
    val data = history.reversed()
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp))
            .padding(16.dp)
    ) {
        val width = size.width
        val height = size.height
        val maxVal = data.maxOf { it.value }.toFloat()
        val minVal = data.minOf { it.value }.toFloat()
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val path = Path()
        data.forEachIndexed { index, sample ->
            val x = index * (width / (data.size - 1).coerceAtLeast(1))
            val y = height - ((sample.value.toFloat() - minVal) / range * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFF00CCFF), style = Stroke(width = 2.dp.toPx()))

        data.forEachIndexed { index, sample ->
            val x = index * (width / (data.size - 1).coerceAtLeast(1))
            val y = height - ((sample.value.toFloat() - minVal) / range * height)
            drawCircle(primaryColor, 3.dp.toPx(), Offset(x, y))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SampleRow(sample: BiomarkerSample, formatter: DateTimeFormatter, onDelete: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                .padding(12.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(sample.drawnAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter), color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("${sample.value} ${sample.unit}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            sample.notes?.let { notes ->
                Text(notes, color = Color.DarkGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("DELETE_ENTRY", color = Color.Red) },
                onClick = {
                    onDelete(sample.id)
                    showMenu = false
                }
            )
        }
    }
}

@Composable
fun AddBiomarkerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, Instant, String?) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val seedData = BiomarkerKeys.SEED_DATA

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("ADD_BIOMARKER_DRAW", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))

                // Suggestions
                Text("SUGGESTIONS", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                LazyRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(seedData.toList()) { (k, n) ->
                        Surface(
                            onClick = {
                                key = k
                                name = n
                            },
                            shape = RoundedCornerShape(2.dp),
                            color = if (key == k) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (key == k) MaterialTheme.colorScheme.primary else Color.DarkGray)
                        ) {
                            Text(n, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(6.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("MARKER_NAME") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("VALUE") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("UNIT") },
                        modifier = Modifier.weight(0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("NOTES") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val dVal = value.toDoubleOrNull() ?: 0.0
                        onSave(key.ifBlank { name.lowercase().replace(" ", "_") }, name, dVal, unit, Instant.now(), notes.ifBlank { null })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
                ) {
                    Text("UPLOAD_BIO_DATA", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun AddSampleQuickDialog(
    markerName: String,
    unit: String,
    onDismiss: () -> Unit,
    onSave: (Double, Instant, String?) -> Unit
) {
    var value by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ADD_SAMPLE: $markerName", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("VALUE ($unit)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("NOTES") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val dVal = value.toDoubleOrNull() ?: 0.0
                        onSave(dVal, Instant.now(), notes.ifBlank { null })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
                ) {
                    Text("SAVE_SAMPLE", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun SessionRow(summary: SessionSummary) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    summary.date.format(formatter).uppercase(),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val protocolName = summary.protocol?.displayName ?: "OPS / FREE"
                    Text(
                        protocolName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (summary.dayType != null) {
                        Text(
                            " · ${summary.dayType.name}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                
                if (summary.primaryAugmentName != null) {
                    Text(
                        "SUB-PROTOCOL: ${summary.primaryAugmentName}",
                        color = Color(0xFF00CCFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${formatVolume(summary.volume)}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "VOLUME (LBS)",
                    color = Color.DarkGray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun RecoverySparklineRow(uiState: CodexUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("CURRENT_RECOVERY", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${uiState.recoveryScore?.totalScore ?: "--"}%", 
                    color = when (uiState.recoveryScore?.status) {
                        com.neon.ascent.core.domain.workout.models.RecoveryStatus.OPTIMAL -> MaterialTheme.colorScheme.primary
                        com.neon.ascent.core.domain.workout.models.RecoveryStatus.CAUTION -> Color.Yellow
                        else -> Color.Red
                    },
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Black, 
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    uiState.recoveryScore?.status?.name ?: "UNKNOWN", 
                    color = Color.DarkGray, 
                    fontSize = 9.sp, 
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Box(modifier = Modifier.size(120.dp, 40.dp).background(Color.White.copy(alpha = 0.02f))) {
                val summaries = uiState.sessionSummaries
                if (summaries.size >= 2) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVol = summaries.maxOf { it.volume }.toFloat().coerceAtLeast(1f)
                        val path = Path()
                        summaries.forEachIndexed { index, summary ->
                            val x = index * (size.width / (summaries.size - 1))
                            val y = size.height - (summary.volume.toFloat() / maxVol * size.height)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFF00CCFF).copy(alpha = 0.4f), style = Stroke(1.dp.toPx()))
                    }
                } else {
                    Text("VOLUME_TREND", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun RecoveryMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun VitalsChart(data: List<com.neon.ascent.feature.codex.ui.VitalsPoint>, sessionSummaries: List<SessionSummary>) {
    if (data.isEmpty()) return
    val primaryColor = MaterialTheme.colorScheme.primary

    val minVal = data.minOf { it.value }.toFloat()
    val maxVal = data.maxOf { it.value }.toFloat()
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val startDate = data.first().date
    val endDate = data.last().date
    val daysRange = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp))
            .padding(top = 16.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        val width = size.width
        val height = size.height

        // Session Ticks
        sessionSummaries.forEach { session ->
            if ((session.date.isAfter(startDate) || session.date.isEqual(startDate)) && 
                (session.date.isBefore(endDate) || session.date.isEqual(endDate))) {
                val d = java.time.temporal.ChronoUnit.DAYS.between(startDate, session.date)
                val x = (d.toFloat() / daysRange) * width
                drawLine(
                    color = primaryColor.copy(alpha = 0.3f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Data Path
        val path = Path()
        data.forEachIndexed { index, point ->
            val d = java.time.temporal.ChronoUnit.DAYS.between(startDate, point.date)
            val x = (d.toFloat() / daysRange) * width
            val y = height - ((point.value.toFloat() - minVal) / range * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF00CCFF),
            style = Stroke(width = 2.dp.toPx())
        )

        // Points
        data.forEach { point ->
            val d = java.time.temporal.ChronoUnit.DAYS.between(startDate, point.date)
            val x = (d.toFloat() / daysRange) * width
            val y = height - ((point.value.toFloat() - minVal) / range * height)
            drawCircle(
                color = Color(0xFF00CCFF),
                radius = 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun FuelEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No fuel snapshots recorded.",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Weight or profile changes trigger logs.",
                color = Color.DarkGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun FuelChart(history: List<com.neon.ascent.core.domain.workout.models.FuelSnapshot>) {
    val data = history.sortedBy { it.timestamp }
    val primaryColor = MaterialTheme.colorScheme.primary
    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp))
                .padding(16.dp)
        ) {
            val width = size.width
            val height = size.height
            
            val minWeight = data.minOf { it.weightKg } * 0.95f
            val maxWeight = data.maxOf { it.weightKg } * 1.05f
            val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)
            
            val minTdee = data.minOf { it.tdee } * 0.9f
            val maxTdee = data.maxOf { it.tdee } * 1.1f
            val tdeeRange = (maxTdee - minTdee).coerceAtLeast(1f)
            
            val start = data.first().timestamp.toEpochMilli()
            val end = data.last().timestamp.toEpochMilli()
            val timeRange = (end - start).coerceAtLeast(1).toFloat()

            // TDEE Background Path (Step)
            val tdeePath = Path()
            data.forEachIndexed { i, snapshot ->
                val x = (snapshot.timestamp.toEpochMilli() - start) / timeRange * width
                val y = height - ((snapshot.tdee - minTdee) / tdeeRange * height)
                if (i == 0) {
                    tdeePath.moveTo(x, y)
                } else {
                    val prevX = (data[i-1].timestamp.toEpochMilli() - start) / timeRange * width
                    val prevY = height - ((data[i-1].tdee - minTdee) / tdeeRange * height)
                    tdeePath.lineTo(x, prevY)
                    tdeePath.lineTo(x, y)
                }
            }
            drawPath(tdeePath, Color(0xFF00CCFF).copy(alpha = 0.15f), style = Stroke(1.dp.toPx()))

            // Weight Line
            val weightPath = Path()
            data.forEachIndexed { i, snapshot ->
                val x = (snapshot.timestamp.toEpochMilli() - start) / timeRange * width
                val y = height - ((snapshot.weightKg - minWeight) / weightRange * height)
                if (i == 0) weightPath.moveTo(x, y) else weightPath.lineTo(x, y)
            }
            drawPath(weightPath, primaryColor, style = Stroke(2.dp.toPx()))
            
            data.forEach { snapshot ->
                val x = (snapshot.timestamp.toEpochMilli() - start) / timeRange * width
                val y = height - ((snapshot.weightKg - minWeight) / weightRange * height)
                drawCircle(primaryColor, 3.dp.toPx(), Offset(x, y))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Latest Metrics
        val latest = data.last()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FuelMetric("WEIGHT", "${latest.weightKg}kg", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            FuelMetric("TDEE", "${latest.tdee}", Color(0xFF00CCFF), Modifier.weight(1f))
            FuelMetric("PROTEIN", "${latest.protein}g", Color(0xFFFF006E), Modifier.weight(1f))
        }
    }
}

@Composable
fun FuelMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun LoadingWing(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$label...",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

fun formatVolume(volume: Long): String {
    return if (volume >= 1000) "%.1fk".format(volume / 1000f) else volume.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDossierPane(
    uiState: CodexUiState,
    onBack: () -> Unit
) {
    val dossier = uiState.dossier
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(dossier?.exercise?.name?.uppercase() ?: "EXERCISE_DOSSIER", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "BACK")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (dossier == null) {
            LoadingWing("COMPILING_DOSSIER")
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionHeader("PROGRESSION_STEP_FUNCTION")
            DossierChart(dossier)

            Spacer(modifier = Modifier.height(24.dp))

            if (dossier.nextIncrementCopy != null) {
                ProgressionBanner(
                    text = dossier.nextIncrementCopy!!,
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Bolt
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            SectionHeader("SESSION_HISTORY")
            dossier.sessions.forEach { session ->
                DossierSessionRow(session)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun DossierChart(dossier: ExerciseDossier) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp))
            .padding(16.dp)
    ) {
        val data = dossier.weightSeries
        if (data.isEmpty()) return@Canvas

        val maxVal = data.maxOf { it.value }.coerceAtLeast(1f)
        val minVal = data.minOf { it.value }.coerceAtMost(maxVal - 1f)
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val path = Path()
        data.forEachIndexed { index, point ->
            val x = index * (size.width / (data.size - 1).coerceAtLeast(1))
            val y = size.height - ((point.value - minVal) / range * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFF00CCFF), style = Stroke(2.dp.toPx()))
    }
}

@Composable
fun DossierSessionRow(session: DossierSession) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(session.date.toString(), color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("${session.weight.toInt()} LBS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Text(session.displaySummary, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ProgressionBanner(
    text: String,
    color: Color,
    icon: ImageVector
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color.Gray,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ProtocolMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun MuscleFrequencyRow(frequency: Map<String, Int>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(frequency.toList().sortedByDescending { it.second }) { (group, count) ->
            Surface(
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(2.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(group.uppercase(), color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$count", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun SessionHeatmap(summaries: List<SessionSummary>, period: CodexPeriod) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val daysToShow = when (period) {
        CodexPeriod.SEVEN_DAYS -> 7
        CodexPeriod.THIRTY_DAYS -> 35
        CodexPeriod.NINETY_DAYS -> 91
        CodexPeriod.YTD -> 364
        CodexPeriod.ALL -> 364
    }
    
    val today = LocalDate.now()
    val startDate = today.minusDays((daysToShow - 1).toLong())
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(daysToShow) { index ->
                val date = startDate.plusDays(index.toLong())
                val session = summaries.find { it.date == date }
                val isHit = session != null
                val isDeload = session?.isDeload == true
                val isFuture = date.isAfter(today)
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            when {
                                isFuture -> Color.Transparent
                                isDeload -> primaryColor.copy(alpha = 0.3f)
                                isHit -> primaryColor
                                else -> Color.White.copy(alpha = 0.05f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, subValue: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(90.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(4.dp))
                Text(subValue, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp), fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun CalendarStat(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(2.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("$count", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun PrRail(prs: List<PrDisplayData>, onSelect: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(prs) { pr ->
            Surface(
                modifier = Modifier
                    .width(220.dp)
                    .clickable { onSelect(pr.exerciseId) },
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        pr.exerciseName.uppercase(), 
                        color = Color.White, 
                        fontWeight = FontWeight.Black, 
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (pr.heaviestWeightDate != null) {
                        PrRow("HEAVY", "${pr.heaviestWeight.toInt()} LBS x ${pr.heaviestWeightReps}", Color(0xFF00CCFF))
                    }
                    
                    if (pr.bestClusterDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        PrRow("CLUSTER", "${pr.bestClusterReps} REPS @ ${pr.bestClusterWeight.toInt()}", MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun PrRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun PlaceholderWing(title: String, description: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$title // OFFLINE",
                color = Color.Red.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(32.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(120.dp).height(2.dp),
                color = Color.Red.copy(alpha = 0.3f),
                trackColor = Color.Transparent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePicker(
    uiState: CodexUiState,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onQueryChanged: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
            OutlinedTextField(
                value = uiState.exerciseSearchQuery,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("SEARCH_EXERCISE", color = Color.DarkGray) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.DarkGray
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val filtered = uiState.availableExercises
                .filter { it.name.contains(uiState.exerciseSearchQuery, ignoreCase = true) }
                .sortedByDescending { uiState.periodExerciseIds.contains(it.id) }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { exercise ->
                    val isRecent = uiState.periodExerciseIds.contains(exercise.id)
                    Surface(
                        onClick = { onSelect(exercise.id) },
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, if (isRecent) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(exercise.name, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            if (isRecent) {
                                Text("RECENT", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
