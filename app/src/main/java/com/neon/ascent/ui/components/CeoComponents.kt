package com.neon.ascent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.lore.data.Megacorp
import com.neon.ascent.core.common.neonBorder
import com.neon.ascent.core.common.*

@Composable
fun CeoContactCard(
    megacorp: Megacorp,
    trustLevel: Float = 0f, // 0.0 - 1.0
    onMessageClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neonTheme = LocalNeonTheme.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onProfileClick() }
            .neonBorder(
                color = neonTheme.accentFor(megacorp.id), 
                glowIntensity = (0.3f + trustLevel * 0.4f).coerceIn(0f, 1f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Corp Logo / Avatar (glowing orb or icon)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .neonGlow(neonTheme.accentFor(megacorp.id)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        megacorp.name.take(1), 
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = megacorp.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = neonTheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${megacorp.ceo.netHandle ?: "UNKNOWN"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = neonTheme.accent,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Status
                OnlineStatusIndicator(isOnline = true)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = megacorp.slogan,
                style = MaterialTheme.typography.bodyMedium,
                color = neonTheme.textSecondary,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(8.dp))

            TrustMeter(
                trustLevel = trustLevel,
                label = "EXECUTIVE TRUST"
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton(
                    text = "MESSAGE @${megacorp.ceo.netHandle ?: "UNKNOWN"}",
                    onClick = onMessageClick,
                    modifier = Modifier.weight(1f)
                )
                NeonButton(
                    text = "DOSSIER",
                    onClick = onProfileClick,
                    variant = ButtonVariant.SECONDARY
                )
            }
        }
    }
}

@Composable
fun OnlineStatusIndicator(isOnline: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "StatusAnim")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF00FF9F).copy(alpha = alpha) else Color.Gray)
                .then(if (isOnline) Modifier.border(1.dp, Color(0xFF00FF9F), CircleShape) else Modifier)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (isOnline) "ONLINE" else "OFFLINE",
            color = if (isOnline) Color(0xFF00FF9F) else Color.Gray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TrustMeter(
    trustLevel: Float,
    label: String
) {
    val neonTheme = LocalNeonTheme.current
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = neonTheme.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${(trustLevel * 100).toInt()}%",
                color = if (trustLevel > 0.75f) neonTheme.secondary else neonTheme.accent,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            val segments = 10
            Row(modifier = Modifier.fillMaxSize()) {
                repeat(segments) { i ->
                    val segmentThreshold = (i + 1).toFloat() / segments
                    val isActive = trustLevel >= segmentThreshold
                    val color = when {
                        !isActive -> Color.Transparent
                        trustLevel > 0.75f -> neonTheme.secondary
                        trustLevel > 0.4f -> neonTheme.accent
                        else -> Color.Red
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(1.dp)
                            .background(color)
                    )
                }
            }
        }
    }
}

enum class ButtonVariant { PRIMARY, SECONDARY }

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY
) {
    val neonTheme = LocalNeonTheme.current
    val baseColor = if (variant == ButtonVariant.PRIMARY) neonTheme.primary else neonTheme.secondary
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .neonBorder(color = baseColor, width = 1.dp, cornerRadius = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = baseColor.copy(alpha = 0.1f),
            contentColor = baseColor
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = Shadow(color = baseColor.copy(alpha = 0.5f), blurRadius = 8f)
            )
        )
    }
}

fun Modifier.neonGlow(color: Color) = this.drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.4f), Color.Transparent),
            center = center,
            radius = size.maxDimension
        ),
        radius = size.maxDimension
    )
}
