package com.neon.ascent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.dashboard.DashboardViewModel
import com.neon.ascent.data.repository.HealthRepository
import com.neon.ascent.feature.health.data.workers.HealthSyncWorker
import com.neon.ascent.feature.notifications.data.NeuralBriefManager
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
import com.neon.ascent.feature.notifications.ui.NotificationPermissionViewModel
import com.neon.ascent.ui.theme.NeonAscentTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var healthRepository: HealthRepository

    @Inject
    lateinit var smartPingScheduler: SmartPingScheduler

    private val notificationViewModel: NotificationPermissionViewModel by viewModels()

    // ActivityResultLauncher for POST_NOTIFICATIONS permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        notificationViewModel.onPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Schedule periodic health sync
        HealthSyncWorker.schedulePeriodicSync(this)

        // Initialize smart notification scheduling with safety
        val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("MainActivity", "Uncaught exception in lifecycleScope", throwable)
        }
        
        lifecycleScope.launch(exceptionHandler) {
            try {
                smartPingScheduler.scheduleSmartPings()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to schedule smart pings", e)
            }
        }

        // Check and request notification permission
        checkAndRequestNotificationPermission()

        setContent {
            NeonAscentTheme {
                val context = LocalContext.current

                val permissionsLauncher = rememberLauncherForActivityResult(
                    PermissionController.createRequestPermissionResultContract()
                ) { granted ->
                    // Permissions updated
                }

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    // Permissions updated
                }

                val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    // Handle bluetooth permissions result
                }

                val notificationTitle = intent.getStringExtra(NeuralBriefManager.EXTRA_NOTIFICATION_TITLE)
                val notificationMessage = intent.getStringExtra(NeuralBriefManager.EXTRA_NOTIFICATION_MESSAGE)
                val notificationTaskId = intent.getStringExtra(com.neon.ascent.feature.notifications.data.NeuralPingManager.EXTRA_TASK_ID)
                
                LaunchedEffect(notificationTitle, notificationMessage, notificationTaskId) {
                    notificationViewModel.setPendingNotification(notificationTitle, notificationMessage, notificationTaskId)
                }

                LaunchedEffect(Unit) {
                    // Sequentially check and request permissions to avoid collisions
                    if (!healthRepository.hasAllPermissions()) {
                        permissionsLauncher.launch(healthRepository.permissions)
                        // Give some buffer for the OS dialog to appear
                        kotlinx.coroutines.delay(1000)
                    }

                    val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    
                    if (!hasFineLocation && !hasCoarseLocation) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        kotlinx.coroutines.delay(1000)
                    }

                    // For Bluetooth, combined with location if needed on older versions, 
                    // but minSdk 31 handles it separately
                    checkAndRequestBluetoothPermissions(bluetoothPermissionLauncher)
                }

                AppNavigation(notificationViewModel = notificationViewModel)
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return // No permission needed
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            notificationViewModel.onPermissionResult(true)
            return
        }

        // Show rationale screen first if needed
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            notificationViewModel.showRationale()
        } else {
            // Direct request
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        val notificationTitle = intent.getStringExtra(NeuralBriefManager.EXTRA_NOTIFICATION_TITLE)
        val notificationMessage = intent.getStringExtra(NeuralBriefManager.EXTRA_NOTIFICATION_MESSAGE)
        val notificationTaskId = intent.getStringExtra(com.neon.ascent.feature.notifications.data.NeuralPingManager.EXTRA_TASK_ID)
        notificationViewModel.setPendingNotification(notificationTitle, notificationMessage, notificationTaskId)
    }

    private fun checkAndRequestBluetoothPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            launcher.launch(missingPermissions.toTypedArray())
        }
    }
}
