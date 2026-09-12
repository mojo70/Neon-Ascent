package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.backup.models.BackupScope
import com.neon.ascent.core.domain.backup.models.RestoreMode
import com.neon.ascent.core.domain.backup.models.RestoreResult

interface FullDataBackupRepository {
    suspend fun exportBackupJson(scope: BackupScope): String
    suspend fun restoreBackupJson(jsonString: String, mode: RestoreMode): RestoreResult
}
