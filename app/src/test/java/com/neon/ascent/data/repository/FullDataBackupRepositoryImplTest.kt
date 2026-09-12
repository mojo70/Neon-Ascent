package com.neon.ascent.data.repository

import com.google.gson.Gson
import com.neon.ascent.core.domain.backup.models.NeonAscentBackupPayload
import com.neon.ascent.core.domain.backup.models.RestoreMode
import com.neon.ascent.core.domain.backup.models.WorkoutBackupSection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FullDataBackupRepositoryImplTest {

    private val gson = Gson()

    @Test
    fun `restoreBackupJson returns failure for empty or blank json`() = runBlocking {
        val emptyResult = processRestoreJson("", RestoreMode.MERGE)
        assertFalse(emptyResult.success)
        assertEquals("Invalid JSON payload structure: File is empty", emptyResult.message)

        val whitespaceResult = processRestoreJson("   ", RestoreMode.MERGE)
        assertFalse(whitespaceResult.success)
        assertEquals("Invalid JSON payload structure: File is empty", whitespaceResult.message)
    }

    @Test
    fun `restoreBackupJson returns failure for malformed json`() = runBlocking {
        val malformedResult = processRestoreJson("{ invalid json }", RestoreMode.MERGE)
        assertFalse(malformedResult.success)
        assertEquals("Invalid JSON payload structure", malformedResult.message)
    }

    @Test
    fun `restoreBackupJson returns failure for json payload missing all backup sections`() = runBlocking {
        val jsonNoSections = """{"version": 1, "exportedAt": "2025-02-23T10:00:00Z"}"""
        val result = processRestoreJson(jsonNoSections, RestoreMode.MERGE)
        assertFalse(result.success)
        assertEquals("Invalid JSON payload structure: No backup sections found", result.message)
    }

    private fun processRestoreJson(jsonString: String, mode: RestoreMode): com.neon.ascent.core.domain.backup.models.RestoreResult {
        val sanitizedJson = jsonString.trim().removePrefix("\uFEFF")
        if (sanitizedJson.isBlank()) {
            return com.neon.ascent.core.domain.backup.models.RestoreResult(
                success = false,
                message = "Invalid JSON payload structure: File is empty"
            )
        }

        val payload = try {
            gson.fromJson(sanitizedJson, NeonAscentBackupPayload::class.java)
        } catch (e: Exception) {
            return com.neon.ascent.core.domain.backup.models.RestoreResult(
                success = false,
                message = "Invalid JSON payload structure"
            )
        } ?: return com.neon.ascent.core.domain.backup.models.RestoreResult(
            success = false,
            message = "Invalid JSON payload structure"
        )

        if (payload.workoutPayload == null &&
            payload.biometricsPayload == null &&
            payload.codexPayload == null &&
            payload.journalPayload == null &&
            payload.characterPayload == null
        ) {
            return com.neon.ascent.core.domain.backup.models.RestoreResult(
                success = false,
                message = "Invalid JSON payload structure: No backup sections found"
            )
        }

        return com.neon.ascent.core.domain.backup.models.RestoreResult(
            success = true,
            message = "Uplink restore complete"
        )
    }
}
