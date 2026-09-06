package com.neon.ascent.feature.biohacking

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import com.neon.ascent.core.ai.GemmaClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class ModelDownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    VERIFYING,
    READY,
    FAILED
}

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemmaClient: GemmaClient,
    private val aiProvider: AiProvider
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadState = MutableStateFlow(ModelDownloadState.NOT_DOWNLOADED)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private var downloadId: Long = -1L

    private val modelUrl = "https://huggingface.co/google/gemma-2b-it-litertlm/resolve/main/gemma-2b-it.litertlm"
    private val modelFileName = "gemma.litertlm"

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                verifyAndInitialize()
            }
        }
    }

    init {
        checkInitialStatus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    fun checkInitialStatus() {
        val file = File(context.getExternalFilesDir(null), modelFileName)
        if (file.exists() && file.length() > 0) {
            _downloadState.value = ModelDownloadState.READY
            _isDownloading.value = false
            _downloadProgress.value = 1.0f
        } else {
            _downloadState.value = ModelDownloadState.NOT_DOWNLOADED
            _isDownloading.value = false
            _downloadProgress.value = null
        }
    }

    fun startDownload() {
        val externalFile = File(context.getExternalFilesDir(null), modelFileName)
        if (externalFile.exists() && externalFile.length() > 0) {
            _downloadState.value = ModelDownloadState.READY
            _isDownloading.value = false
            return
        }

        if (_downloadState.value == ModelDownloadState.DOWNLOADING) return

        _downloadState.value = ModelDownloadState.DOWNLOADING
        _isDownloading.value = true
        _downloadProgress.value = 0.0f
        Log.i("ModelDownloadManager", "Starting download of $modelFileName from $modelUrl")

        try {
            val request = DownloadManager.Request(Uri.parse(modelUrl))
                .setTitle("Downloading Neural Engine")
                .setDescription("Downloading Gemma LiteRT-LM Local Model")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, null, modelFileName)

            downloadId = downloadManager.enqueue(request)
            startProgressPolling()
        } catch (e: Exception) {
            Log.e("ModelDownloadManager", "Failed to enqueue download request", e)
            _downloadState.value = ModelDownloadState.FAILED
            _isDownloading.value = false
            _downloadProgress.value = null
        }
    }

    private fun startProgressPolling() {
        scope.launch {
            while (_downloadState.value == ModelDownloadState.DOWNLOADING) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (bytesTotal > 0) {
                        _downloadProgress.value = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                    }
                    
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        _downloadState.value = ModelDownloadState.VERIFYING
                        _isDownloading.value = false
                        verifyAndInitialize()
                        cursor.close()
                        break
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        Log.e("ModelDownloadManager", "Download failed according to DownloadManager.")
                        _downloadState.value = ModelDownloadState.FAILED
                        _isDownloading.value = false
                        _downloadProgress.value = null
                        cursor.close()
                        break
                    }
                    cursor.close()
                }
                delay(1000)
            }
        }
    }

    private fun verifyAndInitialize() {
        scope.launch {
            _downloadState.value = ModelDownloadState.VERIFYING
            val file = File(context.getExternalFilesDir(null), modelFileName)
            if (file.exists() && file.length() > 0) {
                Log.i("ModelDownloadManager", "Model file downloaded successfully. Size: ${file.length()} bytes.")
                _downloadProgress.value = 1f
                _downloadState.value = ModelDownloadState.READY
                _isDownloading.value = false
                gemmaClient.initialize()
                aiProvider.onModelDownloaded()
            } else {
                Log.e("ModelDownloadManager", "Verification failed: file missing or empty.")
                if (file.exists()) file.delete()
                _downloadState.value = ModelDownloadState.FAILED
                _isDownloading.value = false
                _downloadProgress.value = null
            }
        }
    }

    fun isModelDownloaded(): Boolean {
        val file = File(context.getExternalFilesDir(null), modelFileName)
        return file.exists() && file.length() > 0
    }
}
