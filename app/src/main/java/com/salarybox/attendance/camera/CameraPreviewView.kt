package com.salarybox.attendance.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Controller providing handle to capture bitmaps from the active camera.
 */
class CameraCaptureController {
    internal var onCaptureRequested: ((onSuccess: (Bitmap) -> Unit, onError: (Throwable) -> Unit) -> Unit)? = null

    fun capturePhoto(onSuccess: (Bitmap) -> Unit, onError: (Throwable) -> Unit) {
        onCaptureRequested?.invoke(onSuccess, onError)
    }
}

@Composable
fun rememberCameraCaptureController(): CameraCaptureController {
    return remember { CameraCaptureController() }
}

/**
 * Production-ready reusable CameraX component.
 * Uses front-facing camera, handles permissions, lifecycle binding,
 * frame analysis with backpressure protection, and bitmap capture.
 */
@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    controller: CameraCaptureController? = null,
    onFrameAnalyzed: ((Bitmap) -> Unit)? = null,
    onCameraError: ((String) -> Unit)? = null,
    overlayContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onCameraError?.invoke("Camera permission is required.")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Camera access is required to mark attendance.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Camera Permission")
                }
            }
        }
        return
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isAnalyzing = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setTargetRotation(previewView.display?.rotation ?: 0)
                            .build()

                        controller?.onCaptureRequested = { onSuccess, onError ->
                            imageCapture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        try {
                                            val rawBitmap = image.toBitmap()
                                            val rotation = image.imageInfo.rotationDegrees

                                            // Correct rotation and mirror front-camera image
                                            val matrix = Matrix().apply {
                                                postRotate(rotation.toFloat())
                                                postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)
                                            }
                                            val correctedBitmap = Bitmap.createBitmap(
                                                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                            )
                                            onSuccess(correctedBitmap)
                                        } catch (t: Throwable) {
                                            onError(t)
                                        } finally {
                                            image.close()
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        onError(exception)
                                    }
                                }
                            )
                        }

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        // Setup image analysis for real-time face guidance if listener provided
                        if (onFrameAnalyzed != null) {
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (isAnalyzing.compareAndSet(false, true)) {
                                    try {
                                        val rawBitmap = imageProxy.toBitmap()
                                        val rotation = imageProxy.imageInfo.rotationDegrees
                                        val matrix = Matrix().apply {
                                            postRotate(rotation.toFloat())
                                            postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)
                                        }
                                        val correctedBitmap = Bitmap.createBitmap(
                                            rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                        )
                                        onFrameAnalyzed(correctedBitmap)
                                    } catch (_: Exception) {
                                    } finally {
                                        isAnalyzing.set(false)
                                        imageProxy.close()
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                        } else {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        }
                    } catch (e: Exception) {
                        onCameraError?.invoke("Failed to initialize camera: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Overlay slot
        overlayContent()
    }
}
