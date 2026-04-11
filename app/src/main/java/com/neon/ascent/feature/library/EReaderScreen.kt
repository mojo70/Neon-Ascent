package com.neon.ascent.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        // Dynamic pagination based on screen size
        val bookPages = remember(book, maxWidth, maxHeight) {
            if (book == null) return@remember emptyList()

            // More conservative estimate to prevent line clipping
            val heightInDp = maxHeight.value
            val widthInDp = maxWidth.value
            
            // Adjust line height and char width estimates for 17sp font / 26sp line height
            val linesPerPage = ((heightInDp - 120) / 30).toInt().coerceAtLeast(1)
            val charsPerLine = (widthInDp / 11).toInt().coerceAtLeast(1)
            val estimatedCharsPerPage = (linesPerPage * charsPerLine).coerceIn(300, 2000)

            book?.chapters?.flatMapIndexed { chapterIndex, chapter ->
                val text = stripHtmlWithBreaks(chapter.content)
                val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
                val pages = mutableListOf<String>()
                var currentPageText = StringBuilder()

                for (para in paragraphs) {
                    var remainingPara = para.trim()
                    
                    while (remainingPara.isNotEmpty()) {
                        val availableSpace = estimatedCharsPerPage - currentPageText.length
                        
                        if (remainingPara.length <= availableSpace) {
                            currentPageText.append(remainingPara).append("\n\n")
                            remainingPara = ""
                        } else {
                            // If very little space left on page, move paragraph to next page
                            if (availableSpace < estimatedCharsPerPage / 5 && currentPageText.isNotEmpty()) {
                                pages.add(currentPageText.toString().trim())
                                currentPageText = StringBuilder()
                            } else {
                                // Find last space within available space to avoid splitting words
                                var splitIndex = remainingPara.lastIndexOf(' ', availableSpace)
                                
                                if (splitIndex == -1) {
                                    // No space found? Split at availableSpace if page is empty, else move to next
                                    if (currentPageText.isEmpty()) {
                                        splitIndex = availableSpace.coerceAtMost(remainingPara.length)
                                    } else {
                                        pages.add(currentPageText.toString().trim())
                                        currentPageText = StringBuilder()
                                        continue 
                                    }
                                }
                                
                                val chunk = remainingPara.substring(0, splitIndex).trim()
                                currentPageText.append(chunk)
                                pages.add(currentPageText.toString().trim())
                                currentPageText = StringBuilder()
                                remainingPara = remainingPara.substring(splitIndex).trim()
                            }
                        }
                    }
                }
                
                if (currentPageText.isNotEmpty()) {
                    pages.add(currentPageText.toString().trim())
                }
                
                if (pages.isEmpty()) pages.add("[EMPTY_CHAPTER]")

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

        LaunchedEffect(bookPages) {
            if (bookPages.isNotEmpty()) {
                val savedPageIndex = viewModel.getSavedProgress(bookId)
                if (savedPageIndex in bookPages.indices) {
                    pagerState.scrollToPage(savedPageIndex)
                }
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            if (bookPages.isNotEmpty()) {
                viewModel.saveProgress(bookId, pagerState.currentPage)
            }
        }

        val layoutDirection = if (book?.language == "he") LayoutDirection.Rtl else LayoutDirection.Ltr

        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                            modifier = Modifier.weight(1f),
                            beyondViewportPageCount = 1
                        ) { pageIdx ->
                            val page = bookPages.getOrNull(pageIdx) ?: return@HorizontalPager
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Text(
                                    text = page.chapterTitle,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = Color(0xFFFF006E),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                                )

                                Text(
                                    text = page.content,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontFamily = if (book?.language == "en") FontFamily.Default else NotoSerif,
                                        lineHeight = 26.sp,
                                        fontSize = 17.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
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
}

private fun stripHtmlWithBreaks(html: String): String {
    val document = Jsoup.parse(html)
    document.outputSettings(org.jsoup.nodes.Document.OutputSettings().prettyPrint(false))
    document.select("br").append("\\n")
    document.select("p").prepend("\\n\\n")
    val s = document.text().replace("\\n", "\n")
    return s
}

private fun stripHtml(html: String): String {
    return Jsoup.parse(html).text()
}
