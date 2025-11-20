package com.ostordev.mira

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            // This state will track if we have camera permission
            var hasCamPermission by remember { mutableStateOf(false) }

            // This is the new way to handle permissions in Compose
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { granted ->
                    hasCamPermission = granted
                }
            )

            // Request permission when the composable is first launched
            LaunchedEffect(key1 = true) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }

            // --- UI LOGIC ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black), // <-- THIS IS THE FIX
                contentAlignment = Alignment.Center
            ) {
                if (hasCamPermission) {
                    // If permission is granted, show the camera screen
                    CameraScreen()
                } else {
                    // If permission is denied, show a message (now visible)
                    Text(text = "Please grant camera permission.", color = Color.White)
                }
            }
        }
    }
}


@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for the camera instance and the zoom ratio
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    // This state is kept for the transformable logic
    var zoomRatio by remember { mutableStateOf(1f) }

    // State for handling pinch-to-zoom gestures
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        camera?.let {
            val currentZoom = it.cameraInfo.zoomState.value?.zoomRatio ?: 1f
            val newZoom = (currentZoom * zoomChange).coerceIn(
                it.cameraInfo.zoomState.value?.minZoomRatio ?: 1f,
                it.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
            )
            zoomRatio = newZoom // Update the state
            it.cameraControl.setZoomRatio(newZoom) // Apply to the camera
        }
    }

    // This Box will contain our camera preview
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Apply the transformable modifier to detect gestures
            .transformable(state = transformableState)
    ) {
        // This composable handles setting up the camera preview
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onCameraReady = { cameraInstance ->
                camera = cameraInstance
            }
        )
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier, onCameraReady: (androidx.camera.core.Camera) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    // This launches a side-effect that sets up the camera
    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll() // Unbind previous use cases

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // Use the back camera
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Bind the camera use cases to the lifecycle
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                ImageCapture.Builder().build() // We need an ImageCapture use case for zoom to work reliably
            )
            onCameraReady(camera) // Pass the camera instance back
        } catch (exc: Exception) {
            // Handle errors (e.g., no camera available)
        }
    }

    // Embed the Android-native PreviewView into our Compose UI
    AndroidView({ previewView }, modifier)
}

// A helper suspend function to get the CameraProvider instance
private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    val executor = ContextCompat.getMainExecutor(this)
    cameraProviderFuture.addListener({
        continuation.resume(cameraProviderFuture.get())
    }, executor)
}
