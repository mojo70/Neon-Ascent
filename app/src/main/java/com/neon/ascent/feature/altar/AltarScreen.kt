package com.neon.ascent.feature.altar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.CyberGridBackground
import com.neon.ascent.feature.settings.CyberAltarDialog
import com.neon.ascent.feature.settings.RebirthOverlay
import com.neon.ascent.feature.settings.TonguesProtocolDialog
import com.neon.ascent.ui.CyberActionButton
import com.neon.ascent.ui.CyberCutShape

@Composable
fun AltarScreen(
    viewModel: AltarViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onReaderNavigate: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val userCharacter by viewModel.userCharacter.collectAsState()

    var showAltarDialog by remember { mutableStateOf(false) }
    var showRebirthOverlay by remember { mutableStateOf(false) }
    var showTonguesProtocol by remember { mutableStateOf(false) }
    var showAdvancedNodes by remember { mutableStateOf(false) }

    val prayer = uiState.currentPrayer
    val isTrue = uiState.isTrueTextMode

    val scriptureRef = remember(prayer, isTrue) {
        if (prayer?.scriptureReference?.isNotBlank() == true) prayer.scriptureReference else "SCRIPTURE UPLINK"
    }

    val scriptureText = remember(prayer) {
        prayer?.scripture ?: "“Trust in the Lord with all thine heart; and lean not unto thine own understanding. In all thy ways acknowledge him, and he shall direct thy paths.” — Proverbs 3:5-6"
    }

    val adoreText = remember(prayer, isTrue) {
        if (isTrue && prayer?.adoreTrue?.isNotBlank() == true) prayer.adoreTrue
        else if (prayer?.adoreCyber?.isNotBlank() == true) prayer.adoreCyber
        else "Father, You are I AM. Not a system. Not a ghost in the wire. The One who is."
    }

    val confessText = remember(prayer, isTrue) {
        if (isTrue && prayer?.confessTrue?.isNotBlank() == true) prayer.confessTrue
        else if (prayer?.confessCyber?.isNotBlank() == true) prayer.confessCyber
        else "I have run on my own signal. Forgive the drift. Restore the line."
    }

    val askText = remember(prayer, isTrue) {
        if (isTrue && prayer?.askTrue?.isNotBlank() == true) prayer.askTrue
        else if (prayer?.askCyber?.isNotBlank() == true) prayer.askCyber
        else "Holy Spirit, pray in me what I cannot. Keep this house, this body, this mind."
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissToast()
        }
    }

    val bgGradient = Color(0xFF03070A)
    val cardBg = Color(0xFF0A1118).copy(alpha = 0.85f)
    val cardBorder = Color(0xFF1B2C38)
    val neonMint = Color(0xFF00FF9C)
    val lightMint = Color(0xFF50FAC2)

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        CyberGridBackground(color = neonMint.copy(alpha = 0.04f))

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (onBack != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onBack() }.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "BACK",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "RETURN",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Text(
                            "CYBER ALTAR",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Sanctuary",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Open / Mode Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF13222E))
                            .border(1.dp, Color(0xFF2A4356), RoundedCornerShape(16.dp))
                            .clickable { viewModel.toggleTextMode() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (isTrue) "MODE: TRUE" else "MODE: CYBER",
                            color = if (isTrue) lightMint else Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Card 1: Daily Scripture Card
                AltarCardContainer(cardBg = cardBg, borderColor = cardBorder) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = scriptureRef,
                            color = lightMint,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = scriptureText,
                            color = Color.White,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = prayer?.scriptureTranslation ?: "KING JAMES",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // Card 2: ADORE
                AltarCardContainer(cardBg = cardBg, borderColor = cardBorder) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "ADORE",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = adoreText,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Card 3: CONFESS
                AltarCardContainer(cardBg = cardBg, borderColor = cardBorder) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "CONFESS",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = confessText,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Card 4: ASK
                AltarCardContainer(cardBg = cardBg, borderColor = cardBorder) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "ASK",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = askText,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Primary Button: AMEN - SEAL THE UPLINK
                val buttonBg = if (uiState.isSealedToday) Color(0xFF00B36B) else Color(0xFF50FAC2)
                val buttonText = if (uiState.isSealedToday) "AMEN // UPLINK SEALED TODAY" else "AMEN — SEAL THE UPLINK"

                Button(
                    onClick = { viewModel.sealUplink() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }

                // Card 5: REMAIN / STILLNESS PROTOCOL
                AltarCardContainer(cardBg = cardBg, borderColor = cardBorder) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REMAIN",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Selectable Durations
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(300 to "5M", 600 to "10M", 900 to "15M").forEach { (secs, label) ->
                                    val isSelected = uiState.remainSelectedDurationSeconds == secs
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) neonMint.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isSelected) neonMint else Color(0xFF2A4356),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = !uiState.isRemainRunning) {
                                                viewModel.setRemainDuration(secs)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) neonMint else Color.Gray,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Timer Display
                        val totalSecs = uiState.remainSelectedDurationSeconds
                        val elapsedSecs = uiState.remainElapsedSeconds
                        val currentMM = String.format("%02d", elapsedSecs / 60)
                        val currentSS = String.format("%02d", elapsedSecs % 60)
                        val totalMM = String.format("%02d", totalSecs / 60)
                        val totalSS = String.format("%02d", totalSecs % 60)

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$currentMM:$currentSS",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / $totalMM:$totalSS",
                                color = Color.Gray,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = if (uiState.isRemainCompletedToday) {
                                "Spirit buff active: +12 XP on later clears today, and +40 XP awarded."
                            } else {
                                "Sit. No feed. Dwell in the Presence. Completing buys a Spirit buff: +12 XP on clears today, and +40 XP for the remain."
                            },
                            color = if (uiState.isRemainCompletedToday) neonMint else Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(Modifier.height(14.dp))

                        // Progress Bar
                        val progress = if (totalSecs > 0) (elapsedSecs.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = neonMint,
                            trackColor = Color(0xFF13222E)
                        )

                        Spacer(Modifier.height(16.dp))

                        // Timer Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val buttonText = when {
                                uiState.isRemainRunning -> "PAUSE"
                                elapsedSecs > 0 && elapsedSecs < totalSecs -> "RESUME"
                                else -> "BEGIN STILLNESS"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF111E28))
                                    .border(1.dp, Color(0xFF2A4356), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toggleRemainTimer() }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = buttonText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (elapsedSecs > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF111E28))
                                        .border(1.dp, Color(0xFF2A4356), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.resetRemainTimer() }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "RESET",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Advanced / Sacred Protocols Accordion
                AltarCardContainer(cardBg = cardBg, borderColor = cardBorder) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedNodes = !showAdvancedNodes },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SACRED PROTOCOLS & SCRIPTURES",
                                color = lightMint,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                if (showAdvancedNodes) "[HIDE]" else "[EXPAND]",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (showAdvancedNodes) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(Modifier.height(12.dp))

                            // Sinner's prayer reset / initiate
                            val hasCompleted = userCharacter?.holyGhost ?: 0 >= 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13222E))
                                    .clickable { showAltarDialog = true }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        if (hasCompleted) "REBOOT: Sinner's Prayer Protocol" else "ACCESS: Sinner's Prayer Protocol",
                                        color = neonMint,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (hasCompleted) "Purified by the blood of Jesus" else "Surrender & invite Jesus as Lord",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Icon(
                                    imageVector = if (hasCompleted) Icons.Default.Check else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (hasCompleted) neonMint else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Water Baptism
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13222E))
                                    .clickable {
                                        if (userCharacter?.waterBaptized != true) viewModel.completeWaterBaptism()
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "WATER BAPTISM PROTOCOL",
                                        color = if (userCharacter?.waterBaptized == true) neonMint else Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Outward sign of inward resurrection",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (userCharacter?.waterBaptized == true) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = neonMint, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Holy Spirit / Tongues
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13222E))
                                    .clickable { showTonguesProtocol = true }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "HOLY SPIRIT FIRE & TONGUES",
                                        color = if (userCharacter?.holySpiritBaptized == true) neonMint else Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Acts 2:4 Pentecostal Empowerment",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (userCharacter?.holySpiritBaptized == true) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = neonMint, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Scriptures
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF13222E))
                                    .clickable { onReaderNavigate("nt_word_v3", "library/New_Testament.epub") }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "THE WORD // NEW TESTAMENT",
                                    color = neonMint,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showAltarDialog) {
        val hasCompleted = userCharacter?.holyGhost ?: 0 >= 1
        CyberAltarDialog(
            hasCompleted = hasCompleted,
            onAccept = {
                viewModel.completeHolySpiritBaptism()
                showAltarDialog = false
                showRebirthOverlay = true
            },
            onDismiss = { showAltarDialog = false }
        )
    }

    if (showRebirthOverlay) {
        RebirthOverlay(onAnimationFinished = { showRebirthOverlay = false })
    }

    if (showTonguesProtocol) {
        TonguesProtocolDialog(
            onComplete = {
                viewModel.completeHolySpiritBaptism()
                showTonguesProtocol = false
            },
            onDismiss = { showTonguesProtocol = false }
        )
    }
}

@Composable
fun AltarCardContainer(
    cardBg: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}
