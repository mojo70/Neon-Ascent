package com.neon.ascent.feature.notifications.ui

import androidx.lifecycle.ViewModel
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import com.neon.ascent.feature.notifications.data.NotificationPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    private val permissionManager: NotificationPermissionManager,
    private val neuralPingManager: NeuralPingManager
) : ViewModel() {

    private val _hasPermission = MutableStateFlow(permissionManager.hasPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _showRationale = MutableStateFlow(false)
    val showRationale: StateFlow<Boolean> = _showRationale.asStateFlow()

    fun showRationale() {
        _showRationale.value = true
    }

    fun onPermissionResult(granted: Boolean) {
        _hasPermission.value = granted
        _showRationale.value = false
        if (granted) {
            neuralPingManager.sendNeuralPing(
                title = "NEURAL LINK ESTABLISHED",
                message = "Welcome to the deck. Daily pings enabled."
            )
        }
    }
}
