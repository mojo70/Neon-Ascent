package com.neon.ascent.feature.sanctuary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.sanctuary.models.SanctuaryPassage
import com.neon.ascent.core.domain.sanctuary.repository.SanctuaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SanctuaryUiState(
    val passages: List<SanctuaryPassage> = emptyList(),
    val themes: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val selectedTheme: String? = null,
    val selectedTag: String? = null,
    val searchQuery: String = "",
    val isDefaultDeckOnly: Boolean = false,
    val highlightedPassageId: String? = null,
    val isLoading: Boolean = false
)

private data class SanctuaryFilterState(
    val theme: String?,
    val tag: String?,
    val query: String,
    val defaultOnly: Boolean,
    val highlightedId: String?
)

@HiltViewModel
class SanctuaryViewModel @Inject constructor(
    private val sanctuaryRepository: SanctuaryRepository
) : ViewModel() {

    private val _selectedTheme = MutableStateFlow<String?>(null)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isDefaultDeckOnly = MutableStateFlow(false)
    private val _highlightedPassageId = MutableStateFlow<String?>(null)

    private val _filters = combine(
        _selectedTheme,
        _selectedTag,
        _searchQuery,
        _isDefaultDeckOnly,
        _highlightedPassageId
    ) { theme, tag, query, defaultOnly, highlightedId ->
        SanctuaryFilterState(theme, tag, query, defaultOnly, highlightedId)
    }

    val uiState: StateFlow<SanctuaryUiState> = combine(
        sanctuaryRepository.observeAllPassages(),
        _filters
    ) { allPassages, filters ->
        val allThemes = allPassages.flatMap { it.themes }.distinct().sorted()
        val allTags = allPassages.flatMap { it.tags }.distinct().sorted()

        var filtered = allPassages

        if (filters.defaultOnly) {
            filtered = filtered.filter { it.isFavoriteDefault }
        }

        if (!filters.theme.isNullOrBlank()) {
            filtered = filtered.filter { passage ->
                passage.themes.any { it.equals(filters.theme, ignoreCase = true) }
            }
        }

        if (!filters.tag.isNullOrBlank()) {
            filtered = filtered.filter { passage ->
                passage.tags.any { it.equals(filters.tag, ignoreCase = true) }
            }
        }

        if (filters.query.isNotBlank()) {
            val q = filters.query.trim().lowercase()
            filtered = filtered.filter { passage ->
                passage.reference.lowercase().contains(q) ||
                        (passage.title?.lowercase()?.contains(q) == true) ||
                        passage.text.lowercase().contains(q) ||
                        passage.book.lowercase().contains(q)
            }
        }

        SanctuaryUiState(
            passages = filtered,
            themes = allThemes,
            tags = allTags,
            selectedTheme = filters.theme,
            selectedTag = filters.tag,
            searchQuery = filters.query,
            isDefaultDeckOnly = filters.defaultOnly,
            highlightedPassageId = filters.highlightedId,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SanctuaryUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            sanctuaryRepository.seedDefaultPassagesIfAbsent()
        }
    }

    fun selectTheme(theme: String?) {
        _selectedTheme.value = if (_selectedTheme.value == theme) null else theme
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleDefaultDeckOnly() {
        _isDefaultDeckOnly.value = !_isDefaultDeckOnly.value
    }

    fun showDefaultDeck() {
        _isDefaultDeckOnly.value = true
        _selectedTheme.value = null
        _selectedTag.value = null
        _searchQuery.value = ""
    }

    fun showAllPassages() {
        _isDefaultDeckOnly.value = false
    }

    fun highlightPassage(passageId: String?) {
        _highlightedPassageId.value = passageId
    }

    fun clearFilters() {
        _selectedTheme.value = null
        _selectedTag.value = null
        _searchQuery.value = ""
        _isDefaultDeckOnly.value = false
    }
}
