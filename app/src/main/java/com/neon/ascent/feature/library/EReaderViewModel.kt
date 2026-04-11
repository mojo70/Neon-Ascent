package com.neon.ascent.feature.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EReaderViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _currentBook = MutableStateFlow<EBook?>(null)
    val currentBook: StateFlow<EBook?> = _currentBook

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val bookParser = BookParser(application)

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
}
