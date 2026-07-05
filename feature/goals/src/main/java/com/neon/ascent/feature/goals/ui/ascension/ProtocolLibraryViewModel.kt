package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.Protocol
import com.neon.ascent.core.domain.repository.ProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProtocolLibraryUiState(
    val protocols: List<Protocol> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ProtocolLibraryViewModel @Inject constructor(
    private val protocolRepository: ProtocolRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProtocolLibraryUiState> = combine(
        protocolRepository.getAllProtocols(),
        _searchQuery,
        _selectedCategory
    ) { protocols, query, category ->
        val filtered = protocols.filter { protocol ->
            (category == null || protocol.category == category) &&
            (query.isBlank() || protocol.title.contains(query, ignoreCase = true) || protocol.description.contains(query, ignoreCase = true))
        }
        ProtocolLibraryUiState(
            protocols = filtered,
            searchQuery = query,
            selectedCategory = category,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProtocolLibraryUiState(isLoading = true))

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }
}
