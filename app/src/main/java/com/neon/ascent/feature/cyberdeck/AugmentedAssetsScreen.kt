package com.neon.ascent.feature.cyberdeck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
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
import com.neon.ascent.model.AssetAccount
import com.neon.ascent.model.AssetType
import com.neon.ascent.ui.CyberFrame
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AugmentedAssetsScreen(
    viewModel: NetWorthViewModel,
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val summary by viewModel.summary.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // ... (Header code was here)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9F))
                }
                Text(
                    "AUGMENTED_ASSETS // TERMINAL",
                    color = Color(0xFF00FF9F),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { showChat = true }) {
                Icon(Icons.Default.Chat, contentDescription = "AI Advice", tint = Color(0xFF00CCFF))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (accounts.isEmpty()) {
            AssetOnboardingView { showAddDialog = true }
        } else {
            AssetDashboardView(summary, accounts, onAdd = { showAddDialog = true })
        }
    }

    if (showAddDialog) {
        AddAssetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, inst, type, balance ->
                viewModel.addManualAccount(name, inst, type, balance)
                showAddDialog = false
            }
        )
    }

    if (showChat) {
        ModalBottomSheet(onDismissRequest = { showChat = false }) {
            FinancialAiChatView(summary, accounts)
        }
    }
}

@Composable
fun FinancialAiChatView(
    summary: com.neon.ascent.model.NetWorthSummary,
    accounts: List<AssetAccount>
) {
    var message by remember { mutableStateOf("") }
    val chatHistory = remember { mutableStateListOf<Pair<String, Boolean>>() } // Text, IsUser

    Column(modifier = Modifier.fillMaxHeight(0.8f).padding(16.dp)) {
        Text("NEURAL_FINANCE_ADVISOR", color = Color(0xFF00CCFF), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    "Greetings, Operative. I have analyzed your Augmented Assets. Your liquid integrity is at ${String.format(Locale.getDefault(), "%,.0f", summary.liquidNetWorth)}. How can I optimize your matrix standing?",
                    color = Color(0xFF00CCFF).copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            items(chatHistory) { (txt, isUser) ->
                Text(
                    if (isUser) "> $txt" else txt,
                    color = if (isUser) Color.White else Color(0xFF00CCFF),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Query AI...", color = Color.Gray) }
            )
            IconButton(onClick = {
                if (message.isNotBlank()) {
                    chatHistory.add(message to true)
                    // In a real app, call AI here
                    chatHistory.add("Analyzing allocation patterns... Recommendation: Increase exposure to decentralized nodes to hedge against corpo volatility." to false)
                    message = ""
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Send", tint = Color(0xFF00CCFF)) // Use proper send icon
            }
        }
    }
}

@Composable
fun AssetOnboardingView(onStart: () -> Unit) {
    CyberFrame(label = "INITIALIZATION_REQUIRED") {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No asset nodes detected. Initialize tracking to monitor your matrix net worth.",
                color = Color.Gray,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9F))
            ) {
                Text("LINK_NODES", color = Color.Black)
            }
        }
    }
}

@Composable
fun AssetDashboardView(
    summary: com.neon.ascent.model.NetWorthSummary,
    accounts: List<AssetAccount>,
    onAdd: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            CyberFrame(label = "CORE_LIQUIDITY") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TOTAL_NET_WORTH", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        "$${String.format(Locale.getDefault(), "%,.2f", summary.totalValue)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Allocation Breakdown
                    AllocationBreakdown(summary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MetricSmall("LIQUID", "$${String.format(Locale.getDefault(), "%,.0f", summary.liquidNetWorth)}", Color(0xFF00FF9F).copy(alpha = 0.7f))
                        MetricSmall("DEBT", "$${String.format(Locale.getDefault(), "%,.0f", summary.totalDebt)}", Color.Red.copy(alpha = 0.7f))
                        MetricSmall("EXPENSES", "$${String.format(Locale.getDefault(), "%,.0f", summary.totalExpenses)}", Color(0xFFFFCC00).copy(alpha = 0.7f))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ACTIVE_NODES", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF00FF9F))
                }
            }
        }

        items(accounts) { account ->
            AssetAccountRow(account)
        }
    }
}

@Composable
fun AllocationBreakdown(summary: com.neon.ascent.model.NetWorthSummary) {
    val total = (summary.liquidNetWorth + summary.totalDebt + summary.totalExpenses).coerceAtLeast(1.0)
    val liquidWeight = (summary.liquidNetWorth / total).toFloat()
    val debtWeight = (summary.totalDebt / total).toFloat()
    val expenseWeight = (summary.totalExpenses / total).toFloat()

    Column {
        Text("ALLOCATION_MATRIX", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraSmall)) {
            Box(Modifier.fillMaxHeight().weight(liquidWeight.coerceAtLeast(0.01f)).background(Color(0xFF00FF9F)))
            Box(Modifier.fillMaxHeight().weight(debtWeight.coerceAtLeast(0.01f)).background(Color.Red))
            Box(Modifier.fillMaxHeight().weight(expenseWeight.coerceAtLeast(0.01f)).background(Color(0xFFFFCC00)))
        }
    }
}

@Composable
fun AssetAccountRow(account: AssetAccount) {
    CyberFrame(
        label = account.type.name,
        borderColor = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(account.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(account.institution, color = Color.Gray, fontSize = 10.sp)
            }
            Text(
                "$${String.format(Locale.getDefault(), "%,.2f", account.balance)}",
                color = if (account.type == AssetType.CREDIT_CARD || account.type == AssetType.LOAN) Color.Red else Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MetricSmall(label: String, value: String, color: Color = Color(0xFF00FF9F)) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AddAssetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, AssetType, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var inst by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AssetType.CASH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW_ASSET_NODE", fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Asset Name (e.g. Model S, Home)") })
                TextField(value = inst, onValueChange = { inst = it }, label = { Text("Institution/Provider") })
                TextField(value = balance, onValueChange = { balance = it }, label = { Text("Current Value/Balance") })
                
                Text("NODE_TYPE", color = Color.Gray, fontSize = 10.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssetType.values().forEach { assetType ->
                        FilterChip(
                            selected = type == assetType,
                            onClick = { type = assetType },
                            label = { Text(assetType.name, fontSize = 8.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, inst, type, balance.toDoubleOrNull() ?: 0.0) }) {
                Text("ADD")
            }
        }
    )
}
