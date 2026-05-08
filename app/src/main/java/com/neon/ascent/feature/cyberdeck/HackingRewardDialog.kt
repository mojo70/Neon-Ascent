package com.neon.ascent.feature.cyberdeck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.neon.ascent.model.HackingReward
import com.neon.ascent.model.Rarity

@Composable
fun HackingRewardDialog(
    reward: HackingReward,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFF00FF9F), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BREACH_SUCCESSFUL",
                    color = Color(0xFF00FF9F),
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RewardColumn("XP", reward.xp.toString(), Color(0xFF00B8FF))
                    RewardColumn("EDDIES", reward.eddies.toString(), Color(0xFFFFFF00))
                    RewardColumn("FRAGS", reward.fragments.toString(), Color(0xFFFF00AA))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "COMPONENTS_EXTRACTED:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                reward.components.forEach { (rarity, amount) ->
                    val color = when (rarity) {
                        Rarity.COMMON -> Color.White
                        Rarity.RARE -> Color(0xFF00B8FF)
                        Rarity.EPIC -> Color(0xFFAA00FF)
                        Rarity.LEGENDARY -> Color(0xFFFFCC00)
                    }
                    Text(
                        text = "${rarity.name}: $amount",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
                
                if (reward.craftingAvailable) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00FF9F).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF00FF9F))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CRAFTING_AVAILABLE",
                            color = Color(0xFF00FF9F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9F)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("ACKNOWLEDGE", color = Color.Black, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun RewardColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = color.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
