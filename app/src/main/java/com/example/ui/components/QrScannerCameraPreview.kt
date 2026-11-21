package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.i18n.tr
import com.example.ui.theme.ParkoBlue
import com.example.ui.theme.ParkoCharcoal
import com.example.ui.theme.ParkoEmerald

@Composable
fun QrScannerCameraBox(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraAvailable by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ParkoCharcoal),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission && cameraAvailable) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview
                            )
                        } catch (_: Exception) {
                            cameraAvailable = false
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = tr("Viseur QR", "QR Viewfinder"),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = if (!hasCameraPermission) {
                        tr(
                            "Autorisez la caméra pour scanner les tickets QR directement.",
                            "Allow camera access to scan QR tickets directly."
                        )
                    } else {
                        tr(
                            "Caméra indisponible sur cet appareil — utilisez la recherche ou sélection rapide ci-dessous.",
                            "Camera unavailable on this device — use search or quick selection below."
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center
                )
                if (!hasCameraPermission) {
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("grant_camera_button")
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Activer la caméra", "Enable camera"))
                    }
                }
            }
        }

        // Scanning reticle frame overlay
        Box(
            modifier = Modifier
                .size(196.dp)
                .border(
                    width = 3.dp,
                    color = if (hasCameraPermission && cameraAvailable) ParkoEmerald else ParkoBlue,
                    shape = RoundedCornerShape(22.dp)
                )
        )
    }
}
