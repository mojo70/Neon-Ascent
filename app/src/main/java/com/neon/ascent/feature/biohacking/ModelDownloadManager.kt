package com.neon.ascent.feature.biohacking

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import com.neon.ascent.core.ai.GemmaClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemmaClient: GemmaClient,
    private val aiProvider: AiProvider
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private var downloadId: Long = -1L

    // Updated to reflect the specific Gemma 4 E2B variant path format
    private val modelUrl = "https://huggingface.co/google/gemma-4-E2B-it-litertlm/resolve/main/gemma-4-E2B-it.litertlm"
    private val modelFileName = "gemma.litertlm"
    private val expectedModelSize = 2_000_000_000L // Approximate 2GB
    private val expectedChecksum = "sha256:7f8e9d..." // Placeholder for actual checksum

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                verifyAndInitialize()
            }
        }
    }

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    fun startDownload() {
        val externalFile = File(context.getExternalFilesDir(null), modelFileName)
        if (externalFile.exists() || _isDownloading.value) return

        _isDownloading.value = true
        val request = DownloadManager.Request(Uri.parse(modelUrl))
            .setTitle("Downloading Neural Engine")
            .setDescription("Downloading Gemma 4-E2B Local Model")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, null, modelFileName)

        downloadId = downloadManager.enqueue(request)
        startProgressPolling()
    }

    private fun startProgressPolling() {
        scope.launch {
            while (_isDownloading.value) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (bytesTotal > 0) {
                        _downloadProgress.value = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                    }
                    
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        _isDownloading.value = false
                        if (status == DownloadManager.STATUS_FAILED) {
                            _downloadProgress.value = null
                        }
                    }
                }
                cursor.close()
                delay(1000)
            }
        }
    }

    private fun verifyAndInitialize() {
        scope.launch {
            val file = File(context.getExternalFilesDir(null), modelFileName)
            if (file.exists()) {
                // Verification logic: Size check + Checksum (Mocked)
                val isSizeValid = file.length() > expectedModelSize * 0.9
                val isChecksumValid = performChecksum(file)
                
                if (isSizeValid && isChecksumValid) {
                    _downloadProgress.value = 1f
                    _isDownloading.value = false
                    gemmaClient.initialize()
                    aiProvider.onModelDownloaded()
                } else {
                    // Verification failed
                    file.delete()
                    _isDownloading.value = false
                    _downloadProgress.value = null
                }
            }
        }
    }

    private fun performChecksum(file: File): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val fis: InputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            fis.close()
            val hexString = digest.digest().joinToString("") { "%02x".format(it) }
            hexString.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun isModelDownloaded(): Boolean {
        return File(context.getExternalFilesDir(null), modelFileName).exists()
    }
}
