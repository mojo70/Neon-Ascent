package com.neon.ascent.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.R
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.CyberGridBackground
import com.neon.ascent.ui.GlitchOverlay
import org.jsoup.Jsoup

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val NotoSerif = FontFamily(
    Font(googleFont = GoogleFont("Noto Serif"), fontProvider = provider)
)

@Composable
fun EReaderScreen(
    viewModel: EReaderViewModel = hiltViewModel(),
    bookAssetPath: String,
    bookId: String,
    onBack: () -> Unit
) {
    val book by viewModel.currentBook.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var currentChapterIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(bookId) {
        viewModel.loadBookFromAssets(bookAssetPath, bookId)
    }

    val layoutDirection = if (book?.language == "he") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
            CyberGridBackground()
            GlitchOverlay(intensity = 0.05f)

            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00FF9C)
                        )
                    }
                    Text(
                        text = "// READER // ${book?.title ?: "LOADING..."}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF00FF9C),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00FF9C))
                    }
                } else if (book != null) {
                    val chapter = book?.chapters?.getOrNull(currentChapterIndex)
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = chapter?.title ?: "Unknown Chapter",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color(0xFFFF006E),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            ),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Text(
                            text = stripHtml(chapter?.content ?: ""),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontFamily = if (book?.language == "en") FontFamily.Default else NotoSerif,
                                lineHeight = 28.sp,
                                fontSize = 18.sp
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // Navigation Controls
                    CyberFrame(
                        label = "PAGE_CONTROLS",
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (currentChapterIndex > 0) currentChapterIndex-- },
                                enabled = currentChapterIndex > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                modifier = Modifier.border(1.dp, Color(0xFF00FF9C)).weight(1f)
                            ) {
                                Text("PREV", color = Color(0xFF00FF9C))
                            }
                            
                            Text(
                                text = "${currentChapterIndex + 1} / ${book?.chapters?.size}",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.labelSmall
                            )

                            Button(
                                onClick = { if (currentChapterIndex < (book?.chapters?.size ?: 0) - 1) currentChapterIndex++ },
                                enabled = currentChapterIndex < (book?.chapters?.size ?: 0) - 1,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                modifier = Modifier.border(1.dp, Color(0xFF00FF9C)).weight(1f)
                            ) {
                                Text("NEXT", color = Color(0xFF00FF9C))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun stripHtml(html: String): String {
    return Jsoup.parse(html).text()
}
