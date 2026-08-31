package com.neon.ascent.feature.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.theme.*
import com.neon.ascent.core.common.*
import com.neon.ascent.ui.*
import com.neon.ascent.feature.dashboard.DashboardViewModel

@Composable
fun EurodollarWalletScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val theme = LocalNeonTheme.current
    val char by viewModel.userCharacter.collectAsState()
    val walletAddress = if (char?.walletConnected == true) "0x742d35Cc6634C0532925a3b844Bc454e4438f44e" else "NOT_CONNECTED"
    val secureBalance = char?.secureEddies ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.canvas)
    ) {
        if (theme.mode == VisualMode.CYBER) {
            CyberGridBackground()
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.accent)
                }
                Text(
                    "EURODOLLAR_WALLET // SOLANA",
                    color = theme.accent,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wallet Card
            CyberFrame(label = "NETRUNNER_ASSETS") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SECURE BALANCE", color = theme.inkMuted, fontSize = 12.sp)
                    Text(
                        "€$ $secureBalance.00 ED",
                        color = theme.ink,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "WALLET: ${if (walletAddress.length > 12) walletAddress.take(6) + "..." + walletAddress.takeLast(6) else walletAddress}",
                        color = theme.accent.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { /* TODO: Connect Wallet Logic */ },
                    modifier = Modifier.weight(1f).height(50.dp).clip(CyberButtonShape).border(1.dp, theme.accent, CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.surface)
                ) {
                    Text(if (char?.walletConnected == true) "DISCONNECT" else "CONNECT", color = theme.accent, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { /* Refresh Balance */ },
                    modifier = Modifier.weight(1f).height(50.dp).clip(CyberButtonShape).border(1.dp, theme.secondary, CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.surface)
                ) {
                    Text("REFRESH", color = theme.secondary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Transaction History
            CyberFrame(label = "TRANSACTION_LEDGER") {
                val transactions = listOf(
                    Transaction("VAULT_TRANSFER", "+$secureBalance.00", "SUCCESS"),
                    Transaction("FIXER_PAYOUT", "+500.00", "SUCCESS"),
                    Transaction("AMMO_RESUPPLY", "-120.50", "SUCCESS"),
                )
                
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(transactions) { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(tx.label, color = theme.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(tx.status, color = theme.accent.copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                            Text(
                                tx.amount, 
                                color = if (tx.amount.startsWith("+")) theme.accent else theme.secondary,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

data class Transaction(val label: String, val amount: String, val status: String)
