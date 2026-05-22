package com.neon.ascent.feature.lore

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.*
import com.neon.ascent.core.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoreScreen(
    onBack: () -> Unit,
    viewModel: LoreViewModel = hiltViewModel()
) {
    val userStory by viewModel.userStory.collectAsState()
    var editingIndex by remember { mutableIntStateOf(-2) } // -2: none, -1: main story, 0+: chapter
    var editBuffer by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NEURAL_HISTORY", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "BACK")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetStory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "RESET", tint = Color.Red.copy(alpha = 0.6f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color(0xFF00FF9C)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PerspectiveGrid()
            Scanlines(intensity = 0.1f)
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                item { Spacer(Modifier.height(16.dp)) }

                // Main Story / Biography
                item {
                    LoreSection(
                        title = "ORIGIN_LOG // BASE_BIO",
                        content = userStory.cyberLore,
                        isHacked = false, 
                        onEditClick = {
                            editingIndex = -1
                            editBuffer = userStory.cyberLore
                        }
                    )
                }

                // Weekly Chapters
                itemsIndexed(userStory.weeklyChapters) { index, chapter ->
                    LoreSection(
                        title = chapter.title,
                        content = chapter.content,
                        isHacked = chapter.isHacked,
                        onEditClick = {
                            editingIndex = index
                            editBuffer = chapter.content
                        }
                    )
                }
                
                item {
                    Spacer(Modifier.height(64.dp))
                }
            }

            // Edit Dialog
            if (editingIndex != -2) {
                AlertDialog(
                    onDismissRequest = { editingIndex = -2 },
                    containerColor = Color(0xFF0A0A0A),
                    title = { Text("QUICKHACK_INTERFACE", color = Color(0xFFFF006E), fontFamily = FontFamily.Monospace) },
                    text = {
                        OutlinedTextField(
                            value = editBuffer,
                            onValueChange = { editBuffer = it },
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF006E),
                                unfocusedBorderColor = Color(0xFFFF006E).copy(alpha = 0.5f),
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (editingIndex == -1) {
                                viewModel.hackMainStory(editBuffer)
                            } else {
                                viewModel.hackChapter(editingIndex, editBuffer)
                            }
                            editingIndex = -2
                        }) {
                            Text("UPLOAD_EXPLOIT", color = Color(0xFF00FF9C))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingIndex = -2 }) {
                            Text("ABORT", color = Color.Gray)
                        }
                    },
                    modifier = Modifier.neonBorder(Color(0xFFFF006E))
                )
            }
        }
    }
}

@Composable
fun LoreSection(
    title: String,
    content: String,
    isHacked: Boolean,
    onEditClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if (isHacked) Color(0xFFFF006E) else Color(0xFF00FFFF),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            )
            IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Edit, 
                    contentDescription = "HACK", 
                    tint = if (isHacked) Color(0xFFFF006E) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, if (isHacked) Color(0xFFFF006E).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Text(
                text = content,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 24.sp,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic
                )
            )
        }
    }
}
