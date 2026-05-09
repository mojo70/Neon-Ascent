package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.lore.data.LoreRepository
import com.neon.ascent.core.lore.data.Megacorp
import com.neon.ascent.core.lore.data.QuickhackReward
import com.neon.ascent.data.local.InventoryDao
import com.neon.ascent.data.local.LoreDao
import com.neon.ascent.model.CorpoTrust
import com.neon.ascent.model.QuickHack
import com.neon.ascent.model.QuickHackType
import com.neon.ascent.model.Rarity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DossierViewModel @Inject constructor(
    private val loreRepository: LoreRepository,
    private val loreDao: LoreDao,
    private val inventoryDao: InventoryDao
) : ViewModel() {

    private val _corpoId = MutableStateFlow<String?>(null)
    
    val megacorp: StateFlow<Megacorp?> = _corpoId.map { id ->
        id?.let { loreRepository.getMegacorp(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trustLevel: StateFlow<Float> = _corpoId.flatMapLatest { id ->
        if (id == null) flowOf(0f)
        else loreDao.getCorpoTrust(id).map { it?.trustLevel ?: 0f }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun loadCorp(id: String) {
        _corpoId.value = id
        viewModelScope.launch {
            // Ensure trust entry exists
            val current = loreDao.getCorpoTrust(id).first()
            if (current == null) {
                loreDao.insertCorpoTrust(CorpoTrust(corpoId = id, trustLevel = 0f))
            }
        }
    }
    
    fun increaseTrust(amount: Float) {
        val id = _corpoId.value ?: return
        viewModelScope.launch {
            val current = trustLevel.value
            loreDao.updateTrustLevel(id, (current + amount).coerceIn(0f, 1f))
        }
    }

    fun claimReward(reward: QuickhackReward) {
        viewModelScope.launch {
            val qh = QuickHack(
                id = reward.id,
                name = reward.name,
                description = reward.description,
                rarity = Rarity.valueOf(reward.rarity),
                type = QuickHackType.valueOf(reward.type),
                cooldownHours = 24
            )
            inventoryDao.insertQuickHack(qh)
        }
    }
}
