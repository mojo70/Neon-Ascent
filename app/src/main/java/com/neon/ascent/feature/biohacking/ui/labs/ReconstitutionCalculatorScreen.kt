package com.neon.ascent.feature.biohacking.ui.labs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.data.local.entity.SavedReconstitutionEntity
import com.neon.ascent.core.domain.labs.ReconstitutionCalculator
import com.neon.ascent.ui.CyberActionButton
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.GlitchText
import java.math.BigDecimal
import java.math.RoundingMode

enum class CalculatorMode(val label: String) {
    FORWARD("FORWARD"),
    REVERSE_WATER("REVERSE WATER"),
    REVERSE_DOSE("REVERSE DOSE")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconstitutionCalculatorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReconstitutionViewModel = hiltViewModel(),
    cyan: Color = Color(0xFF00F5FF),
    magenta: Color = Color(0xFFFF0088),
    darkBg: Color = Color(0xFF0A0F14)
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val savedMixes by viewModel.savedMixes.collectAsState()

    var mode by remember { mutableStateOf(CalculatorMode.FORWARD) }

    // Inputs
    var vialMgInput by remember { mutableStateOf("10") }
    var waterMlInput by remember { mutableStateOf("2") }
    
    // Dose Input & Unit
    var doseInput by remember { mutableStateOf("0.5") }
    var doseInMcg by remember { mutableStateOf(false) }

    // Reverse Mode Specific Inputs
    var targetUnitsInput by remember { mutableStateOf("10") }
    var unitsDrawnInput by remember { mutableStateOf("10") }

    // Syringe Barrel
    var selectedBarrel by remember { mutableStateOf(ReconstitutionCalculator.SyringeBarrel.BARREL_1_0) }

    // Active Blend Components (if pre-filled from saved mix or entered)
    var activeComponents by remember { mutableStateOf<List<Pair<String, BigDecimal>>>(emptyList()) }

    // Formula Expansion & Save Dialog Toggles
    var showFormulas by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Dialog Input States
    var saveName by remember { mutableStateOf("") }
    var saveDate by remember { mutableStateOf("") }
    var saveNotes by remember { mutableStateOf("") }
    var saveComponentsRaw by remember { mutableStateOf("") } // e.g. "BPC-157: 5, TB-500: 5"

    // Calculated Parsed BigDecimal Values
    val vialMg = remember(vialMgInput) { ReconstitutionCalculator.parseBigDecimal(vialMgInput) }
    val waterMl = remember(waterMlInput) { ReconstitutionCalculator.parseBigDecimal(waterMlInput) }
    
    val doseRaw = remember(doseInput) { ReconstitutionCalculator.parseBigDecimal(doseInput) }
    val doseMg = remember(doseRaw, doseInMcg) {
        if (doseRaw == null) null
        else if (doseInMcg) ReconstitutionCalculator.mcgToMg(doseRaw)
        else doseRaw
    }
    val doseMcg = remember(doseRaw, doseInMcg) {
        if (doseRaw == null) null
        else if (doseInMcg) doseRaw
        else ReconstitutionCalculator.mgToMcg(doseRaw)
    }

    val targetUnits = remember(targetUnitsInput) { ReconstitutionCalculator.parseBigDecimal(targetUnitsInput) }
    val unitsDrawn = remember(unitsDrawnInput) { ReconstitutionCalculator.parseBigDecimal(unitsDrawnInput) }

    // Calculation Results
    val forwardResult = remember(vialMg, waterMl, doseMg) {
        if (vialMg != null && waterMl != null && doseMg != null) {
            ReconstitutionCalculator.forward(vialMg, waterMl, doseMg)
        } else null
    }

    val reverseWaterResultMl = remember(vialMg, doseMg, targetUnits) {
        if (vialMg != null && doseMg != null && targetUnits != null) {
            ReconstitutionCalculator.reverseWater(vialMg, doseMg, targetUnits)
        } else null
    }

    val reverseDoseResultMg = remember(vialMg, waterMl, unitsDrawn) {
        if (vialMg != null && waterMl != null && unitsDrawn != null) {
            val conc = vialMg.divide(waterMl, 10, RoundingMode.HALF_UP)
            ReconstitutionCalculator.reverseDose(conc, unitsDrawn)
        } else null
    }

    val activeUnits = when (mode) {
        CalculatorMode.FORWARD -> forwardResult?.unitsU100
        CalculatorMode.REVERSE_WATER -> targetUnits
        CalculatorMode.REVERSE_DOSE -> unitsDrawn
    }

    val effectiveDoseMg = when (mode) {
        CalculatorMode.FORWARD -> doseMg
        CalculatorMode.REVERSE_WATER -> doseMg
        CalculatorMode.REVERSE_DOSE -> reverseDoseResultMg
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GlitchText(
                        text = "RECONSTITUTION_CALCULATOR",
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
                actions = {
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Mix", tint = cyan)
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

            // Mode Selector Segmented Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorMode.entries.forEach { m ->
                    val isSelected = m == mode
                    Button(
                        onClick = { mode = m },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) cyan.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (isSelected) cyan else Color.Gray
                        ),
                        border = BorderStroke(1.dp, if (isSelected) cyan else Color.DarkGray),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = m.label,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Inputs Section
            CyberFrame(label = "INPUT_PARAMETERS", borderColor = cyan) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Vial Quantity (mg)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "VIAL_QUANTITY (mg)",
                                color = cyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = vialMgInput,
                            onValueChange = { vialMgInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = cyan,
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Vial Chips (2, 5, 10, 15, 20)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf("2", "5", "10", "15", "20").forEach { chipVal ->
                                FilterChip(
                                    selected = vialMgInput == chipVal,
                                    onClick = { vialMgInput = chipVal },
                                    label = { Text("${chipVal}mg", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = cyan.copy(alpha = 0.25f),
                                        selectedLabelColor = cyan
                                    )
                                )
                            }
                        }
                    }

                    // Water Volume (mL)
                    Column {
                        Text(
                            "BAC_WATER_VOLUME (mL)",
                            color = cyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = waterMlInput,
                            onValueChange = { waterMlInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = cyan,
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Water Chips (1, 2, 2.5, 3)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf("1", "2", "2.5", "3").forEach { chipVal ->
                                FilterChip(
                                    selected = waterMlInput == chipVal,
                                    onClick = { waterMlInput = chipVal },
                                    label = { Text("${chipVal}mL", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = cyan.copy(alpha = 0.25f),
                                        selectedLabelColor = cyan
                                    )
                                )
                            }
                        }
                    }

                    // Dose Input & Units (For Forward & Reverse Water)
                    if (mode == CalculatorMode.FORWARD || mode == CalculatorMode.REVERSE_WATER) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "TARGET_DOSE",
                                    color = cyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                if (doseMg != null && doseMcg != null) {
                                    Text(
                                        text = "${doseMg.toPlainString()} mg (${doseMcg.toPlainString()} mcg)",
                                        color = cyan.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = doseInput,
                                    onValueChange = { doseInput = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = cyan,
                                        unfocusedBorderColor = Color.DarkGray
                                    )
                                )

                                Row(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .border(1.dp, cyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                ) {
                                    Button(
                                        onClick = { doseInMcg = false },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!doseInMcg) cyan.copy(alpha = 0.3f) else Color.Transparent,
                                            contentColor = if (!doseInMcg) cyan else Color.Gray
                                        ),
                                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                                    ) {
                                        Text("mg", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Button(
                                        onClick = { doseInMcg = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (doseInMcg) cyan.copy(alpha = 0.3f) else Color.Transparent,
                                            contentColor = if (doseInMcg) cyan else Color.Gray
                                        ),
                                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                                    ) {
                                        Text("mcg", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }

                    // Mode Specific Additional Inputs
                    if (mode == CalculatorMode.REVERSE_WATER) {
                        Column {
                            Text(
                                "TARGET_U100_UNITS",
                                color = cyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = targetUnitsInput,
                                onValueChange = { targetUnitsInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = cyan,
                                    unfocusedBorderColor = Color.DarkGray
                                )
                            )
                        }
                    }

                    if (mode == CalculatorMode.REVERSE_DOSE) {
                        Column {
                            Text(
                                "UNITS_DRAWN_ON_SYRINGE",
                                color = cyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = unitsDrawnInput,
                                onValueChange = { unitsDrawnInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = cyan,
                                    unfocusedBorderColor = Color.DarkGray
                                )
                            )
                        }
                    }

                    // Syringe Barrel Selector
                    Column {
                        Text(
                            "SYRINGE_BARREL_CAPACITY",
                            color = cyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReconstitutionCalculator.SyringeBarrel.entries.forEach { b ->
                                val isSelected = b == selectedBarrel
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedBarrel = b },
                                    label = { Text(b.label, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = cyan.copy(alpha = 0.25f),
                                        selectedLabelColor = cyan
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Results Section
            CyberFrame(label = "CALCULATED_OUTPUTS", borderColor = cyan) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (mode) {
                        CalculatorMode.FORWARD -> {
                            if (forwardResult != null) {
                                ResultRow("CONCENTRATION", "${forwardResult.concentrationMgPerMl.toPlainString()} mg/mL (${forwardResult.concentrationMcgPerMl.toPlainString()} mcg/mL)", cyan)
                                ResultRow("VOLUME_PER_DOSE", "${forwardResult.volumeMl.toPlainString()} mL", cyan)
                                ResultRow("U-100_SYRINGE_MARK", "${forwardResult.unitsU100.toPlainString()} units", cyan, isHighlight = true)
                                ResultRow("MCG_PER_UNIT", "${forwardResult.mcgPerUnit.toPlainString()} mcg/unit", cyan)
                                ResultRow("DOSES_PER_VIAL", "${forwardResult.dosesPerVial.toPlainString()} doses", cyan)
                            } else {
                                EmptyOutputState()
                            }
                        }
                        CalculatorMode.REVERSE_WATER -> {
                            if (reverseWaterResultMl != null) {
                                ResultRow("WATER_TO_ADD", "${reverseWaterResultMl.toPlainString()} mL", cyan, isHighlight = true)
                                if (vialMg != null && doseMg != null) {
                                    val conc = vialMg.divide(reverseWaterResultMl, 10, RoundingMode.HALF_UP)
                                    val concMcg = conc.multiply(BigDecimal("1000"))
                                    ResultRow("RESULTING_CONCENTRATION", "${conc.stripTrailingZeros().toPlainString()} mg/mL (${concMcg.stripTrailingZeros().toPlainString()} mcg/mL)", cyan)
                                }
                            } else {
                                EmptyOutputState()
                            }
                        }
                        CalculatorMode.REVERSE_DOSE -> {
                            if (reverseDoseResultMg != null) {
                                val doseMcgVal = ReconstitutionCalculator.mgToMcg(reverseDoseResultMg)
                                ResultRow("DELIVERED_DOSE", "${reverseDoseResultMg.toPlainString()} mg (${doseMcgVal.toPlainString()} mcg)", cyan, isHighlight = true)
                                if (vialMg != null && waterMl != null) {
                                    val conc = vialMg.divide(waterMl, 10, RoundingMode.HALF_UP)
                                    ResultRow("CONCENTRATION", "${conc.stripTrailingZeros().toPlainString()} mg/mL", cyan)
                                }
                            } else {
                                EmptyOutputState()
                            }
                        }
                    }

                    // Optional Blend Component Breakdown Display
                    if (activeComponents.isNotEmpty() && effectiveDoseMg != null && vialMg != null && vialMg > BigDecimal.ZERO) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(1.dp, cyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "BLEND_COMPONENT_CONTRIBUTION (At Drawn Dose):",
                                    color = cyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                activeComponents.forEach { (compName, compMg) ->
                                    val ratio = compMg.divide(vialMg, 10, RoundingMode.HALF_UP)
                                    val compDoseMg = effectiveDoseMg.multiply(ratio).stripTrailingZeros()
                                    val compDoseMcg = ReconstitutionCalculator.mgToMcg(compDoseMg)
                                    Text(
                                        "• $compName: ${compDoseMg.toPlainString()} mg (${compDoseMcg.toPlainString()} mcg)",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Visual Syringe Display
                    if (activeUnits != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        VisualSyringe(units = activeUnits, barrel = selectedBarrel, cyan = cyan, magenta = magenta)
                    }

                    // Overflow Warning
                    if (activeUnits != null && selectedBarrel.isOverflow(activeUnits)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(magenta.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, magenta, RoundedCornerShape(4.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "⚠️ OVERFLOW WARNING: ${activeUnits.toPlainString()} units exceeds selected barrel capacity (${selectedBarrel.label}). Use a larger syringe or increase vial concentration.",
                                color = magenta,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Easy-Read Hint Callout
                    if (activeUnits != null) {
                        val unitsVal = activeUnits.toFloat()
                        if (unitsVal > 0f && unitsVal < 5f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .border(1.dp, cyan, RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "💡 EASY-READ HINT: Draw volume is small (<5 u). Consider using more water (e.g., 3 mL) for higher measurement precision.",
                                    color = cyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else if (unitsVal > 50f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .border(1.dp, cyan, RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "💡 EASY-READ HINT: Draw volume is large (>50 u). Consider using less water (e.g., 1 mL) to reduce injection volume.",
                                    color = cyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Dead-space Callout
                    Text(
                        text = "ℹ️ DEAD SPACE NOTE: Standard U-100 insulin syringes retain ~1–2 units in needle dead space. This is fixed retained volume and not subtracted from dosage math.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Copy Summary & Save Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberActionButton(
                    label = "COPY_SUMMARY",
                    color = cyan,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val summary = buildSummaryString(mode, vialMgInput, waterMlInput, doseInput, doseInMcg, targetUnitsInput, unitsDrawnInput, forwardResult, reverseWaterResultMl, reverseDoseResultMg)
                        clipboardManager.setText(AnnotatedString(summary))
                        Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                CyberActionButton(
                    label = "SAVE_THIS_MIX",
                    color = magenta,
                    modifier = Modifier.weight(1f),
                    onClick = { showSaveDialog = true }
                )
            }

            // Saved Reconstitutions Section
            CyberFrame(label = "SAVED_RECONSTITUTIONS", borderColor = cyan) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (savedMixes.isEmpty()) {
                        Text(
                            text = "NO SAVED MIXES. TAP 'SAVE_THIS_MIX' OR THE BOOKMARK ICON TO PERSIST RECONSTITUTIONS LOCALLY.",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        savedMixes.forEach { mix ->
                            SavedMixItemCard(
                                mix = mix,
                                cyan = cyan,
                                magenta = magenta,
                                onSelect = {
                                    vialMgInput = mix.vialMg
                                    waterMlInput = mix.waterMl
                                    activeComponents = parseComponentsString(mix.componentsJson)
                                    Toast.makeText(context, "Pre-filled: ${mix.name}", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    viewModel.deleteMix(mix.id)
                                    Toast.makeText(context, "Deleted mix: ${mix.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // "How this was calculated" Expansion
            CyberFrame(label = "FORMULA_SPECIFICATIONS", borderColor = cyan) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFormulas = !showFormulas },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HOW_THIS_WAS_CALCULATED",
                            color = cyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (showFormulas) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Formulas",
                            tint = cyan
                        )
                    }

                    AnimatedVisibility(visible = showFormulas) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Formulas:", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("• Concentration (mg/mL) = Vial mg / Water mL", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("• Volume (mL) = Target Dose mg / Concentration (mg/mL)", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("• U-100 Units = Volume (mL) × 100", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("• Doses per Vial = Vial mg / Target Dose mg", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("• Reverse Water (mL) = (Vial mg × Target Units) / (Target Dose mg × 100)", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("• Reverse Dose (mg) = Concentration (mg/mL) × (Units Drawn / 100)", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // Save Mix Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    "SAVE_RECONSTITUTION_MIX",
                    color = cyan,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        label = { Text("Mix Name (e.g. BPC-157 10mg)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = saveDate,
                        onValueChange = { saveDate = it },
                        label = { Text("Mixed Date (Optional e.g. 2026-10-11)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = saveNotes,
                        onValueChange = { saveNotes = it },
                        label = { Text("Notes / Protocol Details (Optional)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = saveComponentsRaw,
                        onValueChange = { saveComponentsRaw = it },
                        label = { Text("Blend Components (e.g. BPC-157:5, TB-500:5)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val concVal = if (vialMg != null && waterMl != null) {
                            vialMg.divide(waterMl, 4, RoundingMode.HALF_UP).toPlainString()
                        } else "0"

                        val parsedCompList = parseRawComponentsInput(saveComponentsRaw)
                        val formattedCompJson = if (parsedCompList.isNotEmpty()) formatComponentsString(parsedCompList) else null

                        viewModel.saveMix(
                            name = saveName,
                            vialMg = vialMgInput,
                            waterMl = waterMlInput,
                            concentrationMgPerMl = concVal,
                            mixedDate = saveDate,
                            notes = saveNotes,
                            componentsJson = formattedCompJson
                        )

                        if (parsedCompList.isNotEmpty()) {
                            activeComponents = parsedCompList
                        }

                        showSaveDialog = false
                        saveName = ""
                        saveDate = ""
                        saveNotes = ""
                        saveComponentsRaw = ""
                        Toast.makeText(context, "Reconstitution mix saved locally", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cyan)
                ) {
                    Text("SAVE", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = darkBg
        )
    }
}

@Composable
private fun SavedMixItemCard(
    mix: SavedReconstitutionEntity,
    cyan: Color,
    magenta: Color,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val components = remember(mix.componentsJson) { parseComponentsString(mix.componentsJson) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cyan.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
            .border(1.dp, cyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .clickable { onSelect() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mix.name,
                    color = cyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Mix",
                        tint = magenta,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "${mix.vialMg} mg + ${mix.waterMl} mL BAC Water → ${mix.concentrationMgPerMl} mg/mL",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            if (!mix.mixedDate.isNullOrBlank()) {
                Text(
                    text = "Mixed: ${mix.mixedDate}",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (!mix.notes.isNullOrBlank()) {
                Text(
                    text = "Notes: ${mix.notes}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (components.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Blend Components: " + components.joinToString(", ") { "${it.first} (${it.second.toPlainString()}mg)" },
                    color = cyan.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    color: Color,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlight) color.copy(alpha = 0.15f) else Color.Transparent)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isHighlight) color else Color.LightGray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            color = if (isHighlight) color else Color.White,
            fontSize = if (isHighlight) 13.sp else 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyOutputState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ENTER VALID INPUTS TO COMPUTE RECONSTITUTION",
            color = Color.Gray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatComponentsString(components: List<Pair<String, BigDecimal>>): String {
    return components.joinToString("|") { "${it.first}:${it.second.toPlainString()}" }
}

private fun parseComponentsString(raw: String?): List<Pair<String, BigDecimal>> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split("|").mapNotNull { item ->
        val parts = item.split(":")
        if (parts.size == 2) {
            val name = parts[0].trim()
            val mg = ReconstitutionCalculator.parseBigDecimal(parts[1])
            if (name.isNotEmpty() && mg != null) name to mg else null
        } else null
    }
}

private fun parseRawComponentsInput(raw: String?): List<Pair<String, BigDecimal>> {
    if (raw.isNullOrBlank()) return emptyList()
    // Supports comma or pipe separated e.g. "BPC-157: 5, TB-500: 5"
    val delimiter = if (raw.contains("|")) "|" else ","
    return raw.split(delimiter).mapNotNull { item ->
        val parts = item.split(":")
        if (parts.size == 2) {
            val name = parts[0].trim()
            val mg = ReconstitutionCalculator.parseBigDecimal(parts[1])
            if (name.isNotEmpty() && mg != null) name to mg else null
        } else null
    }
}

private fun buildSummaryString(
    mode: CalculatorMode,
    vialMg: String,
    waterMl: String,
    dose: String,
    doseInMcg: Boolean,
    targetUnits: String,
    unitsDrawn: String,
    forwardResult: ReconstitutionCalculator.ReconstitutionResult?,
    reverseWaterResultMl: BigDecimal?,
    reverseDoseResultMg: BigDecimal?
): String {
    return when (mode) {
        CalculatorMode.FORWARD -> {
            if (forwardResult != null) {
                "${vialMg} mg + ${waterMl} mL → ${forwardResult.concentrationMgPerMl.toPlainString()} mg/mL · ${forwardResult.doseMg.toPlainString()} mg (${forwardResult.doseMcg.toPlainString()} mcg) = ${forwardResult.unitsU100.toPlainString()} units"
            } else "Incomplete Reconstitution Calculation"
        }
        CalculatorMode.REVERSE_WATER -> {
            if (reverseWaterResultMl != null) {
                "${vialMg} mg vial · ${dose} ${if (doseInMcg) "mcg" else "mg"} dose @ ${targetUnits} units → Add ${reverseWaterResultMl.toPlainString()} mL water"
            } else "Incomplete Reverse Water Calculation"
        }
        CalculatorMode.REVERSE_DOSE -> {
            if (reverseDoseResultMg != null) {
                val mcgVal = ReconstitutionCalculator.mgToMcg(reverseDoseResultMg)
                "${vialMg} mg + ${waterMl} mL · ${unitsDrawn} units drawn → ${reverseDoseResultMg.toPlainString()} mg (${mcgVal.toPlainString()} mcg) delivered"
            } else "Incomplete Reverse Dose Calculation"
        }
    }
}
