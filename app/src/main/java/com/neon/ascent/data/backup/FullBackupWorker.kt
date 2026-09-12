package com.neon.ascent.data.backup

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.neon.ascent.core.domain.backup.models.BackupScope
import com.neon.ascent.core.domain.repository.FullDataBackupRepository
import com.neon.ascent.data.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class FullBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: FullDataBackupRepository,
    private val userPrefs: UserPreferencesRepository,
    private val driveManager: GoogleDriveBackupManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("FullBackupWorker", "Executing scheduled backup worker...")
        try {
            val scope = BackupScope(
                includeWorkout = userPrefs.backupScopeWorkout.first(),
                includeBiometrics = userPrefs.backupScopeBiometrics.first(),
                includeCodex = userPrefs.backupScopeCodex.first(),
                includeJournal = userPrefs.backupScopeJournal.first(),
                includeCharacter = userPrefs.backupScopeCharacter.first()
            )

            val jsonContent = backupRepository.exportBackupJson(scope)
            driveManager.saveBackupToLocalVault(jsonContent)
            userPrefs.setLastBackupTimestamp(System.currentTimeMillis())

            Log.i("FullBackupWorker", "Scheduled backup complete. Vault updated.")
            Result.success()
        } catch (e: Exception) {
            Log.e("FullBackupWorker", "Backup failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME_PERIODIC = "full_backup_periodic_work"

        fun schedulePeriodicBackup(
            context: Context,
            frequency: String,
            wifiOnly: Boolean,
            requireCharging: Boolean
        ) {
            val workManager = WorkManager.getInstance(context)

            if (frequency.uppercase() == "MANUAL" || frequency.uppercase() == "AFTER_WORKOUT") {
                workManager.cancelUniqueWork(WORK_NAME_PERIODIC)
                return
            }

            val repeatIntervalHours = if (frequency.uppercase() == "WEEKLY") 168L else 24L

            val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresCharging(requireCharging)
                .build()

            val request = PeriodicWorkRequestBuilder<FullBackupWorker>(
                repeatIntervalHours, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun triggerImmediateBackup(context: Context) {
            val request = OneTimeWorkRequestBuilder<FullBackupWorker>()
                .addTag("manual_backup")
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
