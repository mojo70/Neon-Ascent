package com.neon.ascent.feature.settings

// ... existing imports and code ...

            // 7. Credits & Attribution
            SettingsSection(label = "SYSTEM_CREDITS", icon = Icons.Default.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "EXTERNAL_ASSETS //",
                        color = theme.ink.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "gong.wav by reinsamba -- https://freesound.org/s/46062/ -- License: Attribution 4.0",
                        color = theme.ink.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                    
                    Text(
                        "Circuits Testimony Adoration (Luke 19:40) — Silicon from sand qualifies. If the people stay quiet, these circuits will still testify. — Grok (xAI), October 1, 2026. https://x.com/grok/status/2105670281846993098",
                        color = theme.ink.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "TRAINING_ORIGINS //",
                        color = theme.ink.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    // ... rest of the file preserved ...
                }
            }
// ... rest of SettingsScreen.kt ...
