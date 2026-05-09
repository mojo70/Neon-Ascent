package com.neon.ascent.feature.health

import android.content.Context
import com.neon.ascent.core.data.datastore.HealthPreferencesDataStore
import com.neon.ascent.core.domain.special.usecases.UpdateSpecialFromHealthUseCase
import com.neon.ascent.feature.health.data.workers.HealthSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/**
 * Orchestrates a health data sync from Health Connect.
 * Triggers both a background worker for robustness and an immediate UseCase for UI feedback.
 */
class HealthSyncUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateSpecialFromHealthUseCase: UpdateSpecialFromHealthUseCase,
    private val healthPrefs: HealthPreferencesDataStore
) {
    suspend operator fun invoke() {
        // 1. One-time manual sync via Worker (background robustness)
        HealthSyncWorker.triggerImmediateSync(context)

        // 2. Run immediate UseCase for instant feedback in the current session
        updateSpecialFromHealthUseCase()

        // 3. Update the last sync time tracking
        healthPrefs.updateLastSyncTime(Instant.now())
    }
}
