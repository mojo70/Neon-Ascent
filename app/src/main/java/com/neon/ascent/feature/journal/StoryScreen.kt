package com.neon.ascent.feature.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.UserStoryRepository
import com.neon.ascent.domain.model.UserStory
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.core.common.Scanlines
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val userStoryRepository: UserStoryRepository
) : ViewModel() {
    val userStory: StateFlow<UserStory> = userStoryRepository.getMainStory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStory())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(
    onBack: () -> Unit,
    onHackingRequired: () -> Unit,
    viewModel: StoryViewModel = hiltViewModel(),
    journalViewModel: JournalViewModel = hiltViewModel()
) {
    val story by viewModel.userStory.collectAsState()
    val isHacked by journalViewModel.isSystemDatabaseHacked.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020508))) {
        Scanlines()
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "YOUR_CORE_CHRONICLE", 
                            color = Color(0xFF00FFAA),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF00FFAA))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Bio Section
                CyberFrame(label = "BIOMETRIC_SUMMARY", borderColor = Color(0xFF00FFAA).copy(alpha = 0.5f)) {
                    Text(
                        text = story.bio.ifBlank { "NO_DATA_FOUND" },
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Aspirations Section
                CyberFrame(label = "NEURAL_ASPIRATIONS", borderColor = Color(0xFF00CCFF).copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        if (story.grandAspirations.isEmpty()) {
                            Text("NO_ASPIRATIONS_DECODED", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            story.grandAspirations.forEach { aspiration ->
                                Text(
                                    "// $aspiration",
                                    color = Color(0xFF00CCFF),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Cyber Lore (Hacking Required if not hacked)
                CyberFrame(
                    label = "GENERATED_CYBER_LORE", 
                    borderColor = if (isHacked) Color(0xFFFF0088) else Color.Red
                ) {
                    if (isHacked) {
                        Text(
                            text = if (story.cyberLore.isBlank()) "LORE_EMPTY" else story.cyberLore,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "ENCRYPTION_ACTIVE: RE-HACK REQUIRED",
                                color = Color.Red,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onHackingRequired,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFAA))
                            ) {
                                Text("INITIATE_DECRYPTION", color = Color(0xFF00FFAA), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
