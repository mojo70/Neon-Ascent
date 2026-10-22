package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.backup.models.BackupScope
import com.neon.ascent.core.domain.backup.models.RestoreMode
import com.neon.ascent.core.domain.backup.models.RestoreResult

/**
 * Domain contract for full application data export and restoration.
 * Supports v2 and v3 NeonAscentBackupPayload exports.
 */
interface FullDataBackupRepository {
    suspend fun exportBackupJson(scope: BackupScope): String
    suspend fun restoreBackupJson(jsonString: String, mode: RestoreMode = RestoreMode.MERGE): RestoreResult
}
