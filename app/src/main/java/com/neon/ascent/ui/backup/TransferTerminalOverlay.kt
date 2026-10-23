package com.neon.ascent.ui.backup

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.domain.backup.models.RestoreProgress
import com.neon.ascent.core.domain.backup.models.RestoreStage
import com.neon.ascent.data.backup.p2p.P2pClientState
import kotlinx.coroutines.delay
import kotlin.random.Random

data class TerminalLogItem(
    val stage: RestoreStage,
    val message: String,
    val isComplete: Boolean = true
)

@Composable
fun TransferTerminalOverlay(
    progress: RestoreProgress?,
    p2pClientState: P2pClientState? = null,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    val theme = LocalNeonTheme.current
    val isSteve = theme.mode == VisualMode.STEVE

    // Visual Tokens
    val bgCanvas = if (isSteve) Color(0xFF141414) else Color(0xFF070A0F)
    val terminalBoxBg = if (isSteve) Color(0xFF1A1A1A) else Color(0xFF0C1017)
    val textPrimary = if (isSteve) Color(0xFFE0E0E0) else Color(0xFF00E5FF)
    val textSuccess = if (isSteve) Color(0xFFFFFFFF) else Color(0xFF00FF66)
    val textWarn = if (isSteve) Color(0xFFCCCCCC) else Color(0xFFFFB300)
    val textError = if (isSteve) Color(0xFFFF6B6B) else Color(0xFFFF3366)
    val borderStroke = if (isSteve) Color(0xFF333333) else Color(0xFF00E5FF).copy(alpha = 0.5f)

    // Rolling SHA-256 Checksum State
    var rollingHex by remember { mutableStateOf("0000000000000000") }
    val lockedChecksum = progress?.checksumHex

    LaunchedEffect(progress?.stage, lockedChecksum) {
        if (lockedChecksum != null) {
            rollingHex = lockedChecksum
        } else {
            val hexChars = "0123456789abcdef"
            while (progress?.stage != RestoreStage.READY && progress?.stage != RestoreStage.FAIL && lockedChecksum == null) {
                rollingHex = (1..16).map { hexChars[Random.nextInt(hexChars.length)] }.joinToString("")
                delay(50)
            }
        }
    }

    // Cursor blink transition for CYBER mode
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor"
    )

    // Auto-dismiss hold when READY (~400 ms)
    var isHoldingReady by remember { mutableStateOf(false) }
    LaunchedEffect(progress?.stage) {
        if (progress?.stage == RestoreStage.READY) {
            isHoldingReady = true
            delay(450)
            isHoldingReady = false
        }
    }

    // Log History
    val logItems = remember(progress, p2pClientState) {
        val list = mutableListOf<TerminalLogItem>()

        if (p2pClientState is P2pClientState.Downloading) {
            val pct = p2pClientState.progressPercent
            val msg = if (pct >= 0) "RECEIVING STREAM... $pct%" else "RECEIVING STREAM..."
            list.add(TerminalLogItem(RestoreStage.RECEIVING, msg, isComplete = false))
        } else if (p2pClientState is P2pClientState.Decrypting) {
            list.add(TerminalLogItem(RestoreStage.DECRYPT, "DECRYPTING AES-256-GCM PAYLOAD...", isComplete = false))
        }

        progress?.let { prg ->
            when (prg.stage) {
                RestoreStage.HANDSHAKE -> list.add(TerminalLogItem(RestoreStage.HANDSHAKE, "ESTABLISHING SECURE P2P LINK..."))
                RestoreStage.RECEIVING -> list.add(TerminalLogItem(RestoreStage.RECEIVING, "RECEIVING PAYLOAD OVER ENCRYPTED CHANNEL..."))
                RestoreStage.READING -> list.add(TerminalLogItem(RestoreStage.READING, "READING BACKUP FILE PAYLOAD..."))
                RestoreStage.DECRYPT -> list.add(TerminalLogItem(RestoreStage.DECRYPT, "DECRYPTING PAYLOAD STREAM..."))
                RestoreStage.PARSE -> list.add(TerminalLogItem(RestoreStage.PARSE, "PARSING JSON PAYLOAD STRUCTURE..."))
                RestoreStage.CHECKSUM -> list.add(TerminalLogItem(RestoreStage.CHECKSUM, "SHA-256 CHECKSUM VERIFIED: $rollingHex"))
                RestoreStage.RESTORE_WORKOUT -> list.add(TerminalLogItem(RestoreStage.RESTORE_WORKOUT, "REBUILDING WORKOUT GRAPH & PROFILES..."))
                RestoreStage.RESTORE_DOMAINS -> list.add(TerminalLogItem(RestoreStage.RESTORE_DOMAINS, "RESTORING GOALS, VAULT, SELFMAP, BIO, CHAT, RITES, LIBRARY..."))
                RestoreStage.RESTORE_PREFS -> list.add(TerminalLogItem(RestoreStage.RESTORE_PREFS, "RESTORING BRIEF, DIRECTIVE, HEALTH & SYSTEM FLAGS..."))
                RestoreStage.INTEGRITY -> list.add(TerminalLogItem(RestoreStage.INTEGRITY, "RUNNING DATABASE INTEGRITY CHECKS..."))
                RestoreStage.READY -> list.add(TerminalLogItem(RestoreStage.READY, "RESTORE COMPLETE. ALL DOMAINS SYNCED."))
                RestoreStage.FAIL -> list.add(TerminalLogItem(RestoreStage.FAIL, "RESTORE FAILED: ${prg.error ?: prg.message}"))
            }
        }

        list
    }

    val listState = rememberLazyListState()
    LaunchedEffect(logItems.size) {
        if (logItems.isNotEmpty()) {
            listState.animateScrollToItem(logItems.size - 1)
        }
    }

    Dialog(
        onDismissRequest = {
            if (progress?.stage == RestoreStage.FAIL || progress?.stage == RestoreStage.READY) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgCanvas)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // CYBER scanline overlay
            if (!isSteve) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineSpacing = 6.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.03f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += lineSpacing
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .border(1.dp, borderStroke, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = terminalBoxBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NEON ASCENT // TRANSFER TERMINAL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        )
                        val statusBadge = when (progress?.stage) {
                            RestoreStage.READY -> "[READY]"
                            RestoreStage.FAIL -> "[FAIL]"
                            else -> "[RUNNING]"
                        }
                        Text(
                            text = statusBadge,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = when (progress?.stage) {
                                    RestoreStage.READY -> textSuccess
                                    RestoreStage.FAIL -> textError
                                    else -> textWarn
                                },
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Checksum Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgCanvas, RoundedCornerShape(4.dp))
                            .border(1.dp, borderStroke.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SHA-256: $rollingHex",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (lockedChecksum != null) textSuccess else textWarn,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = if (lockedChecksum != null) "LOCKED" else "VERIFYING...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (lockedChecksum != null) textSuccess else textWarn,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terminal Monospace Logs
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(bgCanvas.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        items(logItems) { log ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "> ${log.message}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = when (log.stage) {
                                            RestoreStage.READY -> textSuccess
                                            RestoreStage.FAIL -> textError
                                            else -> textPrimary.copy(alpha = 0.9f)
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }

                        if (progress?.stage != RestoreStage.READY && progress?.stage != RestoreStage.FAIL) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "> EXECUTING RESTORE PIPELINE",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = textWarn,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    )
                                    if (!isSteve) {
                                        Text(
                                            text = "_",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = textPrimary.copy(alpha = cursorAlpha),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Post-Restore Summary or Fail Actions
                    when (progress?.stage) {
                        RestoreStage.READY -> {
                            val res = progress.result
                            if (res != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bgCanvas, RoundedCornerShape(4.dp))
                                        .border(1.dp, textSuccess.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "RESTORE SUMMARY",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = textSuccess,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• Workout Sessions: ${res.restoredSessionsCount}\n" +
                                                "• Codex & Journal Entries: ${res.restoredCodexCount + res.restoredJournalCount}\n" +
                                                "• Rite Sessions: ${res.restoredRitesCount}\n" +
                                                "• Library Highlights: ${res.restoredHighlightsCount}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = textPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSteve) Color(0xFF333333) else Color(0xFF00E5FF)
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PROCEED TO MAIN HUB",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (isSteve) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }

                        RestoreStage.FAIL -> {
                            Button(
                                onClick = {
                                    if (onRetry != null) onRetry() else onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = textError
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "RETURN / DISMISS",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }

                        else -> {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isSteve) Color.White else Color(0xFF00E5FF),
                                trackColor = borderStroke.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
