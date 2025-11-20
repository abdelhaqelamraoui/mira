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
import androidx.compose.material3.Text
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

    // This is the PreviewView that will be embedded in the composable
    val previewView = remember { PreviewView(context) }

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
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformableState)
            // ▼▼▼ NEW: TAP-TO-FOCUS LOGIC ▼▼▼
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        camera?.let {
                            // 1. Create a MeteringPoint from the tap coordinates
                            val meteringPoint = previewView.meteringPointFactory
                                .createPoint(offset.x, offset.y)

                            // 2. Create a FocusingAction from the MeteringPoint
                            val action = FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF)
                                .build()

                            // 3. Trigger the focus action on the camera
                            it.cameraControl.startFocusAndMetering(action)
                        }
                    }
                )
            }
        // ▲▲▲ END OF NEW LOGIC ▲▲▲
    ) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            previewView = previewView, // Pass the PreviewView instance
            onCameraReady = { cameraInstance ->
                camera = cameraInstance
            }
        )
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    previewView: PreviewView, // Accept the PreviewView from the parent
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
            // Handle errors
        }
    }

    AndroidView({ previewView }, modifier)
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    val executor = ContextCompat.getMainExecutor(this)
    cameraProviderFuture.addListener({
        continuation.resume(cameraProviderFuture.get())
    }, executor)
}
