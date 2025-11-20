package com.ostordev.mira

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            var hasCamPermission by remember { mutableStateOf(false) }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { granted ->
                    hasCamPermission = granted
                }
            )

            LaunchedEffect(key1 = true) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (hasCamPermission) {
                    CameraScreen()
                } else {
                    Text(text = "Please grant camera permission.", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var zoomRatio by remember { mutableStateOf(1f) }
    val previewView = remember { PreviewView(context) }

    // ▼▼▼ NEW: State to control the one-time dialog ▼▼▼
    var showFlashDialog by remember { mutableStateOf(true) }

    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        camera?.let {
            val currentZoom = it.cameraInfo.zoomState.value?.zoomRatio ?: 1f
            val newZoom = (currentZoom * zoomChange).coerceIn(
                it.cameraInfo.zoomState.value?.minZoomRatio ?: 1f,
                it.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
            )
            zoomRatio = newZoom
            it.cameraControl.setZoomRatio(newZoom)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CameraPreview(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformableState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            camera?.let {
                                val meteringPoint = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF).build()
                                it.cameraControl.startFocusAndMetering(action)
                            }
                        }
                    )
                },
            previewView = previewView,
            onCameraReady = { cameraInstance ->
                camera = cameraInstance
            }
        )

        // ▼▼▼ NEW: Show the AlertDialog when needed ▼▼▼
        // This condition checks if the dialog should be shown. It's only true once.
        if (showFlashDialog && camera?.cameraInfo?.hasFlashUnit() == true) {
            AlertDialog(
                onDismissRequest = {
                    // If the user dismisses it (e.g., taps outside), treat it as "No".
                    showFlashDialog = false
                },
                title = { Text("Flashlight") },
                text = { Text("Do you want to turn the flashlight on?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Turn the flash on and dismiss the dialog.
                            camera?.cameraControl?.enableTorch(true)
                            showFlashDialog = false
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            // Leave the flash off and dismiss the dialog.
                            showFlashDialog = false
                        }
                    ) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    previewView: PreviewView,
    onCameraReady: (androidx.camera.core.Camera) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        val context = previewView.context
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                ImageCapture.Builder().build()
            )
            onCameraReady(camera)
        } catch (exc: Exception) {
            Log.e("CameraPreview", "Failed to bind camera", exc)
        }
    }

    AndroidView({ previewView }, modifier = modifier)
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    val executor = ContextCompat.getMainExecutor(this)
    cameraProviderFuture.addListener({
        continuation.resume(cameraProviderFuture.get())
    }, executor)
}
