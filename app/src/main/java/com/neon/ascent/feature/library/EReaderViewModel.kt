package com.neon.ascent.feature.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.BookDao
import com.neon.ascent.data.repository.SettingsRepository
import com.neon.ascent.feature.biohacking.GeminiNanoClient
import com.neon.ascent.model.BookEntity
import com.neon.ascent.model.ChapterEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject

@HiltViewModel
class EReaderViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val geminiNanoClient: GeminiNanoClient,
    private val bookDao: BookDao
) : AndroidViewModel(application) {

    private val _currentBook = MutableStateFlow<EBook?>(null)
    val currentBook: StateFlow<EBook?> = _currentBook

    private val _bookPages = MutableStateFlow<List<ChapterPage>>(emptyList())
    val bookPages: StateFlow<List<ChapterPage>> = _bookPages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val bookParser = BookParser(application)

    fun askAi(query: String, contextText: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = "CONNECTING_TO_NEURAL_LINK..."
            
            val prompt = """
                You are an advanced Cyberpunk Bible Scholar AI. 
                Context from the current reading: $contextText
                
                Question: $query
                
                Provide a concise, insightful answer in a cyberpunk terminal style.
            """.trimIndent()
            
            val response = geminiNanoClient.generateContent(prompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun clearAiResponse() {
        _aiResponse.value = null
    }

    fun loadBookFromAssets(assetPath: String, id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Check if book exists in DB
                val cachedBook = withContext(Dispatchers.IO) {
                    bookDao.getBookById(id)
                }

                if (cachedBook != null) {
                    // 2. Load from DB
                    val cachedChapters = withContext(Dispatchers.IO) {
                        bookDao.getChaptersForBook(id)
                    }
                    _currentBook.value = EBook(
                        id = cachedBook.id,
                        title = cachedBook.title,
                        author = cachedBook.author,
                        language = cachedBook.language,
                        chapters = cachedChapters.map { Chapter(it.title, it.content) }
                    )
                } else {
                    // 3. Parse and Save to DB
                    val inputStream = getApplication<Application>().assets.open(assetPath)
                    val parsedBook = withContext(Dispatchers.IO) {
                        bookParser.parseEpub(inputStream, id)
                    }
                    
                    withContext(Dispatchers.IO) {
                        bookDao.insertFullBook(
                            BookEntity(parsedBook.id, parsedBook.title, parsedBook.author, parsedBook.language),
                            parsedBook.chapters.mapIndexed { index, chapter ->
                                ChapterEntity(bookId = parsedBook.id, chapterIndex = index, title = chapter.title, content = chapter.content)
                            }
                        )
                    }
                    _currentBook.value = parsedBook
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePagination(book: EBook, maxWidth: Float, maxHeight: Float, bookId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val linesPerPage = ((maxHeight - 180) / 34).toInt().coerceAtLeast(1)
            val charsPerLine = (maxWidth / 12).toInt().coerceAtLeast(1)
            val estimatedCharsPerPage = (linesPerPage * charsPerLine).coerceIn(200, 2000)

            val isBilingual = bookId == "ot_word"

            val pages = book.chapters.flatMapIndexed { chapterIndex, chapter ->
                val text = stripHtmlWithBreaks(chapter.content)
                val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
                
                if (isBilingual) {
                    val enParas = mutableListOf<String>()
                    val heParas = mutableListOf<String>()
                    
                    for (para in paragraphs) {
                        val enLines = mutableListOf<String>()
                        val heLines = mutableListOf<String>()
                        
                        val lines = para.split("\n").filter { it.isNotBlank() }
                        for (line in lines) {
                            val hasHebrew = line.any { it in '\u0590'..'\u05FF' }
                            val hasEnglish = line.any { it in 'a'..'z' || it in 'A'..'Z' }
                            
                            if (hasEnglish) {
                                val cleaned = line.filter { it !in '\u0590'..'\u05FF' }.trim()
                                if (cleaned.isNotEmpty()) enLines.add(cleaned)
                            }
                            
                            if (hasHebrew) {
                                val cleaned = line.filter { it !in 'a'..'z' && it !in 'A'..'Z' }.trim()
                                if (cleaned.isNotEmpty()) heLines.add(cleaned)
                            }
                            
                            if (!hasEnglish && !hasHebrew) {
                                val cleaned = line.trim()
                                if (cleaned.isNotEmpty()) {
                                    enLines.add(cleaned)
                                    heLines.add(cleaned)
                                }
                            }
                        }
                        
                        if (enLines.isNotEmpty()) enParas.add(enLines.joinToString("\n"))
                        if (heLines.isNotEmpty()) heParas.add(heLines.joinToString("\n"))
                    }
                    
                    val enPages = paginateParas(enParas, estimatedCharsPerPage)
                    val hePages = paginateParas(heParas, estimatedCharsPerPage)
                    
                    val interleaved = mutableListOf<ChapterPage>()
                    val maxPageCount = maxOf(enPages.size, hePages.size)
                    for (i in 0 until maxPageCount) {
                        if (i < enPages.size) {
                            interleaved.add(ChapterPage(chapterIndex, chapter.title, enPages[i], isRtl = false))
                        }
                        if (i < hePages.size) {
                            interleaved.add(ChapterPage(chapterIndex, chapter.title, hePages[i], isRtl = true))
                        }
                    }
                    if (interleaved.isEmpty()) listOf(ChapterPage(chapterIndex, chapter.title, "[EMPTY_CHAPTER]"))
                    else interleaved
                } else {
                    val pages = paginateParas(paragraphs, estimatedCharsPerPage)
                    val isRtl = book.language == "he"
                    if (pages.isEmpty()) listOf(ChapterPage(chapterIndex, chapter.title, "[EMPTY_CHAPTER]", isRtl = isRtl))
                    else pages.map { content ->
                        ChapterPage(chapterIndex, chapter.title, content, isRtl = isRtl)
                    }
                }
            }
            _bookPages.value = pages
        }
    }

    private fun paginateParas(paragraphs: List<String>, estimatedCharsPerPage: Int): List<String> {
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
                    if (availableSpace < estimatedCharsPerPage / 5 && currentPageText.isNotEmpty()) {
                        pages.add(currentPageText.toString().trim())
                        currentPageText = StringBuilder()
                    } else {
                        var splitIndex = remainingPara.lastIndexOf(' ', availableSpace)
                        if (splitIndex == -1) {
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
        return pages
    }

    private fun stripHtmlWithBreaks(html: String): String {
        val document = Jsoup.parse(html)
        document.outputSettings(org.jsoup.nodes.Document.OutputSettings().prettyPrint(false))
        document.select("br").append(" BR_MARKER ")
        document.select("p, div, tr, td, th, li").prepend(" PARA_MARKER ")
        val s = document.text()
            .replace("BR_MARKER", "\n")
            .replace("PARA_MARKER", "\n\n")
            .replace(Regex("\n +"), "\n")
            .replace(Regex(" +\n"), "\n")
            .trim()
        return s
    }

    fun getSavedProgress(bookId: String): Int {
        return settingsRepository.getBookProgress(bookId)
    }

    fun saveProgress(bookId: String, index: Int) {
        settingsRepository.saveBookProgress(bookId, index)
    }
}
