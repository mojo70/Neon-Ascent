package com.neon.ascent.data.backup.p2p

import java.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

import com.google.gson.annotations.SerializedName

enum class P2pServerState {
    IDLE,
    WAITING_FOR_CONNECTION,
    TRANSFERRING,
    SUCCESS,
    ERROR
}

data class P2pServerInfo(
    val ip: String,
    val port: Int,
    val token: String,
    @SerializedName(value = "aesKeyBase64", alternate = ["key"])
    val aesKeyBase64: String
) {
    fun toQrJson(): String {
        return "{\"ip\":\"$ip\",\"port\":$port,\"token\":\"$token\",\"key\":\"$aesKeyBase64\"}"
    }
}

class P2pMigrationServer {

    private var serverSocket: ServerSocket? = null
    private var scope = CoroutineScope(Dispatchers.IO + Job())

    private val _serverState = MutableStateFlow(P2pServerState.IDLE)
    val serverState: StateFlow<P2pServerState> = _serverState.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    var serverInfo: P2pServerInfo? = null
        private set

    fun startServer(payloadJson: String): P2pServerInfo? {
        stopServer()
        _serverState.value = P2pServerState.WAITING_FOR_CONNECTION
        _statusMessage.value = "INITIALIZING P2P SERVER..."

        return try {
            val localIp = getLocalIpAddress() ?: "127.0.0.1"

            // 1. Generate AES-256 Key
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            val secretKey: SecretKey = keyGen.generateKey()
            val aesKeyBase64 = Base64.getEncoder().encodeToString(secretKey.encoded)

            // 2. Generate Single-use Token
            val token = UUID.randomUUID().toString()

            // 3. Compress & Encrypt Payload
            val encryptedBytes = encryptPayload(payloadJson, secretKey)

            // 4. Start ServerSocket on random open port
            val socket = ServerSocket(0)
            val port = socket.localPort
            serverSocket = socket

            val info = P2pServerInfo(
                ip = localIp,
                port = port,
                token = token,
                aesKeyBase64 = aesKeyBase64
            )
            serverInfo = info
            _statusMessage.value = "WAITING FOR CONNECTION..."

            // 5. Listen for incoming connection on IO scope
            scope.launch {
                try {
                    while (serverSocket?.isClosed == false) {
                        val clientSocket = serverSocket?.accept() ?: break
                        handleClientConnection(clientSocket, token, encryptedBytes)
                    }
                } catch (e: Exception) {
                    if (serverSocket?.isClosed == false) {
                        Log.e("P2pMigrationServer", "Server loop error", e)
                        _serverState.value = P2pServerState.ERROR
                        _statusMessage.value = "SERVER ERROR: ${e.localizedMessage}"
                    }
                }
            }

            // Auto shutdown after 10 mins timeout
            scope.launch {
                delay(10 * 60 * 1000L)
                if (_serverState.value == P2pServerState.WAITING_FOR_CONNECTION) {
                    stopServer()
                    _serverState.value = P2pServerState.ERROR
                    _statusMessage.value = "SERVER TIMED OUT (10 MIN)"
                }
            }

            info
        } catch (e: Exception) {
            Log.e("P2pMigrationServer", "Failed to start server", e)
            _serverState.value = P2pServerState.ERROR
            _statusMessage.value = "SERVER START FAILED: ${e.localizedMessage}"
            null
        }
    }

    private fun handleClientConnection(socket: Socket, expectedToken: String, encryptedBytes: ByteArray) {
        try {
            val reader = socket.getInputStream().bufferedReader()
            val line = reader.readLine() ?: return

            // Expected HTTP Request: GET /transfer?token=<TOKEN> HTTP/1.1
            val tokenParam = line.substringAfter("token=", "").substringBefore(" ").substringBefore("&")

            if (tokenParam == expectedToken) {
                _serverState.value = P2pServerState.TRANSFERRING
                _statusMessage.value = "TRANSFERRING DATA TO NEW DEVICE..."

                val os = socket.getOutputStream()
                val responseHeader = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        "Content-Length: ${encryptedBytes.size}\r\n" +
                        "Connection: close\r\n\r\n"
                os.write(responseHeader.toByteArray(Charsets.UTF_8))
                os.write(encryptedBytes)
                os.flush()

                _serverState.value = P2pServerState.SUCCESS
                _statusMessage.value = "MIGRATION COMPLETE"

                scope.launch {
                    delay(2000)
                    stopServer()
                }
            } else {
                val os = socket.getOutputStream()
                val responseHeader = "HTTP/1.1 401 Unauthorized\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                os.write(responseHeader.toByteArray(Charsets.UTF_8))
                os.flush()
            }
        } catch (e: Exception) {
            Log.e("P2pMigrationServer", "Client handling failed", e)
            _serverState.value = P2pServerState.ERROR
            _statusMessage.value = "TRANSFER FAILED: ${e.localizedMessage}"
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e("P2pMigrationServer", "Error closing server socket", e)
        } finally {
            if (_serverState.value != P2pServerState.SUCCESS && _serverState.value != P2pServerState.ERROR) {
                _serverState.value = P2pServerState.IDLE
                _statusMessage.value = "SERVER STOPPED"
            }
        }
    }

    private fun encryptPayload(json: String, secretKey: SecretKey): ByteArray {
        val gzipBaos = ByteArrayOutputStream()
        GZIPOutputStream(gzipBaos).use { gzip ->
            gzip.write(json.toByteArray(Charsets.UTF_8))
        }
        val compressedBytes = gzipBaos.toByteArray()

        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val cipherText = cipher.doFinal(compressedBytes)

        val result = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(cipherText, 0, result, iv.size, cipherText.size)
        return result
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val ip = address.hostAddress ?: ""
                        if (!ip.contains(":") && (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("P2pMigrationServer", "Error finding local IP", e)
        }
        return null
    }
}
