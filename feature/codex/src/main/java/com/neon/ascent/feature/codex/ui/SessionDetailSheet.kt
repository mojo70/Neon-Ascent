package com.neon.ascent.feature.codex.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.common.LocalVisualMode
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.SetType
import com.neon.ascent.core.domain.workout.models.WorkoutLog
import com.neon.ascent.core.domain.workout.models.WorkoutSession
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface HistoricalSetDisplay {
    data class SingleSet(val set: SetLog, val setNumber: Int) : HistoricalSetDisplay
    data class ClusterGroup(val weight: Float, val miniSets: List<SetLog>) : HistoricalSetDisplay
}

fun groupSetsForDisplay(sets: List<SetLog>): List<HistoricalSetDisplay> {
    val result = mutableListOf<HistoricalSetDisplay>()
    var currentClusterSets = mutableListOf<SetLog>()
    var normalSetCount = 1

    fun flushCluster() {
        if (currentClusterSets.isNotEmpty()) {
            val weight = currentClusterSets.firstOrNull()?.weight ?: 0f
            result.add(HistoricalSetDisplay.ClusterGroup(weight = weight, miniSets = currentClusterSets.toList()))
            currentClusterSets = mutableListOf()
        }
    }

    sets.forEach { set ->
        val isClusterSet = (set.clusterMiniSetIndex != null) || (set.type == SetType.REST_PAUSE)
        if (isClusterSet) {
            currentClusterSets.add(set)
        } else {
            flushCluster()
            val setNum = if (set.type == SetType.NORMAL) normalSetCount++ else 0
            result.add(HistoricalSetDisplay.SingleSet(set = set, setNumber = setNum))
        }
    }
    flushCluster()
    return result
}

fun formatSetWeight(weight: Float): String {
    return if (weight <= 0f) "BW" else if ((weight % 1f) == 0f) "${weight.toInt()} lbs" else "$weight lbs"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailSheet(
    sessionDetail: Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>,
    onDismiss: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val visualMode = LocalVisualMode.current

    val primaryAccent = when (visualMode) {
        VisualMode.STEVE -> theme.ink
        VisualMode.PILOT -> theme.accent
        VisualMode.CYBER -> Color(0xFF00FF9C)
    }
    val secondaryAccent = when (visualMode) {
        VisualMode.STEVE -> theme.inkMuted
        VisualMode.PILOT -> theme.inkMuted
        VisualMode.CYBER -> Color(0xFF00CCFF)
    }
    val deloadColor = when (visualMode) {
        VisualMode.STEVE -> theme.inkMuted
        VisualMode.PILOT -> Color(0xFFFF8C00)
        VisualMode.CYBER -> Color(0xFFFF8C00)
    }

    val (session, logs) = sessionDetail
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy · HH:mm")
    val formattedDate = session.date.atZone(ZoneId.systemDefault()).format(dateFormatter).uppercase()

    val totalVolume = logs.sumOf { (_, sets) ->
        sets.sumOf { (it.weight * it.reps).toLong() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.canvas,
        contentColor = theme.ink,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = primaryAccent.copy(alpha = 0.5f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORICAL_SESSION_DETAIL",
                    color = primaryAccent,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = theme.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.surface,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, theme.hairline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedDate,
                            color = theme.inkMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        if (session.isDeload) {
                            Box(
                                modifier = Modifier
                                    .background(deloadColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, deloadColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DELOAD",
                                    color = deloadColor,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = session.protocol.displayName,
                            color = theme.ink,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )

                        session.protocolDayType?.let { dayType ->
                            Box(
                                modifier = Modifier
                                    .background(secondaryAccent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .border(1.dp, secondaryAccent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = dayType.name,
                                    color = secondaryAccent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Metadata Row
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (session.durationSeconds > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = theme.inkMuted, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "${session.durationSeconds / 60}m",
                                    color = theme.ink,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        session.sessionRpe?.let { rpe ->
                            Text(
                                text = "RPE $rpe",
                                color = secondaryAccent,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        session.jointHealth?.let { jh ->
                            Text(
                                text = "JOINT $jh/3",
                                color = theme.inkMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = secondaryAccent, modifier = Modifier.size(12.dp))
                            Text(
                                text = "$totalVolume LBS",
                                color = primaryAccent,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Primary Augment / Notes
                    val primaryAugment = logs.firstOrNull { it.first.augmentId == session.primaryAugmentId }?.first?.augmentName
                        ?: logs.firstOrNull { it.first.augmentName != null }?.first?.augmentName

                    if (primaryAugment != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SUB-PROTOCOL // $primaryAugment",
                            color = secondaryAccent,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!session.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NOTES: ${session.notes}",
                            color = theme.inkMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = theme.hairline)
            Spacer(modifier = Modifier.height(16.dp))

            // Exercises Section
            Text(
                text = "LOGGED_EXERCISES // ${logs.size}",
                color = theme.inkMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            logs.forEachIndexed { index, (log, sets) ->
                val exerciseVolume = sets.sumOf { (it.weight * it.reps).toLong() }
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    color = theme.surface,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, theme.hairline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Exercise Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. ${log.exerciseName.uppercase()}",
                                    color = theme.ink,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                if (log.augmentName != null) {
                                    Text(
                                        text = "+ ${log.augmentName}",
                                        color = secondaryAccent,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            if (exerciseVolume > 0) {
                                Text(
                                    text = "$exerciseVolume LBS",
                                    color = primaryAccent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sets
                        val displays = groupSetsForDisplay(sets)
                        displays.forEach { display ->
                            when (display) {
                                is HistoricalSetDisplay.SingleSet -> {
                                    SingleSetRow(display.set, display.setNumber)
                                }
                                is HistoricalSetDisplay.ClusterGroup -> {
                                    ClusterGroupRow(display.weight, display.miniSets)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SingleSetRow(set: SetLog, setNumber: Int) {
    val theme = LocalNeonTheme.current
    val visualMode = LocalVisualMode.current

    val labelStr = when (set.type) {
        SetType.NORMAL -> if (setNumber > 0) setNumber.toString() else "1"
        SetType.WARMUP -> "W"
        SetType.DROP -> "D"
        SetType.FAILURE -> "F"
        SetType.REST_PAUSE -> if (set.clusterMiniSetIndex != null) "RP ${set.clusterMiniSetIndex}" else "RP"
        SetType.WIDOWMAKER -> "WM"
        SetType.POWER -> "P"
        SetType.GS -> "GS"
        SetType.PARTIAL -> "PAR"
        SetType.STRETCH -> "STR"
        SetType.MAX_EFFORT -> "ME"
    }

    val badgeColor = if (visualMode == VisualMode.STEVE) {
        theme.ink
    } else {
        when (set.type) {
            SetType.WARMUP -> Color(0xFFFFA500)
            SetType.DROP -> Color(0xFF00CCFF)
            SetType.FAILURE -> Color(0xFFFF4444)
            SetType.REST_PAUSE -> Color(0xFF00FFAA)
            SetType.WIDOWMAKER -> Color(0xFFFF00FF)
            SetType.POWER -> Color(0xFFFFD700)
            SetType.GS -> Color(0xFF00CCFF)
            SetType.PARTIAL -> theme.accent
            SetType.STRETCH -> Color(0xFFFF006E)
            SetType.MAX_EFFORT -> Color(0xFFFF0000)
            else -> theme.ink
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelStr,
                    color = badgeColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${formatSetWeight(set.weight)} × ${if (set.type == SetType.STRETCH) "${set.reps}s" else "${set.reps} reps"}",
                color = theme.ink,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            set.rir?.let { rir ->
                Text(
                    text = "@$rir RIR",
                    color = theme.inkMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (set.isAmrap) {
                Text("[AMRAP]", color = badgeColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            if (set.isLengthenedPartial) {
                Text("[PARTIAL]", color = theme.inkMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }

            if (set.isLoadedStretch || set.stretchDurationSeconds != null) {
                val dur = set.stretchDurationSeconds ?: 10
                Text("[STR ${dur}s]", color = theme.inkMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun ClusterGroupRow(weight: Float, miniSets: List<SetLog>) {
    val theme = LocalNeonTheme.current
    val visualMode = LocalVisualMode.current

    val clusterBadgeColor = when (visualMode) {
        VisualMode.STEVE -> theme.ink
        VisualMode.PILOT -> theme.accent
        VisualMode.CYBER -> Color(0xFF00FF9C)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = theme.canvas,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, clusterBadgeColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Cluster Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RP CLUSTER @ ${formatSetWeight(weight)}",
                    color = clusterBadgeColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${miniSets.sumOf { it.reps }} REPS",
                    color = theme.inkMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Mini Sets explicit listing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                miniSets.sortedBy { it.clusterMiniSetIndex ?: 0 }.forEachIndexed { idx, miniSet ->
                    val miniIndex = miniSet.clusterMiniSetIndex ?: (idx + 1)
                    val rirStr = miniSet.rir?.let { " @$it" } ?: ""
                    val weightDiffStr = if (miniSet.weight != weight && miniSet.weight > 0f) " (${formatSetWeight(miniSet.weight)})" else ""

                    Box(
                        modifier = Modifier
                            .background(clusterBadgeColor.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                            .border(1.dp, clusterBadgeColor.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$miniIndex: ${miniSet.reps}$weightDiffStr$rirStr",
                            color = theme.ink,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Any flags across mini-sets (e.g. stretch, partials)
            val hasPartials = miniSets.any { it.isLengthenedPartial }
            val stretchSet = miniSets.find { it.isLoadedStretch || it.stretchDurationSeconds != null }
            if (hasPartials || stretchSet != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasPartials) {
                        Text("[PARTIAL]", color = theme.inkMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (stretchSet != null) {
                        val dur = stretchSet.stretchDurationSeconds ?: 10
                        Text("[LOADED STRETCH ${dur}s]", color = theme.inkMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
