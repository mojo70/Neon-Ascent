package com.neon.ascent.data.backup.p2p

import com.neon.ascent.core.domain.backup.models.BackupScope
import com.neon.ascent.core.domain.backup.models.RestoreMode
import com.neon.ascent.core.domain.backup.models.RestoreResult
import com.neon.ascent.core.domain.repository.FullDataBackupRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class P2pMigrationClientTest {

    private class FakeFullDataBackupRepository : FullDataBackupRepository {
        var restoreSuccess = true
        var lastRestoredJson: String? = null

        override suspend fun exportBackupJson(scope: BackupScope): String = "{}"
        override suspend fun restoreBackupJson(jsonString: String, mode: RestoreMode): RestoreResult {
            lastRestoredJson = jsonString
            return RestoreResult(success = restoreSuccess, message = if (restoreSuccess) "Restore Success" else "Error")
        }
    }

    @Test
    fun `downloadAndRestore connects to server, decrypts payload, and calls restoreBackupJson`() = runBlocking {
        val server = P2pMigrationServer()
        val backupRepo = FakeFullDataBackupRepository()
        val client = P2pMigrationClient(backupRepo)

        val testPayload = "{\"version\":3,\"testData\":\"end_to_end_p2p\"}"
        val info = server.startServer(testPayload)
        assertNotNull(info)

        val result = client.downloadAndRestore(info!!)
        assertTrue("Client migration should succeed", result.success)
        assertEquals("Restore Success", result.message)
        assertEquals(testPayload, backupRepo.lastRestoredJson)

        server.stopServer()
    }
}
