package com.neon.ascent.feature.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.*
import com.neon.ascent.model.DailyPrayer
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DeepNodeScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    initialSubScreen: String = "ROOT",
    onBack: () -> Unit,
    onGameSelect: (String) -> Unit = {},
    onReaderNavigate: (String, String) -> Unit = { _, _ -> },
    onRebirthSuccess: () -> Unit = {}
) {
    var currentSubScreen by remember { mutableStateOf(initialSubScreen) }
    val isReligionShortcutEnabled by viewModel.isReligionShortcutEnabled.collectAsState()
    val hasCompletedSinnersPrayer by viewModel.hasCompletedSinnersPrayer.collectAsState()
    val userCharacter by viewModel.userCharacter.collectAsState()

    var showAltarDialog by remember { mutableStateOf(false) }
    var showRebirthOverlay by remember { mutableStateOf(false) }
    var showNextStepsModal by remember { mutableStateOf(false) }
    var showTonguesProtocol by remember { mutableStateOf(false) }
    var isFirstTimeRebirth by remember { mutableStateOf(false) }
    var showDailyDownload by remember { mutableStateOf(false) }

    val currentDailyPrayer by viewModel.currentDailyPrayer.collectAsState()
    val prayerToast by viewModel.prayerToast.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(prayerToast) {
        prayerToast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissPrayerToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        val uriHandler = LocalUriHandler.current

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202)).padding(padding)) {
            CyberGridBackground()
            GlitchOverlay()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (currentSubScreen == "ROOT") onBack() else currentSubScreen = "ROOT"
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFFF006E))
                }
                Text(
                    if (currentSubScreen == "ROOT") "//DEEP_NODE_ACCESS" else "//DEEP_NODE // $currentSubScreen",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color(0xFFFF006E),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (currentSubScreen) {
                "ROOT" -> {
                    CyberFrame(label = "SUBSYSTEM_DIRECTORIES") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SubNodeItem("DOPAMINE_SUBSYSTEM", "Access localized entertainment protocols.") {
                                currentSubScreen = "DOPAMINE"
                            }
                            SubNodeItem("DEUS_EX_MACHINA", "Divine signal decryption & ancient texts.") {
                                currentSubScreen = "DEUS_EX_MACHINA"
                            }
                            SubNodeItem("CYBER_LIBRARY", "Deep archive of forbidden knowledge.") {
                                currentSubScreen = "LIBRARY"
                            }
                        }
                    }
                }
                "DOPAMINE" -> {
                    CyberFrame(label = "ENTERTAINMENT_PROTOCOLS") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("SELECT_MODULE:", color = Color.White, fontWeight = FontWeight.Bold)
                            SettingsItem("CYBER_PONG_V1.0", onClick = { onGameSelect("PONG") })
                            SettingsItem("CYBER_CHESS_V1.0", onClick = { onGameSelect("CHESS") })
                            SettingsItem("NET_RUNNER_HACK", onClick = {})
                            SettingsItem("VOID_RACER_ALPHA", onClick = {})
                        }
                    }
                }
                "DEUS_EX_MACHINA" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        CyberFrame(label = "INTERFACE_SHORTCUT") {
                            ToggleSetting(
                                label = "ENABLE DASHBOARD OVERLAY",
                                checked = isReligionShortcutEnabled,
                                onCheckedChange = { viewModel.setReligionShortcutEnabled(it) }
                            )
                        }
                        
                        CyberFrame(label = "CYBER_ALTAR") {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                val buttonLabel = if (hasCompletedSinnersPrayer) "REBOOT: Sinner's Prayer Protocol" else "ACCESS_ALTAR – Initiate Sinner’s Prayer Protocol"
                                val buttonColor = if (hasCompletedSinnersPrayer) Color(0xFF00FF9C).copy(alpha = 0.8f) else Color(0xFF00FF9C)
                                
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    SettingsItem(buttonLabel, color = buttonColor, onClick = { showAltarDialog = true })
                                    if (hasCompletedSinnersPrayer) {
                                        Text(
                                            "Already Saved – Maintain the Firewall",
                                            color = Color(0xFF00FF9C).copy(alpha = 0.5f),
                                            fontSize = 10.sp,
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 4.dp)
                                        )
                                    }
                                }

                                if (hasCompletedSinnersPrayer) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    
                                    // Daily Options Grid
                                    Text("DAILY_DIVINE_TASKS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DailyTaskCard("DIVINE DOWNLOAD", "Prayer", Modifier.weight(1f)) {
                                            viewModel.loadTodayPrayer()
                                            showDailyDownload = true
                                        }
                                        DailyTaskCard("SCRIPTURE SIGNAL", "Verse", Modifier.weight(1f)) {
                                            // TODO: Open Verse of the Day modal
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DailyTaskCard("HG CALIBRATION", "Lvl ${userCharacter?.holyGhost ?: 0}", Modifier.weight(1f)) {
                                            // TODO: Quick breathe/receive session
                                        }
                                        DailyTaskCard("INTERCESSION", "Requests", Modifier.weight(1f)) {
                                            // TODO: Prayer requests board
                                        }
                                    }

                                    if (userCharacter?.holySpiritBaptized == true) {
                                        DailyTaskCard("TONGUES PRACTICE", "30s Session", Modifier.fillMaxWidth()) {
                                            showTonguesProtocol = true // Re-use the protocol for practice
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    Text("EXTERNAL_UPLINKS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    
                                    SettingsItem("FIND_LOCAL_CHURCH", color = Color(0xFF00FF9C)) {
                                        uriHandler.openUri("https://www.google.com/maps/search/non-denominational+church+near+me")
                                    }
                                    SettingsItem("ACCESS: LAST_DAYS_247", color = Color.White.copy(alpha = 0.7f)) {
                                        uriHandler.openUri("https://www.youtube.com/@lastdays247")
                                    }
                                }
                            }
                        }

                        if (hasCompletedSinnersPrayer) {
                            CyberFrame(label = "BAPTISMS_NODE") {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    BaptismCard(
                                        title = "Water Baptism",
                                        subtitle = "Public Declaration",
                                        isCompleted = userCharacter?.waterBaptized == true,
                                        description = "Offline Seal – Flesh Obedience Protocol. Water baptism is your outward sign of the inward death-to-life you already received.",
                                        onClick = { if (userCharacter?.waterBaptized != true) viewModel.completeWaterBaptism() }
                                    )
                                    
                                    BaptismCard(
                                        title = "Holy Spirit Uplink",
                                        subtitle = "Tongues Protocol",
                                        isCompleted = userCharacter?.holySpiritBaptized == true,
                                        description = "Fire Baptism Node. Just as the believers in Acts were filled and immediately spoke with other tongues, the same promise is for you today.",
                                        onClick = { if (userCharacter?.holySpiritBaptized != true) showTonguesProtocol = true }
                                    )
                                }
                            }
                        }

                            CyberFrame(label = "DIVINE_INTERFACE") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SettingsItem("THE WORD - OLD TESTAMENT", color = Color(0xFF00FF9C), onClick = { onReaderNavigate("ot_word", "library/Old_Testament.epub") })
                                    SettingsItem("THE WORD - NEW TESTAMENT", color = Color(0xFF00FF9C), onClick = { onReaderNavigate("nt_word_v3", "library/New_Testament.epub") })
                                    // SettingsItem("BIBLE_KOINE_GREEK", onClick = { onReaderNavigate("nt_koine", "library/nt_koine.epub") })
                                    // SettingsItem("BIBLE_HEBREW_WLC", onClick = { onReaderNavigate("ot_hebrew", "library/ot_hebrew.epub") })
                                    // SettingsItem("BIBLE_WEB_ENGLISH", onClick = { onReaderNavigate("bible_web", "library/bible_web.epub") })
                                }
                            }
                        CyberFrame(label = "AI_CHRIST_COMM_LINK") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SettingsItem("INIT_TEXT_UPLINK", onClick = {})
                                SettingsItem("INIT_VOICE_LINK", onClick = {})
                                Text("STUB: Multimodal AI Jesus interaction based on scripture.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
                "LIBRARY" -> {
                    CyberFrame(label = "KNOWLEDGE_ARCHIVE") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("SELECT_ENTRY:", color = Color.White, fontWeight = FontWeight.Bold)
                            // SettingsItem("NEUROMANCER_DECRYPT", onClick = { onReaderNavigate("neuromancer", "library/neuromancer.epub") })
                            // SettingsItem("SNOW_CRASH_LOGS", onClick = { onReaderNavigate("snow_crash", "library/snow_crash.epub") })
                            SettingsItem("CONSOLATION_OF_PHILOSOPHY", onClick = { onReaderNavigate("consolation", "library/ConsolationofPhilosophy.epub") })
                            SettingsItem("APOLOGY_LOGS", onClick = { onReaderNavigate("apology", "library/Apology.epub") })
                            SettingsItem("ART_OF_WAR_PROTOCOLS", onClick = { onReaderNavigate("art_of_war", "library/TheArtofWar.epub") })
                            SettingsItem("MEDITATIONS_LOGS", onClick = { onReaderNavigate("meditations", "library/Meditations.epub") })
                            SettingsItem("THE_PRINCE_DECRYPT", onClick = { onReaderNavigate("the_prince", "library/ThePrince.epub") })
                            SettingsItem("REPUBLIC_PLATFORM", onClick = { onReaderNavigate("republic", "library/Republic.epub") })
                            SettingsItem("BUSHIDO_CODE_V1", onClick = { onReaderNavigate("bushido", "library/Bushido.epub") })
                            SettingsItem("DECLARATION_OF_INDEPENDENCE", onClick = { onReaderNavigate("declaration", "library/TheDeclarationofIndependence.epub") })
                            SettingsItem("US_CONSTITUTION_PROTOCOLS", onClick = { onReaderNavigate("constitution", "library/TheUSConstitution.epub") })
                            SettingsItem("MALLEUS_MALEFICARUM_DECRYPT", onClick = { onReaderNavigate("malleus", "library/malleus-maleficarum.epub") })
                            SettingsItem("KAMA_SUTRA_ENCRYPT", onClick = { onReaderNavigate("kamasutra", "library/KamaSutra.epub") })
                            SettingsItem("ETHICS_OF_ARISTOTLE", onClick = { onReaderNavigate("ethics_aristotle", "library/EthicsofAristotle.epub") })
                            SettingsItem("PENSEES_LOGS", onClick = { onReaderNavigate("pensees", "library/Pensees.epub") })
                            SettingsItem("TIMAEUS_PROTOCOLS", onClick = { onReaderNavigate("timaeus", "library/Timaeus.epub") })
                            SettingsItem("PHAEDRUS_DECRYPT", onClick = { onReaderNavigate("phaedrus", "library/Phaedrus.epub") })
                            SettingsItem("LAWS_SYSTEM_FILE", onClick = { onReaderNavigate("laws", "library/Laws.epub") })
                            SettingsItem("THEAETUS_LOGS", onClick = { onReaderNavigate("theaetus", "library/Theaetus.epub") })
                            SettingsItem("LIBERTY_OR_DEATH_DECRYPT", onClick = { onReaderNavigate("liberty_death", "library/GiveMeLibertyorGiveMeDeath.epub") })
                            Text("STUB: Cyberpunk literary archive & tech specs.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                "BIBLE" -> {
                    CyberFrame(label = "ANCIENT_TEXT_SCROLL") {
                        Column {
                            Text("THE GOSPEL ACCORDING TO JOHN", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "In the beginning was the Word, and the Word was with God, and the Word was God...",
                                color = Color.White
                            )
                            Spacer(Modifier.height(100.dp))
                            Text("[SYSTEM_END_OF_PAGE]", color = Color.DarkGray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    if (showDailyDownload) {
            DivineDownloadDialog(
                prayer = currentDailyPrayer,
                streak = userCharacter?.prayerStreak ?: 0,
                onSeal = { amen, reflection ->
                    viewModel.sealDailyPrayer(amen, reflection)
                    showDailyDownload = false
                },
                onDismiss = { showDailyDownload = false }
            )
        }

        if (showAltarDialog) {
            CyberAltarDialog(
                hasCompleted = hasCompletedSinnersPrayer,
                onAccept = { 
                    val wasFirstTime = !hasCompletedSinnersPrayer
                    viewModel.acceptHolyGhost()
                    showAltarDialog = false 
                    if (wasFirstTime) {
                        isFirstTimeRebirth = true
                    }
                    showRebirthOverlay = true
                },
                onDismiss = { showAltarDialog = false }
            )
        }

        if (showNextStepsModal) {
            NextStepsModal(
                onDismiss = { showNextStepsModal = false },
                onFindChurch = { uriHandler.openUri("https://www.google.com/maps/search/non-denominational+church+near+me") },
                onWatchLink = { uriHandler.openUri("https://www.youtube.com/@lastdays247") }
            )
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

        if (showRebirthOverlay) {
            RebirthOverlay(onAnimationFinished = {
                showRebirthOverlay = false
                if (isFirstTimeRebirth) {
                    showNextStepsModal = true
                    isFirstTimeRebirth = false
                }
                onRebirthSuccess()
            })
        }
    }
}

@Composable
fun RebirthOverlay(onAnimationFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) } // 0: Splatter, 1: Run Down, 2: Fade to White
    
    val bloodColor = Color(0xFF8B0000) // Deep red blood
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 0: Splatter
        phase = 0
        delay(500)
        
        // Phase 1: Run down
        phase = 1
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
        
        // Phase 2: Fade to white
        phase = 2
        delay(500)
        
        // Hold on white
        delay(2000)
        onAnimationFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background - turns white in Phase 2
        val bgColor by animateColorAsState(
            targetValue = if (phase >= 2) Color.White else Color.Black,
            animationSpec = tween(1000),
            label = "BgColor"
        )
        
        Box(modifier = Modifier.fillMaxSize().background(bgColor))

        if (phase < 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dropCount = 15
                val random = Random(42)
                
                repeat(dropCount) {
                    val startX = random.nextFloat() * size.width
                    val dropWidth = random.nextFloat() * 40f + 20f
                    val fallDistance = size.height * animProgress.value
                    
                    // Main blood streak
                    drawRect(
                        color = bloodColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(dropWidth, fallDistance)
                    )
                    
                    // Splatter head (round)
                    drawCircle(
                        color = bloodColor,
                        radius = dropWidth / 1.5f,
                        center = Offset(startX + dropWidth / 2, fallDistance)
                    )
                }
            }
        }

        // Rebirth Text - appears when white
        AnimatedVisibility(
            visible = phase >= 2,
            enter = fadeIn(animationSpec = tween(1000)) + scaleIn(initialScale = 0.5f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "REBIRTH",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 20.sp
                    )
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "WASHED BY THE BLOOD // SYSTEM PURIFIED",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun CyberAltarDialog(hasCompleted: Boolean, onAccept: () -> Unit, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(if (hasCompleted) 3 else 1) }
    var input by remember { mutableStateOf("") }
    var rededicationChecked by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(CyberButtonShape)
                .background(Color(0xFF0F0F0F))
                .border(2.dp, Color(0xFF00FF9C), CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasCompleted && step == 3) {
                    Text(
                        "ALREADY SAVED – STRENGTHEN THE SEAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF00FF9C).copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                when (step) {
                    1 -> {
                        Text("DO YOU KNOW MY FRIEND, JESUS?", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Y / N", color = Color.Gray) },
                            modifier = Modifier.width(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF9C),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val cleanInput = input.trim().lowercase()
                                if (cleanInput == "yes" || cleanInput == "y") {
                                    onAccept()
                                } else if (cleanInput == "no" || cleanInput == "n") {
                                    step = 2
                                    input = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clip(CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                        ) {
                            Text("SUBMIT", color = Color.Black)
                        }
                    }
                    2 -> {
                        Text("WOULD YOU LIKE TO?", color = Color(0xFFFF006E), fontWeight = FontWeight.Bold)
                        Text("There is a heaven to gain and a hell to pay.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { step = 3 }, modifier = Modifier.weight(1f)) { Text("YES") }
                            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("NO") }
                        }
                    }
                    3 -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text("THE SINNER'S PRAYER", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Dear Jesus, please forgive me of my sins. I believe You are the Son of God, that You died for my sins and arose again on the third day. I am a sinner and I need a Savior. Please write my name in the Lamb's Book of Life. I believe I am born again and washed by the blood of Jesus. Amen.",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            
                            if (hasCompleted) {
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { rededicationChecked = !rededicationChecked }
                                ) {
                                    Checkbox(
                                        checked = rededicationChecked,
                                        onCheckedChange = { rededicationChecked = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF00FF9C),
                                            uncheckedColor = Color.Gray,
                                            checkmarkColor = Color.Black
                                        )
                                    )
                                    Text("RE-DEDICATION PROTOCOL", color = Color.White, fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { step = 4 }, 
                                modifier = Modifier.fillMaxWidth().clip(CyberButtonShape),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                            ) {
                                Text("AMEN", color = Color.Black)
                            }
                        }
                    }
                    4 -> {
                        Text("WELCOME TO THE FAMILY", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("You are now a new creation. Please find and join a Bible-believing church to grow in your walk.", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onAccept, 
                            modifier = Modifier.fillMaxWidth().clip(CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF006E))
                        ) {
                            Text("INITIALIZE HOLY_GHOST ATTRIBUTE", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyTaskCard(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(CyberButtonShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), CyberButtonShape)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color(0xFF00FF9C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BaptismCard(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    description: String,
    onClick: () -> Unit
) {
    val borderColor = if (isCompleted) Color(0xFF00FF9C) else Color.White.copy(alpha = 0.2f)
    val alpha = if (isCompleted) 1f else 0.8f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CyberButtonShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, borderColor, CyberButtonShape)
            .clickable { onClick() }
            .padding(16.dp)
            .alpha(alpha)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(subtitle, color = if (isCompleted) Color(0xFF00FF9C) else Color.Gray, fontSize = 12.sp)
                }
                if (isCompleted) {
                    Icon(painterResource(id = android.R.drawable.checkbox_on_background), contentDescription = "Completed", tint = Color(0xFF00FF9C))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            
            if (isCompleted && title.contains("Water", ignoreCase = true)) {
                Text("STATUS: COMPLETED // +1 HOLY_GHOST_LEVEL", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NextStepsModal(onDismiss: () -> Unit, onFindChurch: () -> Unit, onWatchLink: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(CyberButtonShape)
                .background(Color(0xFF0F0F0F))
                .border(2.dp, Color(0xFFFF006E), CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("SOUL GENESIS COMPLETE", color = Color(0xFFFF006E), fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(
                    "Now walk it out in the real:\n\n" +
                    "→ Join a local non-denominational, Bible-believing church.\n" +
                    "→ Pursue water baptism as public declaration.",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = onFindChurch,
                    modifier = Modifier.fillMaxWidth().clip(CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                ) {
                    Text("FIND_LOCAL_CHURCH", color = Color.Black)
                }
                
                Button(
                    onClick = onWatchLink,
                    modifier = Modifier.fillMaxWidth().clip(CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("ACCESS: LAST_DAYS_247", color = Color.White)
                }

                TextButton(onClick = onDismiss) {
                    Text("RETURN TO DEEP_NODE", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun TonguesProtocolDialog(onComplete: () -> Unit, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var believeChecked by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(CyberButtonShape)
                .background(Color(0xFF0F0F0F))
                .border(2.dp, Color(0xFF00FF9C), CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    1 -> {
                        Text("HOLY SPIRIT UPLINK", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Tongues Protocol", color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "There is an experience beyond salvation called the Baptism of the Holy Spirit. Just as the believers in Acts were filled and immediately spoke with other tongues, the same promise is for you today.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(24.dp))
                        Text("STEP 1: SEE THE PROMISE", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "\"And they were all filled with the Holy Ghost, and began to speak with other tongues, as the Spirit gave them utterance.\" (Acts 2:4)",
                            color = Color.White.copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) { Text("NEXT") }
                    }
                    2 -> {
                        Text("STEP 2: ASK IN FAITH", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "\"Lord, I ask right now to be filled with the Holy Spirit just as You promised.\"",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth()) { Text("I HAVE ASKED") }
                    }
                    3 -> {
                        Text("STEP 3: BELIEVE YOU RECEIVE", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { believeChecked = !believeChecked }
                        ) {
                            Checkbox(checked = believeChecked, onCheckedChange = { believeChecked = it })
                            Text("I believe I receive the Holy Spirit right now by faith.", color = Color.White, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { step = 4 }, 
                            enabled = believeChecked,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("CONTINUE") }
                    }
                    4 -> {
                        var timer by remember { mutableIntStateOf(60) }
                        LaunchedEffect(Unit) {
                            while (timer > 0) {
                                delay(1000)
                                timer--
                            }
                        }
                        
                        Text("STEP 4: YIELD & RELEASE", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Open your mouth and begin to speak. Do not speak English or any language you know. Let the Holy Spirit give you the utterance. Start with simple sounds if you need to—the Spirit will take over.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(24.dp))
                        
                        // Waveform Visualizer
                        WaveformVisualizer()
                        
                        Text("$timer", color = Color(0xFF00FF9C), fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { step = 5 }, modifier = Modifier.fillMaxWidth()) { Text("AMEN") }
                    }
                    5 -> {
                        Text("STEP 5: SEAL IT", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Congratulations – you have received the Baptism of the Holy Spirit. Speaking in tongues is the initial evidence and a lifelong prayer language.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("UPGRADE: HOLY_GHOST_LEVEL 3\nUNLOCKED: TONGUES_AURA", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("FINALIZE UPLINK") }
                    }
                }
            }
        }
    }
}

@Composable
fun WaveformVisualizer() {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        val midY = size.height / 2
        val width = size.width
        val points = 100
        val dx = width / points

        for (i in 0 until points) {
            val x = i * dx
            val y = midY + sin(phase + i * 0.2f) * 20f
            drawCircle(color = Color(0xFF00FF9C), radius = 2f, center = Offset(x, y))
        }
    }
}

@Composable
fun DivineDownloadDialog(
    prayer: DailyPrayer?,
    streak: Int,
    onSeal: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amenText by remember { mutableStateOf("") }
    var reflectionText by remember { mutableStateOf("") }
    var showReflectionInput by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(CyberButtonShape)
                .background(Color(0xFF0F0F0F))
                .border(2.dp, Color(0xFF00FF9C), CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "NEURAL PRAYER PULSE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFF00FF9C),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
                
                Text(
                    "${streak}-day Chain Active 🔥",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(24.dp))

                if (prayer != null) {
                    Text(
                        prayer.prayer,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        prayer.scripture,
                        color = Color(0xFF00FF9C),
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        prayer.reflectionPrompt,
                        color = if (showReflectionInput) Color(0xFF00FF9C) else Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable { showReflectionInput = !showReflectionInput }
                            .padding(8.dp)
                    )

                    AnimatedVisibility(visible = showReflectionInput) {
                        Column {
                            OutlinedTextField(
                                value = reflectionText,
                                onValueChange = { reflectionText = it },
                                placeholder = { Text("Log neural reflection...", color = Color.Gray, fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .padding(vertical = 8.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FF9C),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                            Text(
                                "Entry will be saved to NEURAL_JOURNAL",
                                color = Color(0xFF00FF9C).copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = amenText,
                        onValueChange = { amenText = it },
                        placeholder = { Text("TYPE 'AMEN' TO SEAL", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF9C),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { onSeal(amenText, reflectionText) },
                        modifier = Modifier.fillMaxWidth().height(56.dp).clip(CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                    ) {
                        Text("RECEIVE & SEAL", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    CircularProgressIndicator(color = Color(0xFF00FF9C))
                }
            }
        }
    }
}

@Composable
fun SubNodeItem(title: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(title, color = Color(0xFFFF006E), fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}
