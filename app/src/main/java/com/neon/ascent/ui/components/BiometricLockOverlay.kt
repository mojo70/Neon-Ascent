package com.neon.ascent.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun BiometricLockOverlay(
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = LocalNeonTheme.current
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerAuth() {
        val activity = context.findFragmentActivity()
        if (activity != null) {
            errorMessage = null
            biometricAuthManager.authenticate(
                activity = activity,
                title = "NODE UNLOCK REQUIRED",
                subtitle = "Verify identity to access neural profile",
                onSuccess = {
                    errorMessage = null
                    onUnlockSuccess()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        } else {
            errorMessage = "FragmentActivity unavailable for biometrics"
        }
    }

    LaunchedEffect(Unit) {
        triggerAuth()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CyberFrame(
            label = "NEURAL_NODE_LOCKED // CLEARANCE_REQUIRED",
            borderColor = theme.accentDanger,
            accentColor = theme.accent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Node",
                        tint = theme.accentDanger,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "NEURAL LINK ENCRYPTED",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Biometric node lock is active. Authentication required to decrypt system vault and personal telemetry.",
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
                            color = theme.accentDanger,
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
                        containerColor = theme.accentDanger,
                        contentColor = Color.Black
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

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
