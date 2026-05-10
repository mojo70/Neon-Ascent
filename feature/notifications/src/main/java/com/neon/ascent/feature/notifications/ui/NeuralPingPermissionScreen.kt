package com.neon.ascent.feature.notifications.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink

@Composable
fun NeuralPingPermissionScreen(
    onGranted: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: NotificationPermissionViewModel = hiltViewModel()
) {
    val hasPermission by viewModel.hasPermission.collectAsState()

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    if (hasPermission) {
        LaunchedEffect(Unit) {
            onGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NEURAL PING PROTOCOL",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan
        )

        Text(
            text = "Allow Neon-Ascent to send deck messages?",
            style = MaterialTheme.typography.titleMedium,
            color = NeonPink
        )

        // Rationale Cards
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RationaleItem(
                icon = "🔔",
                title = "Mission Reminders",
                description = "Daily & contextual pings when protocols are ready"
            )
            RationaleItem(
                icon = "⚡",
                title = "Streak Protection",
                description = "Urgent warnings when you're about to break a streak"
            )
            RationaleItem(
                icon = "📡",
                title = "System Status",
                description = "Health sync complete, attribute level-ups, etc."
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "All pings are fully customizable and can be disabled anytime.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("NOT NOW")
            }

            Button(
                onClick = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onPermissionResult(true)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("ENABLE NEURAL PINGS")
            }
        }
    }
}

@Composable
private fun RationaleItem(icon: String, title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0033))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = NeonCyan)
                Text(description, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
