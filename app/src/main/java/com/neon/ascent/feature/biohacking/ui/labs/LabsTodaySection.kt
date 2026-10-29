package com.neon.ascent.feature.biohacking.ui.labs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.domain.health.NeonCharge
import com.neon.ascent.core.domain.health.models.VitalsSnapshot
import com.neon.ascent.core.domain.labs.models.BiohackingProtocol
import com.neon.ascent.core.domain.labs.models.BiohackingProtocolOverride
import com.neon.ascent.core.domain.labs.models.Diet
import com.neon.ascent.core.domain.labs.models.Supplement
import com.neon.ascent.core.domain.labs.models.SupplementOverride
import com.neon.ascent.core.domain.workout.models.RecoveryScore
import com.neon.ascent.core.domain.workout.rules.Macros
import com.neon.ascent.feature.biohacking.VitalsStack
import java.time.LocalDate

@Composable
fun LabsTodaySection(
    snapshot: VitalsSnapshot?,
    rhrSeries: List<Pair<LocalDate, Double>>,
    hrvSeries: List<Pair<LocalDate, Double>>,
    completedSessions: Int,
    scheduledSessions: Int,
    recoveryScore: RecoveryScore?,
    neonCharge: NeonCharge?,
    macros: Macros?,
    hasNutritionPermission: Boolean,
    neonCyan: Color,
    neonMagenta: Color,
    activeDiet: Diet?,
    isHigherCarbDay: Boolean,
    activeSupplementStack: List<Supplement>,
    supplementOverrides: Map<String, SupplementOverride>,
    activeProtocols: List<BiohackingProtocol>,
    protocolOverrides: Map<String, BiohackingProtocolOverride>,
    onOpenDayCard: () -> Unit,
    onOpenSelfMap: () -> Unit,
    onNavigateToCodexDiet: () -> Unit,
    onNavigateToCodexSupplements: () -> Unit,
    onNavigateToCodexProtocols: () -> Unit,
    onNavigateToOps: () -> Unit,
    onRequestNutritionPermission: () -> Unit,
    onChargeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TODAY",
                color = neonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenDayCard) {
                Text("Day card", color = theme.ink, fontSize = 12.sp)
            }
            TextButton(onClick = onOpenSelfMap) {
                Text("Self map", color = theme.ink, fontSize = 12.sp)
            }
        }

        VitalsStack(
            snapshot = snapshot,
            rhrSeries = rhrSeries,
            hrvSeries = hrvSeries,
            completedSessions = completedSessions,
            scheduledSessions = scheduledSessions,
            recoveryScore = recoveryScore,
            neonCharge = neonCharge,
            macros = macros,
            hasNutritionPermission = hasNutritionPermission,
            neonCyan = neonCyan,
            neonMagenta = neonMagenta,
            onNavigateToOps = onNavigateToOps,
            onRequestNutritionPermission = onRequestNutritionPermission,
            onChargeClick = onChargeClick
        )

        CatalogRow(
            label = "Diet",
            value = activeDiet?.name ?: "No diet selected",
            detail = if (activeDiet != null && isHigherCarbDay) "Refeed" else null,
            action = if (activeDiet == null) "Open catalog" else null,
            accent = neonCyan,
            detailColor = neonMagenta,
            onClick = onNavigateToCodexDiet
        )
        HorizontalDivider(color = neonCyan.copy(alpha = 0.2f))
        val stackNames = activeSupplementStack.joinToString(", ") { supplement ->
            val paused = supplementOverrides[supplement.id]?.isPaused == true
            if (paused) "${supplement.name} (paused)" else supplement.name
        }
        CatalogRow(
            label = "Stack",
            value = if (activeSupplementStack.isEmpty()) {
                "No stack selected"
            } else {
                "${activeSupplementStack.size} · $stackNames"
            },
            detail = supplementSynergyLine(activeSupplementStack.map { it.id }.toSet()),
            action = null,
            accent = neonCyan,
            detailColor = neonMagenta,
            onClick = onNavigateToCodexSupplements
        )
        HorizontalDivider(color = neonCyan.copy(alpha = 0.2f))
        val protocolNames = activeProtocols.joinToString(", ") { protocol ->
            val paused = protocolOverrides[protocol.id]?.isPaused == true
            if (paused) "${protocol.name} (paused)" else protocol.name
        }
        val anyActiveToday = activeProtocols.any { protocolOverrides[it.id]?.isPaused != true }
        CatalogRow(
            label = "Protocols",
            value = if (activeProtocols.isEmpty()) {
                "No protocol selected"
            } else {
                "${activeProtocols.size} · $protocolNames"
            },
            detail = when {
                activeProtocols.isEmpty() -> null
                anyActiveToday -> "Active today"
                else -> "None active today"
            },
            action = null,
            accent = neonCyan,
            detailColor = if (anyActiveToday) neonMagenta else theme.inkMuted,
            onClick = onNavigateToCodexProtocols
        )
    }
}

internal fun supplementSynergyLine(stackIds: Set<String>): String? = when {
    (stackIds.contains("caffeine") || stackIds.contains("caffeine_l_theanine_stack")) &&
        stackIds.contains("l_theanine") ->
        "Caffeine + L-Theanine (smoother focus, less jitter)"
    stackIds.contains("magnesium") && stackIds.contains("glycine") ->
        "Magnesium + Glycine (sleep architecture and relaxation)"
    stackIds.contains("vitamin_d3") && stackIds.contains("vitamin_k2") ->
        "Vitamin D3 + K2 (bone matrix and calcium utilization)"
    else -> null
}

@Composable
private fun CatalogRow(
    label: String,
    value: String,
    detail: String?,
    action: String?,
    accent: Color,
    detailColor: Color,
    onClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = action ?: label, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = theme.ink,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (action != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = action,
                    color = accent,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        if (detail != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                color = detailColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
