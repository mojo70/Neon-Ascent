package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensionReviewScreen(
    directiveId: String,
    onBack: () -> Unit,
    viewModel: AscensionReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(directiveId) {
        viewModel.loadReview(directiveId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CYBR-TES // DIALECTIC_REVIEW", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CyberFrame(
                label = "OPERATOR: ${uiState.directive?.title?.uppercase() ?: "UNKNOWN"}",
                accentColor = NeonPink
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.padding(24.dp))
                } else {
                    Text(
                        text = uiState.reviewText,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            if (!uiState.isLoading) {
                Text(
                    "\"The unexamined life is not worth jacking in.\"",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
