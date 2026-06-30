package com.neon.ascent.feature.biohacking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.model.DopamineCategory
import com.neon.ascent.core.domain.model.DopamineMenuItem
import com.neon.ascent.core.domain.model.EnergyLevel
import com.neon.ascent.core.domain.repository.DopamineMenuRepository
import com.neon.ascent.feature.biohacking.AiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.*
import javax.inject.Inject

@Serializable
data class GeneratedDopamineItem(
    val title: String,
    val description: String,
    val duration: Int,
    val category: String,
    val energy: String
)

@HiltViewModel
class DopamineMenuViewModel @Inject constructor(
    private val repository: DopamineMenuRepository,
    private val aiProvider: AiProvider
) : ViewModel() {

    private val _selectedEnergy = MutableStateFlow<EnergyLevel?>(null)
    val selectedEnergy: StateFlow<EnergyLevel?> = _selectedEnergy.asStateFlow()

    private val _selectedCategory = MutableStateFlow<DopamineCategory?>(null)
    val selectedCategory: StateFlow<DopamineCategory?> = _selectedCategory.asStateFlow()

    val menuItems: StateFlow<List<DopamineMenuItem>> = combine(
        repository.getAllItems(),
        _selectedEnergy,
        _selectedCategory
    ) { items, energy, category ->
        items.filter { item ->
            (energy == null || item.energyLevel == energy) &&
            (category == null || item.category == category)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun setEnergyFilter(energy: EnergyLevel?) {
        _selectedEnergy.value = energy
    }

    fun setCategoryFilter(category: DopamineCategory?) {
        _selectedCategory.value = category
    }

    fun logCompletion(item: DopamineMenuItem) {
        viewModelScope.launch {
            repository.logUsage(item.id, Instant.now())
        }
    }

    fun generateNewSuggestions() {
        viewModelScope.launch {
            _isGenerating.value = true
            val prompt = """
                GENERATE_DOPAMINE_MENU_ITEMS
                CONTEXT: Cyberpunk health/productivity app.
                FORMAT: Return 3 new items in JSON format: [{"title": "...", "description": "...", "duration": Int, "category": "RESET|MOVEMENT|CREATIVE|SENSORY|PRODUCTIVE|SOCIAL", "energy": "LOW|MEDIUM|HIGH"}]
                STRICTURE: High impact, low friction, science-based for ADHD/Recovery.
            """.trimIndent()

            val result = aiProvider.generateContent(prompt)
            // Parse and save items (simplified for now)
            parseAndSaveGeneratedItems(result)
            _isGenerating.value = false
        }
    }

    private suspend fun parseAndSaveGeneratedItems(json: String) {
        try {
            val jsonCleaned = json.substringAfter("[").substringBeforeLast("]")
            val items = Json.decodeFromString<List<GeneratedDopamineItem>>("[$jsonCleaned]")
            items.forEach { gen ->
                val item = DopamineMenuItem(
                    id = UUID.randomUUID().toString(),
                    title = gen.title,
                    description = gen.description,
                    durationMinutes = gen.duration,
                    category = try { DopamineCategory.valueOf(gen.category) } catch (e: Exception) { DopamineCategory.RESET },
                    specialTags = emptyList(),
                    energyLevel = try { EnergyLevel.valueOf(gen.energy) } catch (e: Exception) { EnergyLevel.MEDIUM }
                )
                repository.upsertItem(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seedDefaultItems() {
        viewModelScope.launch {
            val defaults = listOf(
                DopamineMenuItem(UUID.randomUUID().toString(), "Deep Breathing", "5-5-5 Box breathing for CNS reset.", 2, DopamineCategory.RESET, emptyList(), EnergyLevel.LOW),
                DopamineMenuItem(UUID.randomUUID().toString(), "Cold Splash", "Neural shock via cold water on face.", 1, DopamineCategory.RESET, emptyList(), EnergyLevel.LOW),
                DopamineMenuItem(UUID.randomUUID().toString(), "10 Push-ups", "Quick burst of physical activity.", 1, DopamineCategory.MOVEMENT, emptyList(), EnergyLevel.MEDIUM),
                DopamineMenuItem(UUID.randomUUID().toString(), "Sunlight Exposure", "Reset circadian rhythm.", 5, DopamineCategory.SENSORY, emptyList(), EnergyLevel.LOW),
                DopamineMenuItem(UUID.randomUUID().toString(), "Journal Win", "Log one small success.", 3, DopamineCategory.PRODUCTIVE, emptyList(), EnergyLevel.LOW)
            )
            defaults.forEach { repository.upsertItem(it) }
        }
    }
}
