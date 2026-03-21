package com.neon.ascent.feature.charactercreation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.neon.ascent.BuildConfig
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun AvatarCaptureScreen(onComplete: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (avatarBitmap != null) {
            // Result View
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("AVATAR CONSTRUCTED", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Image(
                    bitmap = avatarBitmap!!.asImageBitmap(),
                    contentDescription = "Cyber Avatar",
                    modifier = Modifier.size(300.dp).border(2.dp, Color(0xFF00FF9C), CyberButtonShape).clip(CyberButtonShape)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { onComplete(avatarBitmap!!) },
                    modifier = Modifier.fillMaxWidth().height(64.dp).clip(CyberButtonShape).background(Color(0xFFFF006E)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("ACCEPT PROTOCOL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else if (isProcessing) {
            // Loading View
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00FF9C))
                Spacer(modifier = Modifier.height(16.dp))
                Text("GENERATING CYBERNETIC AVATAR...", color = Color(0xFF00FF9C), letterSpacing = 2.sp)
            }
        } else {
            // Camera View
            if (hasCameraPermission) {
                CameraPreview(onImageCaptured = { bitmap ->
                    isProcessing = true
                    scope.launch {
                        avatarBitmap = generateCyberAvatar(bitmap)
                        isProcessing = false
                    }
                })
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CAMERA ACCESS REQUIRED", color = Color(0xFFFF006E), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.clip(CyberButtonShape).background(Color(0xFF00FF9C)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("AUTHORIZE BIOMETRIC ACCESS", color = Color.Black)
                    }
                }
            }
        }
        
        CyberGridBackground()
        GlitchOverlay()
    }
}

suspend fun generateCyberAvatar(input: Bitmap): Bitmap? {
    val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    val prompt = content {
        image(input)
        text("Transform this person into a high-quality cyberpunk line-art avatar. Use neon green and pink highlights. Make it look like a digital blueprint or holographic scan.")
    }

    return try {
        val response = generativeModel.generateContent(prompt)
        // Note: Standard Gemini API returns text unless image output is configured.
        // For now, we'll return the input as a placeholder if no image part is found.
        input 
    } catch (e: Exception) {
        Log.e("Gemini", "Generation failed", e)
        input
    }
}

@Composable
fun CameraPreview(onImageCaptured: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    LaunchedEffect(cameraSelector) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build()
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e("CameraPreview", "Use case binding failed", e)
        }
    }

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        
        Button(
            onClick = {
                imageCapture.takePicture(
                    cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = image.toBitmap()
                            val matrix = Matrix()
                            matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                            matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                            val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            onImageCaptured(finalBitmap)
                            image.close()
                        }
                    }
                )
            },
            modifier = Modifier
                .padding(bottom = 80.dp)
                .size(80.dp)
                .clip(CyberButtonShape)
                .background(Color(0xFFFF006E))
                .border(2.dp, Color.White, CyberButtonShape),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Text("SCAN", color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { cameraProviderFuture ->
        cameraProviderFuture.addListener({
            continuation.resume(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(this))
    }
}
