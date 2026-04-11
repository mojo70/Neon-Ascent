package com.neon.ascent.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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

data class ChapterPage(
    val chapterIndex: Int,
    val chapterTitle: String,
    val content: String
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

    // Paginate content into screen-sized chunks
    val bookPages = remember(book) {
        book?.chapters?.flatMapIndexed { chapterIndex, chapter ->
            val text = stripHtml(chapter.content)
            val paragraphs = text.split("\n").filter { it.isNotBlank() }
            val pages = mutableListOf<String>()
            var current = StringBuilder()

            // Chunking into roughly 1200 characters to fit most screens
            for (p in paragraphs) {
                if (current.length + p.length > 1200 && current.isNotEmpty()) {
                    pages.add(current.toString().trim())
                    current = StringBuilder()
                }
                current.append(p).append("\n\n")
            }
            if (current.isNotEmpty()) pages.add(current.toString().trim())
            if (pages.isEmpty()) pages.add("")

            pages.map { content ->
                ChapterPage(chapterIndex, chapter.title, content)
            }
        } ?: emptyList()
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { bookPages.size }
    )

    LaunchedEffect(bookId) {
        viewModel.loadBookFromAssets(bookAssetPath, bookId)
    }

    // Restore progress
    LaunchedEffect(bookPages) {
        if (bookPages.isNotEmpty()) {
            val savedPageIndex = viewModel.getSavedProgress(bookId)
            if (savedPageIndex in bookPages.indices) {
                pagerState.scrollToPage(savedPageIndex)
            }
        }
    }

    // Save progress
    LaunchedEffect(pagerState.currentPage) {
        if (bookPages.isNotEmpty()) {
            viewModel.saveProgress(bookId, pagerState.currentPage)
        }
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
                } else if (bookPages.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { pageIdx ->
                        val page = bookPages[pageIdx]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = page.chapterTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = Color(0xFFFF006E),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black
                                ),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            Text(
                                text = page.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontFamily = if (book?.language == "en") FontFamily.Default else NotoSerif,
                                    lineHeight = 28.sp,
                                    fontSize = 18.sp
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // Navigation Controls
                    val scope = rememberCoroutineScope()
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
                                onClick = { 
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                },
                                enabled = pagerState.currentPage > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                modifier = Modifier.border(1.dp, Color(0xFF00FF9C)).weight(1f)
                            ) {
                                Text("PREV", color = Color(0xFF00FF9C))
                            }
                            
                            Text(
                                text = "PAGE ${pagerState.currentPage + 1} / ${bookPages.size}",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.labelSmall
                            )

                            Button(
                                onClick = { 
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                },
                                enabled = pagerState.currentPage < bookPages.size - 1,
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
