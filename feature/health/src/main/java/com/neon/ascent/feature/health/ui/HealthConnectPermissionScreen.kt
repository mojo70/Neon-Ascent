package com.neon.ascent.feature.health.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import kotlinx.coroutines.launch

@Composable
fun HealthConnectPermissionScreen(
    viewModel: HealthViewModel = hiltViewModel(),
    onPermissionsGranted: () -> Unit
) {
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    val rationale = viewModel.healthManager.getPermissionRationale()
    val scope = rememberCoroutineScope()

    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(rationale.keys)) { // This is a simplification
            viewModel.checkPermissions()
            onPermissionsGranted()
        }
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            onPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "NEURAL LINK ESTABLISHMENT",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan
        )

        Text(
            text = "Grant access to your biometric streams.\n" +
                    "All data stays on-device. No cloud. No tracking.",
            color = Color.White.copy(alpha = 0.8f)
        )

        rationale.forEach { (_, explanation) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = NeonPink)
                Spacer(Modifier.width(12.dp))
                Text(text = explanation, color = Color.White.copy(alpha = 0.9f))
            }
        }

        Button(
            onClick = {
                scope.launch {
                    val permissions = viewModel.healthManager.getPermissionsToRequest()
                    if (permissions.isNotEmpty()) {
                        permissionsLauncher.launch(permissions)
                    } else {
                        onPermissionsGranted()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CONNECT TO HEALTH VAULT")
        }

        Text(
            text = "You can revoke access anytime in Health Connect settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
