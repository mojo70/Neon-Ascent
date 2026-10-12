package com.neon.ascent.feature.biohacking.ui.labs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.local.entity.SavedReconstitutionEntity
import com.neon.ascent.core.data.repository.ReconstitutionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReconstitutionViewModel @Inject constructor(
    private val repository: ReconstitutionRepository
) : ViewModel() {

    val savedMixes: StateFlow<List<SavedReconstitutionEntity>> = repository.savedMixes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveMix(
        name: String,
        vialMg: String,
        waterMl: String,
        concentrationMgPerMl: String,
        mixedDate: String? = null,
        notes: String? = null,
        componentsJson: String? = null
    ) {
        viewModelScope.launch {
            val entity = SavedReconstitutionEntity(
                name = name.ifBlank { "Mix ${System.currentTimeMillis() % 10000}" },
                vialMg = vialMg,
                waterMl = waterMl,
                concentrationMgPerMl = concentrationMgPerMl,
                mixedDate = mixedDate?.ifBlank { null },
                notes = notes?.ifBlank { null },
                componentsJson = componentsJson?.ifBlank { null }
            )
            repository.saveMix(entity)
        }
    }

    fun deleteMix(id: String) {
        viewModelScope.launch {
            repository.deleteMix(id)
        }
    }
}
