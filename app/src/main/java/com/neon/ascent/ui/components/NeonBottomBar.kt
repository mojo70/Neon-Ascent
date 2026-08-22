package com.neon.ascent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class NavItem(
    val label: String,
    val icon: ImageVector,
    val index: Int
) {
    object Codex : NavItem("CODEX", Icons.Default.AutoStories, 0)
    object Rig : NavItem("RIG", Icons.Default.Terminal, 1)
    object Deck : NavItem("DECK", Icons.Default.GridView, 2)
    object Labs : NavItem("LABS", Icons.Default.Science, 3)
    object Forge : NavItem("FORGE", Icons.Default.AddBox, 4)
    object Ops : NavItem("OPS", Icons.Default.MonitorHeart, 5)

    companion object {
        val items = listOf(Codex, Rig, Deck, Labs, Forge, Ops)
    }
}

@Composable
fun NeonBottomBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = Color(0xFF1A262E)
    val inactiveColor = Color(0xFF5A6E78)
    val activeColor = MaterialTheme.colorScheme.onBackground // 0xFF00FF9C

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF050505).copy(alpha = 0.95f))
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem.items.forEach { item ->
                val isSelected = selectedIndex == item.index
                val color = if (isSelected) activeColor else inactiveColor

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onItemSelected(item.index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        color = color,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.15.sp
                    )
                }
            }
        }
    }
}
