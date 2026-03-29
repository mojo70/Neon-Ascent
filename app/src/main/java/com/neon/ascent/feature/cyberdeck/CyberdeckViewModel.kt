package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.feature.biohacking.AiType
import com.neon.ascent.model.NetWatchAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CyberdeckViewModel @Inject constructor(
    private val aiProvider: AiProvider
) : ViewModel() {
    val aiType: StateFlow<AiType> = aiProvider.activeAiType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiType.NONE)

    private val _netWatchAlerts = MutableStateFlow<List<NetWatchAlert>>(emptyList())
    val netWatchAlerts: StateFlow<List<NetWatchAlert>> = _netWatchAlerts.asStateFlow()

    private val _externalFeeds = MutableStateFlow<List<String>>(emptyList())
    val combinedFeeds: StateFlow<List<String>> = combine(_netWatchAlerts, _externalFeeds) { alerts, external ->
        val alertStrings = alerts.map { "[${it.severity}] ${it.title}: ${it.description}" }
        (external + alertStrings).takeLast(15)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        generateAlerts()
    }

    fun setExternalFeeds(feeds: List<String>) {
        _externalFeeds.value = feeds
    }

    private fun generateAlerts() {
        viewModelScope.launch {
            val templates = listOf(
                "TRACE_DETECTED" to "Unauthorized access attempt in Subnet 7.",
                "BOUNTY_POSTED" to "Target: 'Ghost_Dog'. Reward: 50,000 Eddies.",
                "ICE_UPDATE" to "Arasaka updating firewall protocols in Watson.",
                "NET_WATCH_ADVISORY" to "High level of packet loss in the combat zone.",
                "SYSTEM_WARNING" to "Neural link instability detected in rank 3 operatives."
            )
            
            while (true) {
                val (title, desc) = templates.random()
                val severity = listOf("LOW", "MEDIUM", "CRITICAL").random()
                val newAlert = NetWatchAlert(title = title, description = desc, severity = severity)
                
                _netWatchAlerts.update { (it + newAlert).takeLast(10) }
                delay(Random.nextLong(10000, 30000)) // New alert every 10-30 seconds
            }
        }
    }
}
