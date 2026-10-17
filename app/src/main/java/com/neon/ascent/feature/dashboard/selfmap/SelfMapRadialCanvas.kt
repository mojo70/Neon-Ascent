package com.neon.ascent.feature.dashboard.selfmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.SoftGridBackground
import kotlin.math.cos
import kotlin.math.sin

fun getCategoryColor(category: SelfMapCategory): Color {
    return when (category) {
        SelfMapCategory.BODY_SENSES -> Color(0xFF00FFFF) // Cyan
        SelfMapCategory.PEOPLE -> Color(0xFFFF006E) // Pink/Magenta
        SelfMapCategory.STUFF -> Color(0xFFFFD700) // Gold
        SelfMapCategory.ROLES_ACCOMPLISHMENTS -> Color(0xFF00FF9F) // Neon Green
        SelfMapCategory.IDEAS_LIKES -> Color(0xFFA020F0) // Purple
        SelfMapCategory.CUSTOM -> Color(0xFFFF8C00) // Orange
    }
}

fun getLoadBandLabel(loadPercentage: Float): Pair<String, Color> {
    return when {
        loadPercentage < 60f -> "LIGHT LOAD" to Color(0xFF00FF9F)
        loadPercentage <= 110f -> "MODERATE LOAD" to Color(0xFF00FFFF)
        else -> "HEAVY ATTACHMENT LOAD" to Color(0xFFFF0055)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfMapRadialCanvas(
    viewModel: SelfMapViewModel,
    onOpenWizard: () -> Unit,
    onOpenConversationalIntake: () -> Unit = {},
    onOpenEoELog: (SelfItemEntity?) -> Unit,
    onAnchorDirective: (SelfItemEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent
    val danger = theme.accentDanger
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<SelfMapCategory?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<SelfItemEntity?>(null) }
    var showHelpSheet by remember { mutableStateOf(false) }

    // Filter items
    val filteredItems = remember(uiState.activeItems, searchQuery, selectedCategoryFilter) {
        uiState.activeItems.filter { item ->
            val matchesCategory = selectedCategoryFilter == null || item.category == selectedCategoryFilter
            val matchesQuery = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true) ||
                    (item.notes?.contains(searchQuery, ignoreCase = true) == true)
            matchesCategory && matchesQuery
        }
    }

    SelfMapBiometricGate(isLockEnabled = uiState.biometricLockEnabled) {
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
        ) {
            // --- TOP HUD OVERLAY & SEARCH/FILTERS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0E14).copy(alpha = 0.9f))
                    .padding(12.dp)
            ) {
                // Header + Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val (bandText, bandColor) = getLoadBandLabel(uiState.coreAttachmentLoad)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CORE LOAD: ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = "${uiState.coreAttachmentLoad.toInt()}% [$bandText]",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = bandColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        val formattedAvg = (Math.round(uiState.avgImportance * 100.0) / 100.0).toString()
                        Text(
                            text = "Avg Importance: $formattedAvg | Cores (≥0.7): ${uiState.coreCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        val reportedMoodText = uiState.latestMoodEntry?.let { "${it.score}/10" } ?: "--"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.triggerMoodCheckIn() }
                        ) {
                            Text(
                                text = "Your reported mood: ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = reportedMoodText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = cyan,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { viewModel.setBiometricLockEnabled(!uiState.biometricLockEnabled) }
                        ) {
                            Icon(
                                imageVector = if (uiState.biometricLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Biometric Lock",
                                tint = if (uiState.biometricLockEnabled) danger else cyan
                            )
                        }

                        IconButton(onClick = onOpenConversationalIntake) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Guided Start Conversation",
                                tint = cyan
                            )
                        }

                        IconButton(onClick = { showHelpSheet = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Help Guide",
                                tint = cyan
                            )
                        }

                        IconButton(onClick = onOpenWizard) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Attachments Wizard",
                                tint = cyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Filter self map nodes...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cyan,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("ALL [${uiState.activeItems.size}]", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    items(SelfMapCategory.entries) { cat ->
                        val catCount = uiState.activeItems.count { it.category == cat }
                        val isSel = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedCategoryFilter = if (isSel) null else cat },
                            label = { Text("${cat.displayName.uppercase()} [$catCount]", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getCategoryColor(cat),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // --- RADIAL MAP CANVAS AREA ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                InteractiveRadialMap(
                    items = filteredItems,
                    onNodeTapped = { selectedItemForDetail = it },
                    onNodeDragged = { item, newImportance ->
                        viewModel.updateItem(item.copy(importance = newImportance))
                    }
                )

                // Add with Mentor / Guided Start CTA
                OutlinedButton(
                    onClick = onOpenConversationalIntake,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cyan),
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, cyan.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text(
                        text = "ADD WITH MENTOR",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Log Reaction Floating CTA
                Button(
                    onClick = { onOpenEoELog(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = danger,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "LOG REACTION [EoE]",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

    // --- ITEM DETAIL SHEET ---
    if (selectedItemForDetail != null) {
        SelfItemDetailSheet(
            item = selectedItemForDetail!!,
            viewModel = viewModel,
            onDismiss = { selectedItemForDetail = null },
            onLogEoEForThisItem = {
                val current = selectedItemForDetail
                selectedItemForDetail = null
                onOpenEoELog(current)
            },
            onAnchorDirective = { item ->
                selectedItemForDetail = null
                onAnchorDirective(item)
            }
        )
    }

    // --- HELP TOOLTIP SHEET ---
    if (showHelpSheet) {
        SelfMapHelpSheet(onDismiss = { showHelpSheet = false })
    }

    // --- MOOD CHECK-IN SHEET ---
    if (uiState.showMoodCheckInSheet) {
        SelfMapMoodCheckInSheet(
            viewModel = viewModel,
            eoeId = uiState.pendingMoodEoeId,
            onDismiss = { viewModel.dismissMoodCheckIn() }
        )
    }
}

/**
 * Interactive Radial Node Map Canvas drawing center "SELF", concentric rings, and draggable category nodes.
 */
@Composable
fun InteractiveRadialMap(
    items: List<SelfItemEntity>,
    onNodeTapped: (SelfItemEntity) -> Unit,
    onNodeDragged: (SelfItemEntity, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent

    val currentItems by rememberUpdatedState(items)
    val currentOnNodeTapped by rememberUpdatedState(onNodeTapped)
    val currentOnNodeDragged by rememberUpdatedState(onNodeDragged)

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var draggingItemId by remember { mutableStateOf<Long?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (draggingItemId == null) {
                            zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.5f)
                            panOffset += pan
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            zoomScale = 1.0f
                            panOffset = Offset.Zero
                        },
                        onTap = { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = minOf(size.width, size.height) / 2f * 0.85f

                            val clickedItem = currentItems.firstOrNull { item ->
                                val modelOffset = calculateNodeOffset(item, currentItems, center, maxRadius)
                                val screenOffset = center + panOffset + (modelOffset - center) * zoomScale
                                (tapOffset - screenOffset).getDistance() <= 28.dp.toPx()
                            }
                            if (clickedItem != null) {
                                currentOnNodeTapped(clickedItem)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = minOf(size.width, size.height) / 2f * 0.85f

                            val target = currentItems.firstOrNull { item ->
                                val modelOffset = calculateNodeOffset(item, currentItems, center, maxRadius)
                                val screenOffset = center + panOffset + (modelOffset - center) * zoomScale
                                (startOffset - screenOffset).getDistance() <= 32.dp.toPx()
                            }
                            if (target != null) {
                                draggingItemId = target.id
                                dragPos = startOffset
                            } else {
                                draggingItemId = null
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (draggingItemId != null) {
                                change.consume()
                                dragPos += dragAmount
                            }
                        },
                        onDragEnd = {
                            val currentId = draggingItemId
                            if (currentId != null) {
                                val targetItem = currentItems.firstOrNull { it.id == currentId }
                                if (targetItem != null) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val maxRadius = minOf(size.width, size.height) / 2f * 0.85f
                                    val modelDragPos = center + (dragPos - center - panOffset) / zoomScale
                                    val currentDist = (modelDragPos - center).getDistance()
                                    val newImp = (1.0f - (currentDist / maxRadius)).coerceIn(0.0f, 1.0f)
                                    currentOnNodeDragged(targetItem, newImp)
                                }
                            }
                            draggingItemId = null
                        },
                        onDragCancel = {
                            draggingItemId = null
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val screenCenter = center + panOffset
            val maxRadius = minOf(size.width, size.height) / 2f * 0.85f

            // --- 1. VIVID CYBERPUNK GRID LINES ---
            val gridSpacing = 40.dp.toPx()
            val gridColor = Color(0xFF00FFFF).copy(alpha = 0.12f)
            val gridLinePx = 1.dp.toPx()

            for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = gridLinePx
                )
            }
            for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
                drawLine(
                    color = gridColor,
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), size.height),
                    strokeWidth = gridLinePx
                )
            }

            val ringTextPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 11.sp.toPx()
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // --- 2. HIGH-CONTRAST CONCENTRIC POWER RINGS ---
            val rCore = maxRadius * 0.3f * zoomScale
            drawCircle(
                color = Color(0xFFFF0055).copy(alpha = 0.15f),
                radius = rCore,
                center = screenCenter
            )
            drawCircle(
                color = Color(0xFFFF0055).copy(alpha = 0.85f),
                radius = rCore,
                center = screenCenter,
                style = Stroke(width = 2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f))
            )
            ringTextPaint.color = android.graphics.Color.parseColor("#FFFF0055")
            drawContext.canvas.nativeCanvas.drawText("CORE (0.7-1.0)", screenCenter.x, screenCenter.y - rCore + 24f, ringTextPaint)

            val rHigh = maxRadius * 0.6f * zoomScale
            drawCircle(
                color = Color(0xFF00FFFF).copy(alpha = 0.1f),
                radius = rHigh,
                center = screenCenter
            )
            drawCircle(
                color = Color(0xFF00FFFF).copy(alpha = 0.75f),
                radius = rHigh,
                center = screenCenter,
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )
            ringTextPaint.color = android.graphics.Color.parseColor("#FF00FFFF")
            drawContext.canvas.nativeCanvas.drawText("HIGH (0.4-0.7)", screenCenter.x, screenCenter.y - rHigh + 24f, ringTextPaint)

            val rMed = maxRadius * 0.8f * zoomScale
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.65f),
                radius = rMed,
                center = screenCenter,
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
            )
            ringTextPaint.color = android.graphics.Color.parseColor("#FFFFD700")
            drawContext.canvas.nativeCanvas.drawText("MEDIUM (0.2-0.4)", screenCenter.x, screenCenter.y - rMed + 24f, ringTextPaint)

            val scaledMaxRadius = maxRadius * zoomScale
            drawCircle(
                color = Color(0xFF00FF9F).copy(alpha = 0.55f),
                radius = scaledMaxRadius,
                center = screenCenter,
                style = Stroke(width = 1.5.dp.toPx())
            )
            ringTextPaint.color = android.graphics.Color.parseColor("#FF00FF9F")
            drawContext.canvas.nativeCanvas.drawText("PERIPHERAL (0.0-0.2)", screenCenter.x, screenCenter.y - scaledMaxRadius + 24f, ringTextPaint)

            // --- 3. CENTER "SELF" NODE ---
            drawCircle(
                color = Color(0xFF00FFFF).copy(alpha = 0.35f),
                radius = 36f,
                center = screenCenter
            )
            drawCircle(
                color = Color(0xFF00FFFF),
                radius = 22f,
                center = screenCenter,
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = screenCenter
            )
            ringTextPaint.color = android.graphics.Color.WHITE
            ringTextPaint.textSize = 12.sp.toPx()
            drawContext.canvas.nativeCanvas.drawText("SELF", screenCenter.x, screenCenter.y + 44f, ringTextPaint)

            // --- 4. DRAW ATTACHMENT NODES & TEXT LABELS ---
            val nodeLabelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 13.sp.toPx()
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.LEFT
                color = android.graphics.Color.WHITE
            }

            currentItems.forEach { item ->
                val screenNodePos = if (item.id == draggingItemId) {
                    dragPos
                } else {
                    val modelOffset = calculateNodeOffset(item, currentItems, center, maxRadius)
                    screenCenter + (modelOffset - center) * zoomScale
                }

                val catColor = getCategoryColor(item.category)

                drawLine(
                    color = catColor.copy(alpha = 0.65f),
                    start = screenCenter,
                    end = screenNodePos,
                    strokeWidth = 1.5.dp.toPx()
                )

                drawCircle(
                    color = catColor.copy(alpha = 0.45f),
                    radius = 18.dp.toPx(),
                    center = screenNodePos
                )

                drawCircle(
                    color = catColor,
                    radius = 11.dp.toPx(),
                    center = screenNodePos
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = screenNodePos
                )

                drawContext.canvas.nativeCanvas.drawText(
                    item.name,
                    screenNodePos.x + 22.dp.toPx(),
                    screenNodePos.y + 5.dp.toPx(),
                    nodeLabelPaint
                )
            }
        }

        // --- ZOOM CONTROLS OVERLAY ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(Color(0xFF0A0E14).copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                .border(1.dp, cyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { zoomScale = (zoomScale / 1.25f).coerceIn(0.5f, 3.5f) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Zoom Out",
                    tint = cyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "${(zoomScale * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier
                    .clickable {
                        zoomScale = 1.0f
                        panOffset = Offset.Zero
                    }
                    .padding(horizontal = 6.dp)
            )

            IconButton(
                onClick = { zoomScale = (zoomScale * 1.25f).coerceIn(0.5f, 3.5f) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom In",
                    tint = cyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (zoomScale != 1.0f || panOffset != Offset.Zero) {
                IconButton(
                    onClick = {
                        zoomScale = 1.0f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Zoom",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun calculateNodeOffset(
    item: SelfItemEntity,
    allItems: List<SelfItemEntity>,
    center: Offset,
    maxRadius: Float
): Offset {
    val index = allItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
    val total = allItems.size.coerceAtLeast(1)
    val angle = (2f * Math.PI * index / total) - (Math.PI / 2f) // Start top

    val dist = maxRadius * (1.0f - item.importance.coerceIn(0.0f, 1.0f))
    val x = center.x + (dist * cos(angle)).toFloat()
    val y = center.y + (dist * sin(angle)).toFloat()
    return Offset(x, y)
}

/**
 * Detail Sheet for viewing, editing, archiving, or deleting a SelfItemEntity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfItemDetailSheet(
    item: SelfItemEntity,
    viewModel: SelfMapViewModel,
    onDismiss: () -> Unit,
    onLogEoEForThisItem: () -> Unit,
    onAnchorDirective: (SelfItemEntity) -> Unit = {}
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent
    val danger = theme.accentDanger

    var nameInput by remember(item) { mutableStateOf(item.name) }
    var categoryInput by remember(item) { mutableStateOf(item.category) }
    var importanceInput by remember(item) { mutableFloatStateOf(item.importance) }
    var notesInput by remember(item) { mutableStateOf(item.notes ?: "") }
    var showConfirmDelete by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val linkedEoEs = remember(uiState.eoeEntries, item.id) {
        uiState.eoeEntries.filter { it.selfItemId == item.id }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10141A),
        scrimColor = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "SELF MAP // ITEM DETAILS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = cyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Item Name", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                items(SelfMapCategory.entries) { cat ->
                    val isSel = categoryInput == cat
                    FilterChip(
                        selected = isSel,
                        onClick = { categoryInput = cat },
                        label = { Text(cat.displayName, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getCategoryColor(cat),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Importance Slider
            val (ringLabel, ringColor) = getRingLabel(importanceInput)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "IMPORTANCE: ${(importanceInput * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = ringLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ringColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Slider(
                value = importanceInput,
                onValueChange = { importanceInput = it },
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = ringColor,
                    activeTrackColor = ringColor,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Notes / Description", fontFamily = FontFamily.Monospace) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Linked EoE Entries Section
            CyberFrame(
                label = "LINKED EOE REACTION LOGS [${linkedEoEs.size}]",
                borderColor = cyan,
                accentColor = cyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (linkedEoEs.isEmpty()) {
                        Text(
                            text = "No emotional reactions logged for this item yet.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    } else {
                        linkedEoEs.take(3).forEach { entry ->
                            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                Text(
                                    text = "• ${entry.emotionLabel.uppercase()} (Intensity: ${entry.intensity ?: 5}/10)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = danger,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Text(
                                    text = "EP: \"${entry.epText}\" | P: \"${entry.perceptionText}\"",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.LightGray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onLogEoEForThisItem,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "+ LOG REACTION FOR THIS ITEM",
                            color = cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LINKED CHRONICLE ENTRIES
            val linkedChronicle = remember(uiState.chronicleEntries, item.id) {
                uiState.chronicleEntries.filter { it.selfItemId == item.id }
            }
            var showAddChronicleDialog by remember { mutableStateOf(false) }
            var chronicleInputText by remember { mutableStateOf("") }

            CyberFrame(
                label = "LINKED CHRONICLE ENTRIES [${linkedChronicle.size}]",
                borderColor = cyan,
                accentColor = cyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (linkedChronicle.isEmpty()) {
                        Text(
                            text = "No Chronicle entries linked to this item yet.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    } else {
                        linkedChronicle.take(3).forEach { entry ->
                            Text(
                                text = "• \"${entry.content}\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showAddChronicleDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "+ WRITE LINKED CHRONICLE ENTRY",
                            color = cyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showAddChronicleDialog) {
                AlertDialog(
                    onDismissRequest = { showAddChronicleDialog = false },
                    containerColor = Color(0xFF10141A),
                    title = {
                        Text(
                            text = "WRITE CHRONICLE ENTRY",
                            style = MaterialTheme.typography.titleMedium.copy(color = cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Link entry to item: ${item.name}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = chronicleInputText,
                                onValueChange = { chronicleInputText = it },
                                placeholder = { Text("Write reflection...", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (chronicleInputText.isNotBlank()) {
                                    viewModel.saveLinkedChronicleEntry(chronicleInputText, item.id)
                                    chronicleInputText = ""
                                    showAddChronicleDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = Color.Black),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("SAVE TO CHRONICLE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddChronicleDialog = false }) {
                            Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Button(
                onClick = {
                    onDismiss()
                    onAnchorDirective(item)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ANCHOR A DIRECTIVE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.archiveItem(item.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("ARCHIVE", fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.updateItem(
                                item.copy(
                                    name = nameInput.trim(),
                                    category = categoryInput,
                                    importance = importanceInput,
                                    notes = notesInput.trim().ifEmpty { null }
                                )
                            )
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!showConfirmDelete) {
                TextButton(
                    onClick = { showConfirmDelete = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DELETE ITEM PERMANENTLY", color = danger, fontFamily = FontFamily.Monospace)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(danger.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CONFIRM DELETE?", color = danger, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Row {
                        TextButton(onClick = { showConfirmDelete = false }) {
                            Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                viewModel.deleteItem(item.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = danger, contentColor = Color.White),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("DELETE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
