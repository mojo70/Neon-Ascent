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
import com.neon.ascent.core.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensionReviewScreen(
    directiveId: String,
    onBack: () -> Unit,
    viewModel: AscensionReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalNeonTheme.current

    LaunchedEffect(directiveId) {
        viewModel.loadReview(directiveId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CYBR-TES // DIALECTIC_REVIEW", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.accent)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = theme.canvas,
                    titleContentColor = theme.accent
                )
            )
        },
        containerColor = theme.canvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(theme.canvas)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CyberFrame(
                label = "OPERATOR: ${uiState.directive?.title?.uppercase() ?: "UNKNOWN"}",
                accentColor = theme.secondary
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = theme.accent, modifier = Modifier.padding(24.dp))
                } else {
                    Text(
                        text = uiState.reviewText,
                        color = theme.ink,
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
                    color = theme.inkMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
