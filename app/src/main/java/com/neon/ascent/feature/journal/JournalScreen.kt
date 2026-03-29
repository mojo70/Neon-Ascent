package com.neon.ascent.feature.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.model.JournalEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(
    onBack: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020508))) {
        // Background Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = Color(0xFF00CCFF).copy(alpha = 0.05f)
            val step = 40.dp.toPx()
            for (x in 0..size.width.toInt() step step.toInt()) drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 0.5f)
            for (y in 0..size.height.toInt() step step.toInt()) drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 0.5f)
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "DECRYPTED_JOURNAL",
                color = Color(0xFF00FFAA),
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (entries.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("NO_ENTRIES_FOUND // DATABASE_EMPTY", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(entries) { entry ->
                        JournalEntryItem(
                            entry = entry,
                            onHeartClick = { viewModel.toggleHeart(entry) },
                            onDeleteClick = { viewModel.deleteEntry(entry) }
                        )
                    }
                }
            }

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).border(1.dp, Color(0xFF00FFAA))
            ) {
                Text("RETURN_TO_TERMINAL", color = Color(0xFF00FFAA), fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun JournalEntryItem(
    entry: JournalEntry,
    onHeartClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1015))
            .border(1.dp, Color(0xFF00CCFF).copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    entry.category.uppercase(),
                    color = Color(0xFF00CCFF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    dateFormat.format(Date(entry.timestamp)),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "\"${entry.text}\"",
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onHeartClick) {
                    Icon(
                        imageVector = if (entry.isHearted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Heart",
                        tint = if (entry.isHearted) Color(0xFFFF0088) else Color.Gray
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray
                    )
                }
            }
        }
        
        // Decorative corner accent
        Canvas(modifier = Modifier.matchParentSize()) {
            val path = Path().apply {
                moveTo(size.width - 10.dp.toPx(), 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, 10.dp.toPx())
            }
            drawPath(path, Color(0xFF00CCFF), style = Stroke(width = 2.dp.toPx()))
        }
    }
}
