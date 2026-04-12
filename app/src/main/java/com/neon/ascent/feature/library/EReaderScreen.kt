package com.neon.ascent.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.input.ImeAction
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
    val content: String,
    val isRtl: Boolean = false
)

@Composable
fun EReaderScreen(
    viewModel: EReaderViewModel = hiltViewModel(),
    bookAssetPath: String,
    bookId: String,
    onBack: () -> Unit
) {
    val book by viewModel.currentBook.collectAsState()
    val bookPages by viewModel.bookPages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var showNavDrawer by remember { mutableStateOf(false) }
    var showAiSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        LaunchedEffect(book, maxWidth, maxHeight, bookId) {
            val currentBook = book
            if (currentBook != null) {
                viewModel.updatePagination(currentBook, maxWidth.value, maxHeight.value, bookId)
            }
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

        Box(modifier = Modifier.fillMaxSize()) {
            CyberGridBackground()
            GlitchOverlay(intensity = 0.05f)

            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    IconButton(onClick = { showAiSearch = !showAiSearch }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Neural Link",
                            tint = if (showAiSearch) Color(0xFFFF006E) else Color(0xFF00FF9C)
                        )
                    }
                    IconButton(onClick = { showNavDrawer = true }) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Chapters",
                            tint = Color(0xFF00FF9C)
                        )
                    }
                }

                // AI Search Bar
                AnimatedVisibility(
                    visible = showAiSearch,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    CyberFrame(
                        label = "NEURAL_LINK",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ASK CYBER ORACLE...", color = Color(0xFF00FF9C).copy(alpha = 0.5f), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF00FF9C),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        val currentPage = bookPages.getOrNull(pagerState.currentPage)
                                        viewModel.askAi(searchQuery, currentPage?.content ?: "")
                                        keyboardController?.hide()
                                    }) {
                                        Icon(Icons.Default.Search, "Ask", tint = Color(0xFF00FF9C))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                val currentPage = bookPages.getOrNull(pagerState.currentPage)
                                viewModel.askAi(searchQuery, currentPage?.content ?: "")
                                keyboardController?.hide()
                            }),
                            singleLine = true
                        )
                    }
                }

                // AI Response Section
                AnimatedVisibility(
                    visible = aiResponse != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    CyberFrame(
                        label = "TERMINAL_OUTPUT",
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "CYBER_ORACLE_v1.0 > ",
                                    color = Color(0xFFFF006E),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearAiResponse() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = aiResponse ?: "",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (isAiLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    color = Color(0xFF00FF9C),
                                    trackColor = Color.DarkGray
                                )
                            }
                        }
                    }
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
                        val pageLayoutDirection = if (page.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                        
                        CompositionLocalProvider(LocalLayoutDirection provides pageLayoutDirection) {
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
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                                )

                                Text(
                                    text = page.content,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontFamily = if (page.isRtl) NotoSerif else FontFamily.Default,
                                        lineHeight = 26.sp,
                                        fontSize = 17.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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

            // Chapter Navigation Drawer (Overlay)
            if (showNavDrawer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable { showNavDrawer = false }
                ) {
                    CyberFrame(
                        label = "CHAPTER_NAV",
                        modifier = Modifier
                            .fillMaxHeight(0.7f)
                            .fillMaxWidth(0.85f)
                            .align(Alignment.Center)
                            .clickable(enabled = false) { }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "SELECT CHAPTER",
                                    color = Color(0xFF00FF9C),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { showNavDrawer = false }) {
                                    Icon(Icons.Default.Close, "Close", tint = Color(0xFF00FF9C))
                                }
                            }
                            
                            val chapters = book?.chapters ?: emptyList()
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(chapters.size) { index ->
                                    val chapter = chapters[index]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (bookPages.getOrNull(pagerState.currentPage)?.chapterIndex == index) Color(0xFF00FF9C).copy(alpha = 0.1f) else Color.Transparent)
                                            .border(0.5.dp, if (bookPages.getOrNull(pagerState.currentPage)?.chapterIndex == index) Color(0xFF00FF9C) else Color.DarkGray)
                                            .clickable {
                                                val firstPageIndex = bookPages.indexOfFirst { it.chapterIndex == index }
                                                if (firstPageIndex != -1) {
                                                    scope.launch {
                                                        pagerState.scrollToPage(firstPageIndex)
                                                        showNavDrawer = false
                                                    }
                                                }
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = chapter.title,
                                            color = if (bookPages.getOrNull(pagerState.currentPage)?.chapterIndex == index) Color(0xFF00FF9C) else Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
