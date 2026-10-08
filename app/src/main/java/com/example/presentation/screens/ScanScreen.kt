package com.example.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.NextPayApplication
import com.example.domain.UpiData
import com.example.domain.UpiParser
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    onQrScanned: (UpiData) -> Unit,
    modifier: Modifier = Modifier
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
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Photo picker for selecting QR image from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = NextPayApplication.instance.qrScannerManager.decodeFromUri(context, uri)
            if (decoded != null) {
                onQrScanned(decoded)
            }
        }
    }

    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraControl: androidx.camera.core.CameraControl? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
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

                        val analyzer = NextPayApplication.instance.qrScannerManager.createAnalyzer { upiData ->
                            onQrScanned(upiData)
                        }
                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission request view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Grant camera access to scan any UPI QR code instantly offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }

        // Viewfinder Target Box Overlay
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                        RoundedCornerShape(24.dp)
                    )
                    .testTag("qr_scanner_viewfinder"),
                contentAlignment = Alignment.Center
            ) {
                // Subtle crosshair
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Point camera at any UPI QR code",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Controls Bar at Bottom
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Torch Toggle
                IconButton(
                    onClick = {
                        torchEnabled = !torchEnabled
                        cameraControl?.enableTorch(torchEnabled)
                    },
                    modifier = Modifier.testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight",
                        tint = if (torchEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Pick from Gallery
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("gallery_picker_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload QR", fontSize = 13.sp)
                }

                // Enter Manually
                OutlinedButton(
                    onClick = { showManualInputDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("manual_entry_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual", fontSize = 13.sp)
                }
            }
        }
    }

    // Manual input dialog fallback
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = { Text("Enter UPI ID or Paste Code") },
            text = {
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it },
                    label = { Text("UPI Link or ID") },
                    placeholder = { Text("upi://pay?... or name@bank") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = UpiParser.parse(manualText)
                        if (parsed != null) {
                            showManualInputDialog = false
                            onQrScanned(parsed)
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Use This")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showManualInputDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
