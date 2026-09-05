package com.neon.ascent.feature.health.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.checkPermissions()
        // We'll let the LaunchedEffect handle onPermissionsGranted if successful
    }

    LaunchedEffect(state.hasPermissions) {
        if (state.hasPermissions) {
            onPermissionsGranted()
        }
    }

    if (state.showRationale) {
        PermissionRationaleDialog(
            rationale = state.rationale,
            onConfirm = {
                viewModel.dismissRationale()
                scope.launch {
                    val permissions = viewModel.healthManager.getPermissionsToRequest()
                    permissionsLauncher.launch(permissions)
                }
            },
            onDismiss = { viewModel.dismissRationale() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020202))
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
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

        state.rationale.ifEmpty { viewModel.healthManager.getPermissionRationale() }.forEach { (_, explanation) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = NeonPink)
                Spacer(Modifier.width(12.dp))
                Text(text = explanation, color = Color.White.copy(alpha = 0.9f))
            }
        }

        Button(
            onClick = {
                viewModel.requestHealthPermissions()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
            } else {
                Text("CONNECT TO HEALTH VAULT")
            }
        }

        if (state.hasPermissions) {
            Button(
                onClick = { viewModel.triggerImmediateSync() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("FORCE NEURAL SYNC (GARMIN)")
            }
        }

        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Text(
            text = "You can revoke access anytime in Health Connect settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun PermissionRationaleDialog(
    rationale: Map<String, String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Health Connect Permissions Required", 
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Health Connect needs permissions to sync your biometric data. Would you like to add permissions now?",
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(4.dp))
                rationale.forEach { (_, explanation) ->
                    Text(
                        "• $explanation", 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("ADD PERMISSIONS", color = NeonPink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}
