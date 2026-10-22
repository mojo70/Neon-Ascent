package com.neon.ascent.data.backup.p2p

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class P2pMigrationServerTest {

    @Test
    fun `startServer returns valid server info and handles request with valid token`() = runBlocking {
        val server = P2pMigrationServer()
        val testPayload = "{\"version\":3,\"testData\":\"cyberpunk\"}"

        val info = server.startServer(testPayload)
        assertNotNull("Server info should not be null", info)
        assertTrue("Port should be > 0", info!!.port > 0)
        assertNotNull("IP should be populated", info.ip)
        assertNotNull("Token should be populated", info.token)
        assertNotNull("AES key should be populated", info.aesKeyBase64)

        // Test HTTP Request with valid token
        val url = URL("http://127.0.0.1:${info.port}/transfer?token=${info.token}")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 3000
        conn.readTimeout = 3000

        val responseCode = conn.responseCode
        assertEquals(200, responseCode)

        val encryptedBytes = conn.inputStream.readBytes()
        assertTrue("Response byte array should not be empty", encryptedBytes.isNotEmpty())

        conn.disconnect()
        server.stopServer()
        assertEquals(P2pServerState.SUCCESS, server.serverState.value)
    }

    @Test
    fun `startServer rejects request with invalid token`() = runBlocking {
        val server = P2pMigrationServer()
        val info = server.startServer("{\"version\":3}")
        assertNotNull(info)

        val url = URL("http://127.0.0.1:${info!!.port}/transfer?token=INVALID_TOKEN")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"

        val responseCode = conn.responseCode
        assertEquals(401, responseCode)

        conn.disconnect()
        server.stopServer()
    }
}
