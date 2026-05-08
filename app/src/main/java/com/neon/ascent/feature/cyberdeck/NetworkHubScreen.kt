package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.model.ChatSession
import com.neon.ascent.ui.CyberFrame
import java.util.Locale

@Composable
fun NetworkHubScreen(
    onBack: () -> Unit,
    onChessClick: () -> Unit,
    viewModel: CyberdeckViewModel,
    chatViewModel: ChatViewModel = hiltViewModel(),
    hubViewModel: NetworkHubViewModel = hiltViewModel(),
    stockViewModel: StockViewModel = hiltViewModel()
) {
    var activeTab by remember { mutableStateOf(NetworkTab.CHATS) }
    var selectedContact by remember { mutableStateOf<String?>(null) }

    if (selectedContact != null) {
        ChatDetailScreen(
            contactName = selectedContact!!,
            onBack = { selectedContact = null },
            viewModel = chatViewModel
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020508))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9F))
                }
                Text(
                    "NET // V3.0",
                    color = Color(0xFF00FF9F),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tab Bar
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                NetworkTabItem("CHATS", activeTab == NetworkTab.CHATS, Modifier.weight(1f)) { activeTab = NetworkTab.CHATS }
                NetworkTabItem("SCAN", activeTab == NetworkTab.SCAN, Modifier.weight(1f)) { activeTab = NetworkTab.SCAN }
                NetworkTabItem("BIZ", activeTab == NetworkTab.BIZ, Modifier.weight(1f)) { activeTab = NetworkTab.BIZ }
                NetworkTabItem("NODES", activeTab == NetworkTab.NODES, Modifier.weight(1f)) { activeTab = NetworkTab.NODES }
                NetworkTabItem("GAMES", activeTab == NetworkTab.GAMES, Modifier.weight(1f)) { activeTab = NetworkTab.GAMES }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area
            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                when (activeTab) {
                    NetworkTab.CHATS -> ChatListArea(chatViewModel) { selectedContact = it }
                    NetworkTab.SCAN -> NetScanArea(hubViewModel, chatViewModel)
                    NetworkTab.BIZ -> BizScreenRefined(stockViewModel)
                    NetworkTab.NODES -> CorpoNodesArea(viewModel)
                    NetworkTab.GAMES -> GamesArea(onChessClick)
                }
            }
        }
    }
}

@Composable
fun ChatListArea(viewModel: ChatViewModel, onContactClick: (String) -> Unit) {
    val sessions by viewModel.chatSessions.collectAsState()
    var lookupText by remember { mutableStateOf("") }

    Column {
        // Netrunner Lookup
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = lookupText,
                onValueChange = { lookupText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("NETRUNNER_LOOKUP...", color = Color.Gray, fontSize = 12.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            )
            IconButton(onClick = { 
                if (lookupText.isNotBlank()) {
                    viewModel.addContact(lookupText)
                    lookupText = ""
                }
            }) {
                Icon(Icons.Default.Search, contentDescription = "Add", tint = Color(0xFF00FF9F))
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sessions) { session ->
                ChatSessionItem(session) { onContactClick(session.contactName) }
            }
        }
    }
}

@Composable
fun ChatSessionItem(session: ChatSession, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    CyberFrame(
        label = session.contactName,
        borderColor = if (session.isFixer) Color(0xFFFF0088) else Color(0xFF00FF9F),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unread Indicator (Red Light)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.isUnread) Color.Red.copy(alpha = alpha) else Color.Transparent
                    )
                    .then(if (session.isUnread) Modifier.border(1.dp, Color.Red, CircleShape) else Modifier)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    session.lastMessage,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(session.lastTimestamp),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun NetScanArea(viewModel: NetworkHubViewModel, chatViewModel: ChatViewModel) {
    val state by viewModel.scanningState.collectAsState()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        CyberFrame(label = "PROXIMITY_SCANNER") {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                when (val s = state) {
                    is ScanningState.Idle -> {
                        Text("INITIATE_PROXIMITY_TRACE", color = Color.Gray)
                    }
                    is ScanningState.Scanning -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF00FF9F))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("ACQUIRING_TARGET...", color = Color(0xFF00FF9F), fontSize = 12.sp)
                        }
                    }
                    is ScanningState.Tracing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TARGET_LOCKED", color = Color.Red, fontWeight = FontWeight.Bold)
                            Text("DISTANCE: ${String.format(Locale.getDefault(), "%.1f", s.distanceMeters)}m", color = Color.White, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                            Text("PHYSICALLY_MOVE_TOWARDS_SIGNAL", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                    is ScanningState.Found -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SIGNAL_CAPTURED!", color = Color(0xFF00FF9F), fontWeight = FontWeight.Bold)
                            Text(s.name, color = Color.White, fontSize = 20.sp)
                            Button(onClick = { 
                                chatViewModel.addContact(s.name)
                                viewModel.stopScan()
                            }) {
                                Text("ADD_TO_CONTACTS")
                            }
                        }
                    }
                    is ScanningState.Error -> {
                        Text("ERROR: ${s.message}", color = Color.Red)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                if (state == ScanningState.Idle) viewModel.startScan() else viewModel.stopScan()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9F))
        ) {
            Text(if (state == ScanningState.Idle) "START_TRACE" else "ABORT_TRACE", color = Color.Black)
        }
    }
}

@Composable
fun NetworkTabItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF00FF9F) else Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) Color(0xFF00FF9F) else Color.Transparent)
        )
    }
}

@Composable
fun CorpoNodesArea(viewModel: CyberdeckViewModel) {
    val corpos = listOf(
        "ARASAKA" to "Security: HIGH | Reward: $$$",
        "MILITECH" to "Security: MED | Reward: $$",
        "KANG_TAO" to "Security: CRITICAL | Reward: $$$$",
        "BIOTECHNICA" to "Security: LOW | Reward: $"
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(corpos) { (name, desc) ->
            CyberFrame(label = name) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(desc, color = Color.Gray, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { /* TODO: Trigger Hacking Challenge */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                    ) {
                        Text("ATTACK", color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun GamesArea(onChessClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CyberFrame(label = "CYBER_CHESS_V2") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { onChessClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, tint = Color(0xFF00FF9F))
                Spacer(modifier = Modifier.width(16.dp))
                Text("ENTER_MATCHMAKING", color = Color.White)
            }
        }
    }
}

enum class NetworkTab { CHATS, SCAN, BIZ, NODES, GAMES }
