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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.ImagePart
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.neon.ascent.BuildConfig
import com.neon.ascent.model.UserCharacter
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun AvatarCaptureScreen(
    onComplete: (Bitmap) -> Unit,
    creationViewModel: CreationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val characterState by creationViewModel.uiState.collectAsState()
    
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
                
                TextButton(onClick = { avatarBitmap = null; errorMessage = null }) {
                    Text("RE-GENERATE", color = Color(0xFF00FF9C))
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
                Text("UPLINKING TO NANO_BANANA_CORE", color = Color(0xFFFF006E), fontSize = 10.sp)
            }
        } else {
            // Camera View
            if (hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(onImageCaptured = { bitmap ->
                        isProcessing = true
                        scope.launch {
                            try {
                                val generated = generateCyberAvatar(bitmap, characterState)
                                if (generated != null) {
                                    avatarBitmap = generated
                                } else {
                                    errorMessage = "AVATAR_GEN_FAILED: NO_DATA"
                                }
                            } catch (e: Exception) {
                                Log.e("AvatarGen", "Fail", e)
                                errorMessage = "CONNECTION_LOST: ${e.message}"
                            } finally {
                                isProcessing = false
                            }
                        }
                    })
                    
                    // Skip/Opt-out Button
                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    avatarBitmap = generateCyberAvatar(null, characterState)
                                } catch (e: Exception) {
                                    errorMessage = "AVATAR_GEN_FAILED"
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 80.dp)
                            .height(48.dp)
                            .clip(CyberButtonShape)
                            .background(Color(0xFF1A1A1A))
                            .border(1.dp, Color(0xFFFF006E).copy(alpha = 0.5f), CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("BYPASS BIOMETRIC SCAN", color = Color(0xFFFF006E), fontSize = 12.sp)
                    }
                }
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
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        isProcessing = true
                        scope.launch {
                            try {
                                avatarBitmap = generateCyberAvatar(null, characterState)
                            } catch (e: Exception) {
                                errorMessage = "AVATAR_GEN_FAILED"
                            } finally {
                                isProcessing = false
                            }
                        }
                    }) {
                        Text("SKIP AND USE SYSTEM-GENERATED AVATAR", color = Color(0xFF00FF9C))
                    }
                }
            }
        }
        
        if (errorMessage != null && !isProcessing && avatarBitmap == null) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomCenter) {
                Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        CyberGridBackground()
        GlitchOverlay()
    }
}

suspend fun generateCyberAvatar(input: Bitmap?, character: UserCharacter): Bitmap? {
    val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", 
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    val promptText = if (input != null) {
        "Transform this person into a high-quality cyberpunk line-art avatar. " +
        "Style: Digital blueprint, holographic scan, minimalist line art. " +
        "Colors: Matrix green (#00FF9C) and Cyberpink (#FF006E) highlights on a deep black background. " +
        "Context: They are a ${character.archetype} (${character.mbti}) with a ${character.alignment} alignment."
    } else {
        "Generate a high-quality cyberpunk line-art avatar face for a character with these traits: " +
        "Archetype: ${character.archetype}, MBTI: ${character.mbti}, Alignment: ${character.alignment}, " +
        "Sex: ${character.sex}, Somatotype: ${character.somatotype}. " +
        "Style: Digital blueprint, holographic scan, minimalist line art. " +
        "Colors: Matrix green (#00FF9C) and Cyberpink (#FF006E) highlights on a deep black background."
    }

    return try {
        val result = if (input != null) {
            generativeModel.generateContent(content {
                image(input)
                text(promptText)
            })
        } else {
            generativeModel.generateContent(promptText)
        }
        
        // Extract the generated image from the response parts if supported
        val imagePart = result.candidates.firstOrNull()?.content?.parts?.filterIsInstance<ImagePart>()?.firstOrNull()
        
        imagePart?.image ?: input ?: createPlaceholderAvatar(character)
    } catch (e: Exception) {
        Log.e("Gemini", "Generation failed: ${e.message}", e)
        input ?: createPlaceholderAvatar(character)
    }
}

private fun createPlaceholderAvatar(character: UserCharacter): Bitmap {
    val size = 512
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    // Background
    paint.color = android.graphics.Color.BLACK
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    
    // Base Glow
    paint.color = android.graphics.Color.parseColor("#00FF9C")
    paint.alpha = 40
    canvas.drawCircle(size/2f, size/2f, size/2.5f, paint)
    
    // Geometric Face Profile based on Archetype
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 6f
    paint.alpha = 255
    
    val path = android.graphics.Path()
    when (character.archetype) {
        "THE STRATEGIST" -> { // Sharp angles
            path.moveTo(size*0.3f, size*0.3f)
            path.lineTo(size*0.7f, size*0.3f)
            path.lineTo(size*0.8f, size*0.6f)
            path.lineTo(size*0.5f, size*0.9f)
            path.lineTo(size*0.2f, size*0.6f)
            path.close()
        }
        else -> { // Standard bio-mask
            path.addCircle(size/2f, size/2f, size/3f, android.graphics.Path.Direction.CW)
        }
    }
    canvas.drawPath(path, paint)
    
    // Cyber-optics (Pink)
    paint.color = android.graphics.Color.parseColor("#FF006E")
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawRect(size*0.35f, size*0.45f, size*0.45f, size*0.5f, paint)
    canvas.drawRect(size*0.55f, size*0.45f, size*0.65f, size*0.5f, paint)
    
    // Data lines
    paint.color = android.graphics.Color.parseColor("#00FF9C")
    paint.strokeWidth = 2f
    for (i in 0 until 5) {
        val y = size * (0.2f + i * 0.15f)
        canvas.drawLine(0f, y, size.toFloat(), y, paint)
    }
    
    return bitmap
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
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            onImageCaptured(bitmap)
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
