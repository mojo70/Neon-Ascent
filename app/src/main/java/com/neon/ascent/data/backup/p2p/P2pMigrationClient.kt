package com.neon.ascent.data.backup.p2p

import android.util.Log
import com.neon.ascent.core.domain.backup.models.RestoreMode
import com.neon.ascent.core.domain.backup.models.RestoreResult
import com.neon.ascent.core.domain.repository.FullDataBackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed class P2pClientState {
    object Idle : P2pClientState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : P2pClientState()
    object Decrypting : P2pClientState()
    object Restoring : P2pClientState()
    data class Success(val message: String) : P2pClientState()
    data class Error(val message: String) : P2pClientState()
}

@Singleton
class P2pMigrationClient @Inject constructor(
    private val fullDataBackupRepository: FullDataBackupRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _clientState = MutableStateFlow<P2pClientState>(P2pClientState.Idle)
    val clientState: StateFlow<P2pClientState> = _clientState.asStateFlow()

    suspend fun downloadAndRestore(serverInfo: P2pServerInfo): RestoreResult = withContext(Dispatchers.IO) {
        try {
            _clientState.value = P2pClientState.Downloading(0, 0L, 0L)
            Log.i("P2pMigrationClient", "Connecting to http://${serverInfo.ip}:${serverInfo.port}...")

            // 1. HTTP GET Request
            val request = Request.Builder()
                .url("http://${serverInfo.ip}:${serverInfo.port}/transfer?token=${serverInfo.token}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = "HTTP error ${response.code}: ${response.message}"
                _clientState.value = P2pClientState.Error(errorMsg)
                return@withContext RestoreResult(success = false, message = errorMsg)
            }

            val responseBody = response.body ?: run {
                val errorMsg = "Empty response body from migration server"
                _clientState.value = P2pClientState.Error(errorMsg)
                return@withContext RestoreResult(success = false, message = errorMsg)
            }

            val totalBytes = responseBody.contentLength()
            val inputStream = responseBody.byteStream()
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                baos.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val progress = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt() else -1
                _clientState.value = P2pClientState.Downloading(progress, totalRead, totalBytes)
            }

            val encryptedBytes = baos.toByteArray()
            if (encryptedBytes.size <= 12) {
                val errorMsg = "Downloaded payload is corrupted or incomplete"
                _clientState.value = P2pClientState.Error(errorMsg)
                return@withContext RestoreResult(success = false, message = errorMsg)
            }

            // 2. Decrypt Payload
            _clientState.value = P2pClientState.Decrypting
            val keyString = serverInfo.aesKeyBase64
            if (keyString.isNullOrBlank()) {
                val errorMsg = "Encryption key missing from QR credentials"
                _clientState.value = P2pClientState.Error(errorMsg)
                return@withContext RestoreResult(success = false, message = errorMsg)
            }
            val rawKeyBytes = Base64.getDecoder().decode(keyString)
            val secretKey: SecretKey = SecretKeySpec(rawKeyBytes, "AES")

            val iv = encryptedBytes.copyOfRange(0, 12)
            val cipherText = encryptedBytes.copyOfRange(12, encryptedBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val compressedBytes = cipher.doFinal(cipherText)

            // 3. Decompress GZIP
            val gzipInputStream = GZIPInputStream(ByteArrayInputStream(compressedBytes))
            val jsonPayload = gzipInputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            // 4. Restore Database
            _clientState.value = P2pClientState.Restoring
            val restoreResult = fullDataBackupRepository.restoreBackupJson(jsonPayload, RestoreMode.REPLACE)

            if (restoreResult.success) {
                _clientState.value = P2pClientState.Success(restoreResult.message)
            } else {
                _clientState.value = P2pClientState.Error(restoreResult.message)
            }

            restoreResult
        } catch (e: Exception) {
            Log.e("P2pMigrationClient", "Migration failed", e)
            val errorMsg = e.localizedMessage ?: "P2P migration failed"
            _clientState.value = P2pClientState.Error(errorMsg)
            RestoreResult(success = false, message = errorMsg)
        }
    }

    fun resetState() {
        _clientState.value = P2pClientState.Idle
    }
}
