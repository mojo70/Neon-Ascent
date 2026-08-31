package com.neon.ascent.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.R
import com.neon.ascent.core.common.*
import com.neon.ascent.model.TerminalEvent
import com.neon.ascent.model.TrainingTemplate
import com.neon.ascent.core.domain.character.models.UserCharacter
import kotlin.random.Random

val CyberCutShape = GenericShape { size, _ ->
    val cutSize = 12f
    moveTo(cutSize, 0f)
    lineTo(size.width - cutSize, 0f)
    lineTo(size.width, cutSize)
    lineTo(size.width, size.height - cutSize)
    lineTo(size.width - cutSize, size.height)
    lineTo(cutSize, size.height)
    lineTo(0f, size.height - cutSize)
    lineTo(0f, cutSize)
    close()
}

@Composable
fun SoftGridBackground(modifier: Modifier = Modifier) {
    val theme = LocalNeonTheme.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSpacing = 40.dp.toPx()
        val gridColor = theme.grid
        
        // Horizontal lines
        for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 1f
            )
        }
        
        // Vertical lines
        for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                strokeWidth = 1f
            )
        }
    }
}

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
fun CyberTabButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    palette: NeonThemeData = LocalNeonTheme.current
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(CyberButtonShape)
            .background(if (selected) palette.secondary else palette.canvas)
            .border(
                width = 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.4f) else palette.ink.copy(alpha = 0.2f),
                shape = CyberButtonShape
            )
            .selectable(selected = selected, onClick = onClick, role = androidx.compose.ui.semantics.Role.RadioButton),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else palette.ink.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
fun TemplateCard(
    template: TrainingTemplate,
    isSelected: Boolean,
    isSuggested: Boolean = false,
    onClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val accentColor = if (isSuggested) theme.accentDanger else theme.accent
    
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(80.dp)
            .clip(CyberButtonShape)
            .background(if (isSelected) accentColor else theme.canvas)
            .border(
                width = if (isSuggested) 2.dp else 1.dp,
                color = if (isSelected) theme.ink else accentColor.copy(alpha = 0.4f),
                shape = CyberButtonShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = template.name,
                color = if (isSelected) theme.canvas else theme.ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            if (isSuggested) {
                Text(
                    text = "[SYSTEM_MATCH]",
                    color = if (isSelected) theme.canvas.copy(alpha = 0.7f) else accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun CyberHelmetIcon(
    modifier: Modifier = Modifier.size(120.dp),
    neuralLoad: Float = 0.2f,        // 0.0 to 1.0 — drives glow intensity & effects
    primaryColor: Color = LocalNeonTheme.current.accent
) {
    val theme = LocalNeonTheme.current
    if (theme.mode == VisualMode.STEVE) {
        // Simplified icon for Steve mode? Or just follow rules.
        // Rule: No glow/bloom.
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "HelmetAnim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f + (neuralLoad * 0.1f),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2.2f

        // 1. Outer helmet glow layers
        if (theme.glowEnabled) {
            for (i in 0..10) {
                val f = i.toFloat()
                drawCircle(
                    color = primaryColor.copy(alpha = (0.12f - f * 0.01f).coerceAtLeast(0f) * (1f + neuralLoad)),
                    radius = radius + f * 5f,
                    style = Stroke(width = 12f + f * 4f)
                )
            }
        }

        // 2. Main helmet shell
        val helmetPath = Path().apply {
            // ... (rest of path same)
            moveTo(center.x - radius * 0.75f, center.y - radius * 0.6f)
            quadraticBezierTo(center.x, center.y - radius * 1.0f, center.x + radius * 0.75f, center.y - radius * 0.6f)
            lineTo(center.x + radius * 0.9f, center.y - radius * 0.2f)
            lineTo(center.x + radius * 0.9f, center.y + radius * 0.3f)
            lineTo(center.x + radius * 0.7f, center.y + radius * 0.8f)
            quadraticBezierTo(center.x, center.y + radius * 0.95f, center.x - radius * 0.7f, center.y + radius * 0.8f)
            lineTo(center.x - radius * 0.9f, center.y + radius * 0.3f)
            lineTo(center.x - radius * 0.9f, center.y - radius * 0.2f)
            close()
        }

        // Base shell
        drawPath(helmetPath, color = if (theme.mode == VisualMode.CYBER) Color(0xFF020806) else theme.surface, style = Fill)

        // 3. Helmet outline
        if (theme.glowEnabled) {
            for (i in 0..3) {
                drawPath(
                    path = helmetPath,
                    color = primaryColor.copy(alpha = 0.3f / (i + 1)),
                    style = Stroke(width = 8f + i * 6f)
                )
            }
        }
        drawPath(
            path = helmetPath,
            color = if (theme.mode == VisualMode.STEVE) theme.ink else primaryColor,
            style = Stroke(width = if (theme.mode == VisualMode.STEVE) 1.dp.toPx() else 3f)
        )

        // 4. Visor
        val visorColor = if (theme.mode == VisualMode.STEVE) theme.ink else if (neuralLoad > 0.7f) Color(0xFFFF006E) else Color(0xFF00FFFF)
        val visorLeft = center.x - radius * 0.65f
        val visorTop = center.y - radius * 0.3f
        val visorWidth = radius * 1.3f
        val visorHeight = radius * 0.4f
        
        // Inner visor glow
        drawRect(
            color = if (theme.mode == VisualMode.STEVE) theme.surfaceRaised else visorColor.copy(alpha = 0.15f + neuralLoad * 0.2f),
            topLeft = Offset(visorLeft, visorTop),
            size = Size(visorWidth, visorHeight),
            style = Fill
        )

        // Visor border layers
        if (theme.glowEnabled) {
            for (i in 0..5) {
                val f = i.toFloat()
                drawRect(
                    color = visorColor.copy(alpha = (0.2f - f * 0.03f).coerceAtLeast(0f) * (1f + neuralLoad)),
                    topLeft = Offset(visorLeft - f * 2, visorTop - f * 2),
                    size = Size(visorWidth + f * 4, visorHeight + f * 4),
                    style = Stroke(width = 2f + f * 2f)
                )
            }
        }
        drawRect(
            color = visorColor, 
            topLeft = Offset(visorLeft, visorTop), 
            size = Size(visorWidth, visorHeight), 
            style = Stroke(width = if (theme.mode == VisualMode.STEVE) 1.dp.toPx() else 2f)
        )

        // Visor Detail
        if (theme.mode == VisualMode.CYBER) {
            val scanlineY = (System.currentTimeMillis() % 2000 / 2000f) * visorHeight
            drawLine(
                color = Color.White.copy(alpha = 0.4f * pulse),
                start = Offset(visorLeft + 5f, visorTop + scanlineY),
                end = Offset(visorLeft + visorWidth - 5f, visorTop + scanlineY),
                strokeWidth = 1.5f
            )
        }
        
        // Random data dots
        if (neuralLoad > 0.5f && theme.mode == VisualMode.CYBER) {
            val r = Random(System.currentTimeMillis() / 100)
            repeat(5) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = 1.5f,
                    center = Offset(
                        visorLeft + r.nextFloat() * visorWidth,
                        visorTop + r.nextFloat() * visorHeight
                    )
                )
            }
        }

        // 5. Side accent panels
        drawRoundRect(
            color = if (theme.mode == VisualMode.STEVE) theme.ink else Color(0xFFFF006E).copy(alpha = 0.8f),
            topLeft = Offset(center.x - radius * 0.95f, center.y),
            size = Size(radius * 0.2f, radius * 0.3f),
            cornerRadius = CornerRadius(4f),
            style = Fill
        )
        // Accent glow
        if (theme.glowEnabled) {
            drawCircle(
                color = Color(0xFFFF006E).copy(alpha = 0.3f * pulse),
                radius = radius * 0.15f,
                center = Offset(center.x - radius * 0.85f, center.y + radius * 0.15f)
            )
        }
    }
}

@Composable
fun HeartRatePulse(heartRate: Int) {
    val theme = LocalNeonTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "HeartRate")
    val duration = (60000 / heartRate.coerceAtLeast(40)).toInt()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = "Heart Rate",
        tint = if (theme.mode == VisualMode.STEVE) theme.ink else Color(0xFFFF006E),
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
    )
}

@Composable
fun CyberFrame(
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    val theme = LocalNeonTheme.current
    val finalAccent = accentColor ?: theme.accentDanger
    val finalBorder = borderColor ?: theme.accent
    val finalBg = backgroundColor ?: theme.overlay

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                Modifier
                    .size(4.dp, 16.dp)
                    .background(finalAccent)
                    .neonBorder(finalAccent, width = 1.dp, glowIntensity = if (theme.glowEnabled) 0.8f else 0f, cornerRadius = 0.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (theme.mode == VisualMode.STEVE) theme.ink else finalBorder,
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
                    .background(finalBorder.copy(alpha = 0.3f))
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neonBorder(finalBorder.copy(alpha = 0.6f), width = 1.dp, cornerRadius = 4.dp, glowIntensity = if (theme.glowEnabled) 1f else 0f)
                .background(finalBg)
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
fun AvatarImage(
    character: UserCharacter?, 
    modifier: Modifier = Modifier, 
    alpha: Float = 1f,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center
) {
    val theme = LocalNeonTheme.current
    val avatarBitmap = remember(character?.avatarPath) {
        if (character?.avatarPath != null && character.avatarPath != "internal_storage_placeholder") {
            try {
                BitmapFactory.decodeFile(character.avatarPath)
            } catch (e: Exception) { null }
        } else { null }
    }

    val finalModifier = modifier.alpha(alpha)
    val colorFilter = if (theme.mode == VisualMode.STEVE) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    } else {
        null
    }

    if (avatarBitmap != null) {
        Image(
            bitmap = avatarBitmap.asImageBitmap(),
            contentDescription = "Avatar",
            modifier = finalModifier,
            contentScale = contentScale,
            alignment = alignment,
            colorFilter = colorFilter
        )
    } else {
        // Use the provided holographic body image as the fallback
        Image(
            painter = painterResource(id = R.drawable.full_body_hologram),
            contentDescription = "Holographic Avatar",
            modifier = finalModifier,
            contentScale = contentScale,
            alignment = alignment,
            colorFilter = colorFilter
        )
    }
}

@Composable
fun NeuralLoadGauge(load: Float, modifier: Modifier = Modifier) {
    val theme = LocalNeonTheme.current
    val gaugeColor = if (theme.mode == VisualMode.STEVE) theme.ink else if (load > 0.7f) Color(0xFFFF006E) else Color(0xFF00FF9C)
    
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
                color = theme.ink.copy(alpha = 0.05f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            val sweepAngle = load * 360f
            val glowIntensity = if (theme.glowEnabled) 0.5f + (load * 0.5f) else 0f
            
            if (theme.glowEnabled) {
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
            }
            
            drawArc(
                color = gaugeColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = if (theme.mode == VisualMode.STEVE) 2.dp.toPx() else strokeWidth, cap = StrokeCap.Round)
            )

            if (theme.mode == VisualMode.CYBER) {
                rotate(rotation, center) {
                    drawLine(
                        brush = Brush.verticalGradient(listOf(Color.Transparent, gaugeColor.copy(alpha = 0.6f))),
                        start = center,
                        end = Offset(center.x, center.y - radius),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(load * 100).toInt()}%",
                color = theme.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                style = if (theme.glowEnabled) TextStyle(
                    shadow = Shadow(color = gaugeColor, blurRadius = 15f * load)
                ) else LocalTextStyle.current
            )
            Text(
                text = "LOAD",
                color = if (theme.mode == VisualMode.STEVE) theme.inkMuted else gaugeColor,
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val buttonColor = if (enabled) (if (theme.mode == VisualMode.STEVE) theme.ink else color) else theme.inkMuted
    
    val glowIntensity by animateFloatAsState(
        targetValue = if (isPressed && enabled && theme.glowEnabled) 0.3f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "GlowIntensity"
    )
    
    val staticRipple by animateFloatAsState(
        targetValue = if (isPressed && enabled && theme.mode == VisualMode.CYBER) 1f else 0f,
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
                enabled = enabled,
                onClick = onClick
            )
            .background(theme.canvas.copy(alpha = 0.9f))
            .neonBorder(
                color = buttonColor, 
                width = 2.dp, 
                glowIntensity = if (enabled && theme.glowEnabled) glowIntensity else 0f,
                cornerRadius = 0.dp // HexTerminalShape handles shape
            )
            .drawBehind {
                if (staticRipple > 0f && enabled) {
                    val random = Random(System.nanoTime())
                    repeat(10) {
                        val y = random.nextFloat() * size.height
                        drawLine(
                            color = buttonColor.copy(alpha = 0.2f * staticRipple),
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
            color = if (isPressed && enabled) buttonColor.copy(alpha = 0.7f) else (if (theme.mode == VisualMode.STEVE && !isPressed) theme.ink else buttonColor), 
            fontSize = 14.sp,
            fontWeight = FontWeight.Black, 
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp,
            style = if (theme.glowEnabled) TextStyle(
                shadow = Shadow(
                    color = buttonColor.copy(alpha = 0.5f * glowIntensity),
                    blurRadius = 10f * glowIntensity
                )
            ) else LocalTextStyle.current
        )
    }
}

@Composable
fun TerminalFeedSection(feed: List<TerminalEvent>, cyan: Color, magenta: Color) {
    var isExpanded by remember { mutableStateOf(false) }
    
    CyberFrame(
        label = "LIVE_TERMINAL_FEED // CORE_SYNC",
        borderColor = cyan.copy(alpha = 0.8f),
        accentColor = magenta
    ) {
        Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(cyan, RoundedCornerShape(1.dp))
                            .alpha(if (feed.any { it.status == "PENDING" }) pulseAlpha() else 1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ACTIVE_OPERATIONS: ${feed.size}",
                        color = cyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = cyan.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (feed.isEmpty()) {
                        Text(
                            "> NO_ACTIVE_FEEDS_DETECTED",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontStyle = FontStyle.Italic
                        )
                    } else {
                        feed.forEach { event ->
                            TerminalFeedItem(event, cyan, magenta)
                        }
                    }
                }
            }
            
            if (!isExpanded && feed.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    feed.take(2).forEach { event ->
                        TerminalFeedItem(event, cyan, magenta, isCompact = true)
                    }
                    if (feed.size > 2) {
                        Text(
                            text = "... [${feed.size - 2} MORE OPERATIONS]",
                            color = cyan.copy(alpha = 0.4f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalFeedItem(event: TerminalEvent, cyan: Color, magenta: Color, isCompact: Boolean = false) {
    val color = when (event.status) {
        "COMPLETED", "LOGGED" -> cyan
        "PENDING" -> magenta
        else -> Color.White
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isCompact) 4.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "> [${event.type}] ",
            color = Color.Gray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = event.title,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = event.status,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun pulseAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    return infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    ).value
}

@Composable
fun CyberMetricCard(label: String, value: String, subValue: String, color: Color? = null, modifier: Modifier = Modifier) {
    val theme = LocalNeonTheme.current
    val finalColor = color ?: theme.accent
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.surface.copy(alpha = 0.7f))
            .neonBorder(finalColor.copy(alpha = 0.6f), width = 1.dp, cornerRadius = 8.dp, glowIntensity = if (theme.glowEnabled) 1f else 0f)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = label, 
                color = if (theme.mode == VisualMode.STEVE) theme.inkMuted else finalColor, 
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = value, 
                color = theme.ink, 
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black, 
                    fontFamily = FontFamily.Monospace
                )
            )
            Text(
                text = subValue, 
                color = theme.inkMuted, 
                fontSize = 9.sp, 
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val theme = LocalNeonTheme.current
    val finalColor = color ?: theme.accent
    var glitchState by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "GlitchText")
    
    val trigger by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Trigger"
    )

    LaunchedEffect(trigger) {
        if (Random.nextFloat() > 0.8f && theme.mode == VisualMode.CYBER) {
            glitchState = true
            kotlinx.coroutines.delay(100)
            glitchState = false
        }
    }

    Box(modifier = modifier) {
        if (glitchState) {
            Text(
                text = text,
                color = Color.Magenta.copy(alpha = 0.5f),
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.offset(x = 2.dp)
            )
            Text(
                text = text,
                color = Color.Cyan.copy(alpha = 0.5f),
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.offset(x = (-2).dp)
            )
        }
        Text(
            text = text,
            color = finalColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CyberCrossIcon(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val color = Color(0xFF00FF9C)
            val strokeWidth = 2.dp.toPx()
            
            // Draw a cyberpunk cross
            drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth)
            drawLine(color, Offset(0f, size.height * 0.35f), Offset(size.width, size.height * 0.35f), strokeWidth)
        }
    }
}

@Composable
fun NeuralJackIcon(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val color = Color(0xFFFF006E)
            drawCircle(color, radius = size.width / 4, style = Stroke(width = 2.dp.toPx()))
            drawCircle(color, radius = size.width / 8, style = Fill)
        }
    }
}

@Composable
fun HolyGhostAura(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Aura")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = modifier
            .size(100.dp)
            .background(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = alpha), Color.Transparent)
                )
            )
    )
}

@Composable
fun NightCityGlow(modifier: Modifier = Modifier) {
    val theme = LocalNeonTheme.current
    if (theme.mode == VisualMode.STEVE) return
    // Simple mock for NightCityGlow
}

@Composable
fun AcidRainOverlay(modifier: Modifier = Modifier) {
    val theme = LocalNeonTheme.current
    if (theme.mode == VisualMode.STEVE) return
    // Simple mock for AcidRainOverlay
}
