package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.lore.data.Megacorp
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.core.common.PerspectiveGrid
import com.neon.ascent.core.common.cyberGlitch
import com.neon.ascent.core.common.*

@Composable
fun MegacorpDossierScreen(
    corpId: String,
    onBack: () -> Unit,
    bypassedCorps: Set<String> = emptySet(),
    viewModel: DossierViewModel = hiltViewModel()
) {
    val megacorp by viewModel.megacorp.collectAsState()
    val trustLevel by viewModel.trustLevel.collectAsState()
    val neonTheme = LocalNeonTheme.current

    LaunchedEffect(corpId) {
        viewModel.loadCorp(corpId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020508))
    ) {
        // Scrolling Grid Background
        Box(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
            PerspectiveGrid()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = neonTheme.primary)
                }
                Column {
                    Text(
                        "// CLASSIFIED_DOSSIER",
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        megacorp?.name?.uppercase() ?: "LOADING...",
                        color = neonTheme.primary,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Trust Header
            CyberFrame(
                label = "ENCRYPTION_LEVEL",
                modifier = Modifier.padding(horizontal = 16.dp),
                borderColor = Color.Red.copy(alpha = 0.5f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "DECRYPTION_PROGRESS",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        LinearProgressIndicator(
                            progress = { trustLevel },
                            modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp),
                            color = neonTheme.accent,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "${(trustLevel * 100).toInt()}%",
                        color = neonTheme.accent,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Dossier Sections
            megacorp?.let { corp ->
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (corpId == "netwatch" && trustLevel >= 0.10f) {
                        item {
                            CyberFrame(
                                label = "NETWATCH_RUNNER_DOSSIER // CONFIDENTIAL",
                                borderColor = Color.Red
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val threatRating = when {
                                        bypassedCorps.size >= 5 -> "CRITICAL // PHANTOM"
                                        bypassedCorps.size >= 2 -> "HIGH // OPERATIVE"
                                        else -> "MODERATE // NOVICE"
                                    }
                                    
                                    Text("SUBJECT: RUNNER_ALPHA // SYNAPSE_RENEGADE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("ACTIVE_THREAT_LEVEL: $threatRating", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Text("TOTAL_SYSTEM_BREACHES: ${bypassedCorps.size}", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Text("CRACKED_NODES: ${if (bypassedCorps.isEmpty()) "NONE DETECTED" else bypassedCorps.joinToString(", ")}", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("// PREDICTIVE_WARNINGS_AND_BEHAVIOR", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    
                                    val containsMojo = bypassedCorps.any { it.contains("Mojo", ignoreCase = true) }
                                    if (!containsMojo) {
                                        Text("• Predictive analysis shows a 87% probability of attempting to interface with the rogue MojoTyger node at 3 AM.", color = Color.Yellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    } else {
                                        Text("• Warning: Subject has been in contact with MojoTyger Core. Expect high-energy chaotic subversion.", color = Color.Yellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Text("• Subject has collected ${bypassedCorps.size} high-tier decryption signatures. Recommend deployment of level-4 black ICE immediately.", color = Color.Yellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // 1. CEO Psych Profile (10%)
                    item {
                        DossierSection(
                            title = "CEO_PSYCH_PROFILE",
                            gate = 0.10f,
                            currentTrust = trustLevel,
                            content = corp.dossier?.psychProfile ?: "NO_DATA",
                            accentColor = neonTheme.primary
                        )
                    }

                    // 2. Known Attack Vectors (30%)
                    item {
                        DossierSection(
                            title = "ATTACK_VECTORS",
                            gate = 0.30f,
                            currentTrust = trustLevel,
                            content = corp.dossier?.attackVectors ?: "NO_DATA",
                            accentColor = neonTheme.accent
                        )
                    }

                    // 3. Leaked Memos (45%)
                    item {
                        DossierSectionList(
                            title = "LEAKED_INTERNAL_MEMOS",
                            gate = 0.45f,
                            currentTrust = trustLevel,
                            items = corp.dossier?.leakedMemos ?: emptyList(),
                            accentColor = Color.Yellow
                        )
                    }

                    // 4. Rival Shade (60%)
                    item {
                        DossierSectionList(
                            title = "RIVAL_CORP_INTEL",
                            gate = 0.60f,
                            currentTrust = trustLevel,
                            items = corp.dossier?.rivalShade?.map { "[${it.targetCorpoId.uppercase()}] ${it.intel}" } ?: emptyList(),
                            accentColor = Color.Magenta
                        )
                    }

                    // 5. Quickhack Vault (75%)
                    item {
                        DossierVaultSection(
                            title = "QUICKHACK_VAULT",
                            gate = 0.75f,
                            currentTrust = trustLevel,
                            oneTimeEddies = corp.dossier?.oneTimeEddies,
                            permanentPerk = corp.dossier?.permanentPerk,
                            iceBlueprints = corp.dossier?.iceBlueprints ?: emptyList(),
                            rewards = corp.dossier?.quickhackVault ?: emptyList(),
                            onClaim = { viewModel.claimReward(it) }
                        )
                    }

                    // 6. Blackmail Material (85%)
                    item {
                        DossierSectionList(
                            title = "LEVERAGE_MATERIAL",
                            gate = 0.85f,
                            currentTrust = trustLevel,
                            items = corp.dossier?.blackmail ?: emptyList(),
                            accentColor = Color.Red
                        )
                    }

                    // 7. Executive Backdoor (95%)
                    item {
                        DossierSection(
                            title = "EXECUTIVE_BACKDOOR",
                            gate = 0.95f,
                            currentTrust = trustLevel,
                            content = corp.dossier?.executiveBackdoor ?: "NO_DATA",
                            accentColor = Color.White,
                            isGlitchy = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DossierSection(
    title: String,
    gate: Float,
    currentTrust: Float,
    content: String,
    accentColor: Color,
    isGlitchy: Boolean = false
) {
    val isUnlocked = currentTrust >= gate
    val blurAmount by animateDpAsState(if (isUnlocked) 0.dp else 12.dp, label = "Blur")

    CyberFrame(
        label = if (isUnlocked) title else "LOCKED // GATE_${(gate * 100).toInt()}%",
        borderColor = if (isUnlocked) accentColor.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.3f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .blur(blurAmount)
                    .then(if (isGlitchy && isUnlocked) Modifier.cyberGlitch(0.1f) else Modifier)
            ) {
                if (isUnlocked) {
                    Text(
                        content,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                } else {
                    repeat(3) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .padding(vertical = 4.dp)
                                .background(Color.Gray.copy(alpha = 0.2f))
                        )
                    }
                }
            }
            
            if (!isUnlocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun DossierSectionList(
    title: String,
    gate: Float,
    currentTrust: Float,
    items: List<String>,
    accentColor: Color
) {
    val isUnlocked = currentTrust >= gate
    
    CyberFrame(
        label = if (isUnlocked) title else "LOCKED // GATE_${(gate * 100).toInt()}%",
        borderColor = if (isUnlocked) accentColor.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.3f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isUnlocked) {
                items.forEach { item ->
                    Row {
                        Text("// ", color = accentColor, fontWeight = FontWeight.Bold)
                        Text(
                            item,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                Text(
                    "UNAUTHORIZED_ACCESS_DENIED",
                    color = Color.Red.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun DossierVaultSection(
    title: String,
    gate: Float,
    currentTrust: Float,
    oneTimeEddies: Int? = null,
    permanentPerk: String? = null,
    iceBlueprints: List<String> = emptyList(),
    rewards: List<com.neon.ascent.core.lore.data.QuickhackReward>,
    onClaim: (com.neon.ascent.core.lore.data.QuickhackReward) -> Unit
) {
    val isUnlocked = currentTrust >= gate
    
    CyberFrame(
        label = if (isUnlocked) title else "LOCKED // GATE_${(gate * 100).toInt()}%",
        borderColor = if (isUnlocked) Color(0xFF00FF9F) else Color.Gray.copy(alpha = 0.3f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isUnlocked) {
                // One-time eddies
                oneTimeEddies?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("§", color = Color(0xFF00FF9F), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("LEAKED_FUNDS: §$it", color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }

                // Permanent Perk
                permanentPerk?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, null, tint = Color.Magenta, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PERMANENT_PERK: $it", color = Color.Magenta, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // ICE Blueprints
                if (iceBlueprints.isNotEmpty()) {
                    Text("// ICE_BLUEPRINTS", color = Color.Gray, fontSize = 10.sp)
                    iceBlueprints.forEach { blueprint ->
                        Text("• $blueprint", color = Color.Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                rewards.forEach { reward ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF00FF9F).copy(alpha = 0.3f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Terminal, null, tint = Color(0xFF00FF9F))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(reward.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(reward.description ?: "", color = Color.Gray, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { onClaim(reward) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9F).copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, Color(0xFF00FF9F))
                        ) {
                            Text("DOWNLOAD", color = Color(0xFF00FF9F), fontSize = 10.sp)
                        }
                    }
                }
            } else {
                Text("VAULT_SEALED", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
