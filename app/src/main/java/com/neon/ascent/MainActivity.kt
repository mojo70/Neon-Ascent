package com.neon.ascent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.dashboard.DashboardViewModel
import com.neon.ascent.data.repository.HealthRepository
import com.neon.ascent.ui.theme.NeonAscentTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var healthRepository: HealthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeonAscentTheme {
                val viewModel: DashboardViewModel = hiltViewModel()
                val healthState by viewModel.healthState.collectAsState()

                val permissionsLauncher = rememberLauncherForActivityResult(
                    PermissionController.createRequestPermissionResultContract()
                ) { granted ->
                    viewModel.refreshHealthData()
                }

                LaunchedEffect(Unit) {
                    if (!healthRepository.hasAllPermissions()) {
                        permissionsLauncher.launch(healthRepository.permissions)
                    }
                }

                AppNavigation()
            }
        }
    }
}
