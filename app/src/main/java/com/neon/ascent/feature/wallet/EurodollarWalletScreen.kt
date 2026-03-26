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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.feature.charactercreation.CyberButtonShape
import com.neon.ascent.feature.charactercreation.CyberFrame
import com.neon.ascent.feature.charactercreation.CyberGridBackground

@Composable
fun EurodollarWalletScreen(onBack: () -> Unit) {
    var walletAddress by remember { mutableStateOf("NOT_CONNECTED") }
    var balance by remember { mutableStateOf(0.0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CyberGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9C))
                }
                Text(
                    "EURODOLLAR_WALLET // SOLANA",
                    color = Color(0xFF00FF9C),
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
                    Text("TOTAL BALANCE", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        "€$ ${String.format("%.2f", balance)} ED",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "WALLET: ${if (walletAddress.length > 12) walletAddress.take(6) + "..." + walletAddress.takeLast(6) else walletAddress}",
                        color = Color(0xFF00FF9C).copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { /* Connect Wallet */ },
                    modifier = Modifier.weight(1f).height(50.dp).clip(CyberButtonShape).border(1.dp, Color(0xFF00FF9C), CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A))
                ) {
                    Text("CONNECT", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { /* Refresh Balance */ },
                    modifier = Modifier.weight(1f).height(50.dp).clip(CyberButtonShape).border(1.dp, Color(0xFFFF006E), CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A))
                ) {
                    Text("REFRESH", color = Color(0xFFFF006E), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Transaction History
            CyberFrame(label = "TRANSACTION_LEDGER") {
                val transactions = listOf(
                    Transaction("FIXER_PAYOUT", "+500.00", "SUCCESS"),
                    Transaction("AMMO_RESUPPLY", "-120.50", "SUCCESS"),
                    Transaction("NEURAL_UPGRADE", "-2500.00", "SUCCESS"),
                    Transaction("MISSION_REWARD", "+1200.00", "SUCCESS")
                )
                
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(transactions) { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(tx.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(tx.status, color = Color(0xFF00FF9C).copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                            Text(
                                tx.amount, 
                                color = if (tx.amount.startsWith("+")) Color(0xFF00FF9C) else Color(0xFFFF006E),
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
