package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.backup.models.BackupScope
import com.neon.ascent.core.domain.backup.models.RestoreMode
import com.neon.ascent.core.domain.backup.models.RestoreProgress
import com.neon.ascent.core.domain.backup.models.RestoreResult

/**
 * Domain contract for full application data export and restoration.
 * Supports v2, v3, v4, and v5 NeonAscentBackupPayload exports.
 */
interface FullDataBackupRepository {
    suspend fun exportBackupJson(scope: BackupScope): String
    suspend fun restoreBackupJson(
        jsonString: String,
        mode: RestoreMode = RestoreMode.MERGE,
        onProgress: ((RestoreProgress) -> Unit)? = null
    ): RestoreResult
}
