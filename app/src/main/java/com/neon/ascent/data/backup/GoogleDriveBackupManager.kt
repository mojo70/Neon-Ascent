package com.neon.ascent.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun saveBackupToLocalVault(jsonContent: String): File {
        val vaultDir = File(context.filesDir, "cloud_backups").apply { if (!exists()) mkdirs() }
        val backupFile = File(vaultDir, "neon_ascent_backup_latest.json")
        FileOutputStream(backupFile).use { it.write(jsonContent.toByteArray()) }
        return backupFile
    }

    fun readBackupFromLocalVault(): String? {
        val backupFile = File(context.filesDir, "cloud_backups/neon_ascent_backup_latest.json")
        return if (backupFile.exists()) backupFile.readText() else null
    }

    fun getLastVaultModifiedTimestamp(): Long? {
        val backupFile = File(context.filesDir, "cloud_backups/neon_ascent_backup_latest.json")
        return if (backupFile.exists()) backupFile.lastModified() else null
    }

    fun writeToUri(contentUri: Uri, jsonContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(contentUri)?.use { out ->
                out.write(jsonContent.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readFromUri(contentUri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(contentUri)?.use { input: InputStream ->
                input.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            null
        }
    }
}
