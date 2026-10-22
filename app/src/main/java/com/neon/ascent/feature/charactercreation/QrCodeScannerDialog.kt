package com.neon.ascent.feature.charactercreation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.data.backup.p2p.P2pServerInfo
import java.util.EnumMap
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrCodeScannerDialog(
    onDismissRequest: () -> Unit,
    onQrScanned: (P2pServerInfo) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val theme = LocalNeonTheme.current
    val gson = remember { Gson() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth().height(480.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, theme.primary)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    ">>> SCAN MIGRATION QR <<<",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = theme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .border(2.dp, theme.primary, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }

                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    val cameraExecutor = Executors.newSingleThreadExecutor()

                                    // Primary ML Kit Scanner
                                    val barcodeScanner = BarcodeScanning.getClient(
                                        BarcodeScannerOptions.Builder()
                                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                            .build()
                                    )

                                    // Fallback ZXing Reader
                                    val zxingReader = MultiFormatReader().apply {
                                        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                                            put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
                                            put(DecodeHintType.TRY_HARDER, true)
                                        }
                                        setHints(hints)
                                    }

                                    var isScanned = false

                                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        if (isScanned) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val inputImage = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )

                                            barcodeScanner.process(inputImage)
                                                .addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        val rawValue = barcode.rawValue
                                                        if (!rawValue.isNullOrBlank() && !isScanned) {
                                                            try {
                                                                val info = gson.fromJson(rawValue, P2pServerInfo::class.java)
                                                                if (!info.ip.isNullOrBlank() && info.port > 0 && !info.token.isNullOrBlank() && !info.aesKeyBase64.isNullOrBlank()) {
                                                                    isScanned = true
                                                                    cameraProvider.unbindAll()
                                                                    onQrScanned(info)
                                                                    break
                                                                }
                                                            } catch (e: Exception) {
                                                                Log.d("QrCodeScanner", "Invalid MLKit QR JSON: ${e.message}")
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    // Fallback to ZXing analyzer if ML Kit fails
                                                    scanZxingImage(imageProxy, zxingReader) { jsonText ->
                                                        try {
                                                            val info = gson.fromJson(jsonText, P2pServerInfo::class.java)
                                                            if (!info.ip.isNullOrBlank() && info.port > 0 && !info.token.isNullOrBlank()) {
                                                                isScanned = true
                                                                cameraProvider.unbindAll()
                                                                onQrScanned(info)
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.d("QrCodeScanner", "Invalid ZXing QR JSON: ${e.message}")
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            scanZxingImage(imageProxy, zxingReader) { jsonText ->
                                                try {
                                                    val info = gson.fromJson(jsonText, P2pServerInfo::class.java)
                                                    if (!info.ip.isNullOrBlank() && info.port > 0 && !info.token.isNullOrBlank()) {
                                                        isScanned = true
                                                        cameraProvider.unbindAll()
                                                        onQrScanned(info)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.d("QrCodeScanner", "Invalid ZXing QR JSON: ${e.message}")
                                                }
                                            }
                                        }
                                    }

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (e: Exception) {
                                        Log.e("QrCodeScanner", "Camera bind failed", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .background(theme.surface)
                            .border(1.dp, theme.hairline),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "CAMERA PERMISSION REQUIRED TO SCAN MIGRATION QR",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = theme.inkMuted,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.surfaceRaised),
                    border = BorderStroke(1.dp, theme.hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "CANCEL SCAN",
                        color = theme.ink,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun scanZxingImage(
    imageProxy: ImageProxy,
    reader: MultiFormatReader,
    onQrFound: (String) -> Unit
) {
    if (imageProxy.format == ImageFormat.YUV_420_888 || imageProxy.format == ImageFormat.YUV_422_888) {
        val yPlane = imageProxy.planes[0]
        val yBuffer = yPlane.buffer
        val ySize = yBuffer.remaining()
        val bytes = ByteArray(ySize)
        yBuffer.get(bytes)

        val rowStride = yPlane.rowStride
        val width = imageProxy.width
        val height = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees

        // Attempt 1: Standard YUV source with correct rowStride
        var qrText = decodeYuvData(bytes, rowStride, height, width, height, reader)

        // Attempt 2: Rotated YUV source if rotation is 90 or 270 degrees
        if (qrText == null && (rotation == 90 || rotation == 270)) {
            val rotatedBytes = rotateYuv90(bytes, rowStride, height)
            qrText = decodeYuvData(rotatedBytes, height, rowStride, height, width, reader)
        }

        if (qrText != null) {
            onQrFound(qrText)
        }
    }
    imageProxy.close()
}

private fun decodeYuvData(
    bytes: ByteArray,
    dataWidth: Int,
    dataHeight: Int,
    rectWidth: Int,
    rectHeight: Int,
    reader: MultiFormatReader
): String? {
    return try {
        val source = PlanarYUVLuminanceSource(
            bytes,
            dataWidth,
            dataHeight,
            0,
            0,
            rectWidth,
            rectHeight,
            false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = reader.decodeWithState(binaryBitmap)
        result.text
    } catch (_: Exception) {
        null
    } finally {
        reader.reset()
    }
}

private fun rotateYuv90(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
    val rotatedData = ByteArray(data.size)
    var i = 0
    for (x in 0 until imageWidth) {
        for (y in imageHeight - 1 downTo 0) {
            if (y * imageWidth + x < data.size && i < rotatedData.size) {
                rotatedData[i] = data[y * imageWidth + x]
            }
            i++
        }
    }
    return rotatedData
}
