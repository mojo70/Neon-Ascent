package com.neon.ascent.feature.biohacking.ui.labs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.domain.labs.ReconstitutionCalculator
import com.neon.ascent.core.domain.labs.UnitConverterCalculator
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.GlitchText
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    cyan: Color = Color(0xFF00F5FF),
    magenta: Color = Color(0xFFFF0088),
    darkBg: Color = Color(0xFF0A0F14)
) {
    var selectedProfile by remember { mutableStateOf(UnitConverterCalculator.CompoundProfile.GENERAL) }

    // mcg <-> mg states
    var mcgInput by remember { mutableStateOf("250") }
    var mgInput by remember { mutableStateOf("0.25") }

    // IU <-> mg states
    var iuInput by remember { mutableStateOf("3.0") }
    var iuMgInput by remember { mutableStateOf("1.0") }
    var customRatioInput by remember { mutableStateOf("3.0") }

    val mcgVal = ReconstitutionCalculator.parseBigDecimal(mcgInput)
    val mgVal = ReconstitutionCalculator.parseBigDecimal(mgInput)

    val iuVal = ReconstitutionCalculator.parseBigDecimal(iuInput)
    val iuMgVal = ReconstitutionCalculator.parseBigDecimal(iuMgInput)

    val effectiveRatio = when (selectedProfile) {
        UnitConverterCalculator.CompoundProfile.HGH_SOMATROPIN -> UnitConverterCalculator.HGH_IU_PER_MG
        UnitConverterCalculator.CompoundProfile.HCG -> ReconstitutionCalculator.parseBigDecimal(customRatioInput)
        UnitConverterCalculator.CompoundProfile.GENERAL -> ReconstitutionCalculator.parseBigDecimal(customRatioInput)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GlitchText(
                        text = "UNIT_CONVERTER",
                        color = cyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = cyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        },
        containerColor = darkBg
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Disclaimer Banner
            CyberFrame(label = "DISCLAIMER // RESEARCH_ONLY", borderColor = magenta.copy(alpha = 0.8f)) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Warning",
                        tint = magenta,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Math reference tool for personal/research reference only. Not medical advice or a dosing recommendation.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }

            // Compound Profile Selector
            CyberFrame(label = "COMPOUND_PROFILE", borderColor = cyan) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UnitConverterCalculator.CompoundProfile.entries.forEach { prof ->
                        val isSelected = prof == selectedProfile
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedProfile = prof },
                            label = { Text(prof.displayName, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cyan.copy(alpha = 0.25f),
                                selectedLabelColor = cyan
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ℹ️ ${selectedProfile.note}",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Universal mcg ↔ mg Converter
            CyberFrame(label = "MASS_CONVERSION // MCG ↔ MG", borderColor = cyan) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("MICROGRAMS (mcg)", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = mcgInput,
                                onValueChange = {
                                    mcgInput = it
                                    val parsed = ReconstitutionCalculator.parseBigDecimal(it)
                                    if (parsed != null) {
                                        mgInput = UnitConverterCalculator.mcgToMg(parsed).toPlainString()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.DarkGray)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("MILLIGRAMS (mg)", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = mgInput,
                                onValueChange = {
                                    mgInput = it
                                    val parsed = ReconstitutionCalculator.parseBigDecimal(it)
                                    if (parsed != null) {
                                        mcgInput = UnitConverterCalculator.mgToMcg(parsed).toPlainString()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.DarkGray)
                            )
                        }
                    }

                    if (mcgVal != null && mgVal != null) {
                        ResultRow("EQUIVALENCE", "${mcgVal.toPlainString()} mcg == ${mgVal.toPlainString()} mg", cyan, isHighlight = true)
                    }
                }
            }

            // Documented IU ↔ mg Converter Section
            CyberFrame(label = "INTERNATIONAL_UNITS // IU ↔ MG", borderColor = cyan) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedProfile == UnitConverterCalculator.CompoundProfile.HCG && effectiveRatio == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(magenta.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "HCG is natively sold and dosed in International Units (IU). No default mg conversion is applied. Supply a custom IU/mg ratio below if required.",
                                color = magenta,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (selectedProfile == UnitConverterCalculator.CompoundProfile.GENERAL || selectedProfile == UnitConverterCalculator.CompoundProfile.HCG) {
                        Column {
                            Text("CUSTOM_IU_PER_MG_RATIO", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customRatioInput,
                                onValueChange = { customRatioInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.DarkGray)
                            )
                        }
                    }

                    if (effectiveRatio != null && effectiveRatio > BigDecimal.ZERO) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("UNITS (IU)", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = iuInput,
                                    onValueChange = {
                                        iuInput = it
                                        val parsed = ReconstitutionCalculator.parseBigDecimal(it)
                                        if (parsed != null) {
                                            val convertedMg = UnitConverterCalculator.iuToMg(parsed, effectiveRatio)
                                            if (convertedMg != null) iuMgInput = convertedMg.toPlainString()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.DarkGray)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("MASS (mg)", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = iuMgInput,
                                    onValueChange = {
                                        iuMgInput = it
                                        val parsed = ReconstitutionCalculator.parseBigDecimal(it)
                                        if (parsed != null) {
                                            val convertedIu = UnitConverterCalculator.mgToIu(parsed, effectiveRatio)
                                            if (convertedIu != null) iuInput = convertedIu.toPlainString()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.DarkGray)
                                )
                            }
                        }

                        if (iuVal != null && iuMgVal != null) {
                            ResultRow("IU_CONVERSION", "${iuVal.toPlainString()} IU ≈ ${iuMgVal.toPlainString()} mg (@ ${effectiveRatio.toPlainString()} IU/mg)", cyan, isHighlight = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, color: Color, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlight) color.copy(alpha = 0.15f) else Color.Transparent)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (isHighlight) color else Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = if (isHighlight) color else Color.White, fontSize = if (isHighlight) 13.sp else 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
