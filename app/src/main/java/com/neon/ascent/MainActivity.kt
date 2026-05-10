package com.neon.ascent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.neon.ascent.feature.notifications.ui.NotificationPermissionViewModel
import com.neon.ascent.ui.theme.NeonAscentTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var healthRepository: HealthRepository

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
        
        // Schedule daily health sync
        HealthSyncWorker.scheduleDailySync(this)

        // Check and request notification permission
        checkAndRequestNotificationPermission()

        setContent {
            NeonAscentTheme {
                val context = LocalContext.current
                val viewModel: DashboardViewModel = hiltViewModel()
                val healthState by viewModel.healthState.collectAsState()

                val permissionsLauncher = rememberLauncherForActivityResult(
                    PermissionController.createRequestPermissionResultContract()
                ) { granted ->
                    viewModel.refreshHealthData()
                }

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    ) {
                        viewModel.fetchRealWeather()
                    }
                }

                LaunchedEffect(Unit) {
                    if (!healthRepository.hasAllPermissions()) {
                        permissionsLauncher.launch(healthRepository.permissions)
                    }

                    val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasFineLocation || hasCoarseLocation) {
                        viewModel.fetchRealWeather()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
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
}
