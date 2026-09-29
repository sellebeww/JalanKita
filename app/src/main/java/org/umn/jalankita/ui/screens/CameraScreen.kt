package org.umn.jalankita.ui.screens

import android.Manifest
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.umn.jalankita.data.media.PhotoStorage
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.ui.permissions.hasCameraPermission
import org.umn.jalankita.ui.permissions.openAppSettings
import org.umn.jalankita.ui.permissions.rememberSinglePermissionLauncher
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * CameraX capture with an explicit confirm step, so the user can retake a blurry
 * shot before it becomes part of the report.
 */
@Composable
fun CameraScreen(
    reportId: String,
    photoStorage: PhotoStorage,
    /** Framing advice for the chosen damage category, shown over the viewfinder. */
    photoTip: String?,
    onPhotoConfirmed: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var permissionRequested by remember { mutableStateOf(false) }
    var capturedPath by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberSinglePermissionLauncher { granted ->
        hasPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            !hasPermission -> CameraPermissionRationale(
                permanentlyDenied = permissionRequested,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { context.openAppSettings() },
                onBack = onBack,
            )

            capturedPath != null -> CapturedPreview(
                path = capturedPath!!,
                onRetake = {
                    photoStorage.delete(capturedPath)
                    capturedPath = null
                },
                onUse = { onPhotoConfirmed(capturedPath!!) },
            )

            else -> CameraPreview(
                isCapturing = isCapturing,
                errorMessage = errorMessage,
                photoTip = photoTip,
                onBack = onBack,
                onCapture = { imageCapture ->
                    if (isCapturing) return@CameraPreview
                    isCapturing = true
                    errorMessage = null
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bytes = image.toJpegBytes()
                                val rotation = image.imageInfo.rotationDegrees
                                image.close()
                                scope.launch {
                                    photoStorage.saveCompressed(bytes, rotation, reportId)
                                        .onSuccess { capturedPath = it.absolutePath }
                                        .onFailure {
                                            errorMessage = "Foto gagal disimpan. Silakan coba lagi."
                                        }
                                    isCapturing = false
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                errorMessage = "Kamera gagal mengambil foto. Silakan coba lagi."
                                isCapturing = false
                            }
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun CameraPreview(
    isCapturing: Boolean,
    errorMessage: String?,
    photoTip: String?,
    onBack: () -> Unit,
    onCapture: (ImageCapture) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var bindError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewView) {
        runCatching {
            val provider = context.awaitCameraProvider()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
            )
        }.onFailure {
            bindError = "Kamera tidak tersedia di perangkat ini."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = Color.White,
            )
        }

        if (photoTip != null && bindError == null) {
            Text(
                text = photoTip,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = Spacing.xxl, end = Spacing.xxl)
                    .background(Color.Black.copy(alpha = 0.45f), MaterialTheme.shapes.small)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }

        val message = bindError ?: errorMessage
        if (message != null) {
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .border(3.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .background(Color.White, CircleShape)
                    .clickable(enabled = bindError == null && !isCapturing) { onCapture(imageCapture) },
                contentAlignment = Alignment.Center,
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = "Ambil Foto",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CapturedPreview(path: String, onRetake: () -> Unit, onUse: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AsyncImage(
            model = File(path),
            contentDescription = "Pratinjau foto",
            contentScale = ContentScale.Fit,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onRetake,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.weight(1f).height(Dimens.buttonHeight),
            ) {
                Text("Ambil Ulang", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onUse,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f).height(Dimens.buttonHeight),
            ) {
                Text("Gunakan Foto", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CameraPermissionRationale(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Izin kamera diperlukan",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "JalanKita memakai kamera hanya untuk mengambil foto bukti kerusakan jalan.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = if (permanentlyDenied) onOpenSettings else onRequest,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text(if (permanentlyDenied) "Buka Pengaturan" else "Izinkan Kamera")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onBack,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("Kembali")
        }
    }
}

private fun ImageProxy.toJpegBytes(): ByteArray {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener({ continuation.resume(future.get()) }, ContextCompat.getMainExecutor(this))
}
