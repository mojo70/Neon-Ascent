package com.neon.ascent.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.model.UserCharacter
import kotlin.random.Random

val CyberButtonShape = GenericShape { size, _ ->
    moveTo(0f, 12f)
    lineTo(12f, 0f)
    lineTo(size.width - 24f, 0f)
    lineTo(size.width, 24f)
    lineTo(size.width, size.height - 12f)
    lineTo(size.width - 12f, size.height)
    lineTo(24f, size.height)
    lineTo(0f, size.height - 24f)
    close()
}

val HexTerminalShape = GenericShape { size, _ ->
    val gap = 14f
    moveTo(gap, 0f)
    lineTo(size.width - gap, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width - gap, size.height)
    lineTo(gap, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

@Composable
fun CyberFrame(
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF006E),
    borderColor: Color = Color(0xFF00FF9C),
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                Modifier
                    .size(4.dp, 16.dp)
                    .background(accentColor)
                    .neonBorder(accentColor, width = 1.dp, glowIntensity = 0.8f, cornerRadius = 0.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = borderColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .height(1.dp)
                    .weight(1f)
                    .background(borderColor.copy(alpha = 0.3f))
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neonBorder(borderColor.copy(alpha = 0.6f), width = 1.dp, cornerRadius = 4.dp)
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(12.dp)
        ) {
            content()
        }
    }
}

@Composable
fun HudCornerAccents(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineLen = 24.dp.toPx()
        val thickness = 2.dp.toPx()
        
        drawLine(color, Offset(0f, 0f), Offset(lineLen, 0f), thickness)
        drawLine(color, Offset(0f, 0f), Offset(0f, lineLen), thickness)
        
        drawLine(color, Offset(size.width, 0f), Offset(size.width - lineLen, 0f), thickness)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, lineLen), thickness)
        
        drawLine(color, Offset(0f, size.height), Offset(lineLen, size.height), thickness)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - lineLen), thickness)
        
        drawLine(color, Offset(size.width, size.height), Offset(size.width - lineLen, size.height), thickness)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - lineLen), thickness)
    }
}

@Composable
fun PixelatedSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val color = Color(0xFF00FF9C).copy(alpha = 0.2f)
        val pixelSize = size.width / 12f
        
        drawRect(color, Offset(center.x - pixelSize*2, pixelSize*1.5f), Size(pixelSize*4, pixelSize*4))
        drawRect(color, Offset(center.x - pixelSize, pixelSize*5.5f), Size(pixelSize*2, pixelSize*1.5f))
        drawRect(color, Offset(center.x - pixelSize*3.5f, pixelSize*7f), Size(pixelSize*7, pixelSize*10f))
    }
}

@Composable
fun AvatarImage(character: UserCharacter?, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val avatarBitmap = remember(character?.avatarPath) {
        if (character?.avatarPath != null && character.avatarPath != "internal_storage_placeholder") {
            try {
                BitmapFactory.decodeFile(character.avatarPath)
            } catch (e: Exception) { null }
        } else { null }
    }

    if (avatarBitmap != null) {
        Image(
            bitmap = avatarBitmap.asImageBitmap(),
            contentDescription = "Avatar",
            modifier = modifier.alpha(alpha),
            contentScale = ContentScale.Crop
        )
    } else {
        PixelatedSilhouette(modifier = modifier)
    }
}

@Composable
fun NeuralLoadGauge(load: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "GaugeAnim")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f + (load * 0.05f),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (1000 / (load + 0.5f).coerceAtLeast(0.1f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (5000 / (load + 0.1f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.graphicsLayer {
        scaleX = pulseScale
        scaleY = pulseScale
    }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension - strokeWidth) / 2
            
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            val sweepAngle = load * 360f
            val gaugeColor = if (load > 0.7f) Color(0xFFFF006E) else Color(0xFF00FF9C)
            val glowIntensity = 0.5f + (load * 0.5f)
            
            for (i in 0..6) {
                val f = i.toFloat()
                val glowAlpha = (0.25f - f * 0.03f).coerceAtLeast(0f) * glowIntensity
                drawArc(
                    color = gaugeColor.copy(alpha = glowAlpha),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth + f * (10f + load * 5f), cap = StrokeCap.Round)
                )
            }
            
            drawArc(
                color = gaugeColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            rotate(rotation, center) {
                drawLine(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, gaugeColor.copy(alpha = 0.6f))),
                    start = center,
                    end = Offset(center.x, center.y - radius),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(load * 100).toInt()}%",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    shadow = Shadow(color = if (load > 0.7f) Color(0xFFFF006E) else Color(0xFF00FF9C), blurRadius = 15f * load)
                )
            )
            Text(
                text = "LOAD",
                color = if (load > 0.7f) Color(0xFFFF006E) else Color(0xFF00FF9C),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CyberActionButton(
    label: String, 
    color: Color, 
    modifier: Modifier = Modifier, 
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val glowIntensity by animateFloatAsState(
        targetValue = if (isPressed) 0.3f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "GlowIntensity"
    )
    
    val staticRipple by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(150),
        label = "StaticRipple"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(HexTerminalShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(Color(0xFF080808).copy(alpha = 0.9f))
            .neonBorder(
                color = color, 
                width = 2.dp, 
                glowIntensity = glowIntensity,
                cornerRadius = 0.dp // HexTerminalShape handles shape
            )
            .drawBehind {
                if (staticRipple > 0f) {
                    val random = Random(System.nanoTime())
                    repeat(10) {
                        val y = random.nextFloat() * size.height
                        drawLine(
                            color = color.copy(alpha = 0.2f * staticRipple),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label, 
            color = if (isPressed) color.copy(alpha = 0.7f) else color, 
            fontSize = 14.sp,
            fontWeight = FontWeight.Black, 
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = color.copy(alpha = 0.5f * glowIntensity),
                    blurRadius = 10f * glowIntensity
                )
            )
        )
    }
}

@Composable
fun CyberMetricCard(label: String, value: String, subValue: String, color: Color = Color(0xFF00FF9C), modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F0F0F).copy(alpha = 0.7f))
            .neonBorder(color.copy(alpha = 0.6f), width = 1.dp, cornerRadius = 8.dp)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = label, 
                color = color, 
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = value, 
                color = Color.White, 
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black, 
                    fontFamily = FontFamily.Monospace
                )
            )
            Text(
                text = subValue, 
                color = Color.Gray, 
                fontSize = 9.sp, 
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
