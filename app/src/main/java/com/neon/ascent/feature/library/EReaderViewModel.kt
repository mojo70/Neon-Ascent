package com.neon.ascent.feature.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.SettingsRepository
import com.neon.ascent.feature.biohacking.GeminiNanoClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EReaderViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val geminiNanoClient: GeminiNanoClient
) : AndroidViewModel(application) {

    private val _currentBook = MutableStateFlow<EBook?>(null)
    val currentBook: StateFlow<EBook?> = _currentBook

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
                val inputStream = getApplication<Application>().assets.open(assetPath)
                _currentBook.value = bookParser.parseEpub(inputStream, id)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getSavedProgress(bookId: String): Int {
        return settingsRepository.getBookProgress(bookId)
    }

    fun saveProgress(bookId: String, index: Int) {
        settingsRepository.saveBookProgress(bookId, index)
    }
}
