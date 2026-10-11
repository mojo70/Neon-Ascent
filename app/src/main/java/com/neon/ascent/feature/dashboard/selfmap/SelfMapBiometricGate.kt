package com.neon.ascent.feature.dashboard.selfmap

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.util.BiometricAuthManager

fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun SelfMapBiometricGate(
    isLockEnabled: Boolean,
    onUnlockSuccess: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = LocalNeonTheme.current
    val danger = theme.accentDanger
    val context = LocalContext.current
    val biometricAuthManager = remember { BiometricAuthManager(context) }

    var isUnlocked by remember(isLockEnabled) { mutableStateOf(!isLockEnabled) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerAuth() {
        val activity = context.findFragmentActivity()
        if (activity != null && biometricAuthManager.canAuthenticate()) {
            errorMessage = null
            biometricAuthManager.authenticate(
                activity = activity,
                title = "SELF MAP CLEARANCE REQUIRED",
                subtitle = "Verify identity to access neural profile & reaction telemetry",
                onSuccess = {
                    errorMessage = null
                    isUnlocked = true
                    onUnlockSuccess()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        } else {
            // Fallback for devices without biometric hardware or in preview
            isUnlocked = true
            onUnlockSuccess()
        }
    }

    LaunchedEffect(isLockEnabled) {
        if (isLockEnabled && !isUnlocked) {
            triggerAuth()
        }
    }

    if (!isLockEnabled || isUnlocked) {
        content()
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberFrame(
                label = "SELF_MAP // BIOMETRIC_NODE_LOCKED",
                borderColor = danger,
                accentColor = danger
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked Node",
                            tint = danger,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "PERSONAL ATTACHMENTS ENCRYPTED",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Biometric node lock is active. Authentication is required to decrypt your Self Map and emotional reaction history.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = danger,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = { triggerAuth() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = danger,
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "AUTHENTICATE NODE",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
