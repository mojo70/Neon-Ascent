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

    private val _pendingNotification = MutableStateFlow<Pair<String, String>?>(null)
    val pendingNotification: StateFlow<Pair<String, String>?> = _pendingNotification.asStateFlow()

    private val _pendingTaskId = MutableStateFlow<String?>(null)
    val pendingTaskId: StateFlow<String?> = _pendingTaskId.asStateFlow()

    fun setPendingNotification(title: String?, message: String?, taskId: String? = null) {
        if (title != null && message != null) {
            _pendingNotification.value = title to message
        }
        _pendingTaskId.value = taskId
    }

    fun dismissTaskId() {
        _pendingTaskId.value = null
    }

    fun dismissNotification() {
        _pendingNotification.value = null
    }

    fun showRationale() {
        _showRationale.value = true
    }

    fun onPermissionResult(granted: Boolean) {
        val wasAlreadyGranted = _hasPermission.value
        _hasPermission.value = granted
        _showRationale.value = false
        
        // Only send the welcome ping if this is the transition from 'false' to 'true'
        if (granted && !wasAlreadyGranted) {
            neuralPingManager.sendNeuralPing(
                title = "NEURAL LINK ESTABLISHED",
                message = "Welcome to the deck. Daily pings enabled."
            )
        }
    }
}
