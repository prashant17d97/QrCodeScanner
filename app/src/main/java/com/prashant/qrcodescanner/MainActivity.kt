package com.prashant.qrcodescanner

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.prashant.qrcodescanner.ui.theme.QrCodeScannerTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var shouldShowCamera = mutableStateOf(false)

    override fun onDestroy() {
        super.onDestroy()
        shouldShowCamera.value = false
        cameraExecutor.shutdown()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QrCodeScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestCameraPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()
        requestCameraPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @Composable
    fun Greeting() {
        var value by remember {
            mutableStateOf("Prashant")
        }
        var flash by remember {
            mutableStateOf(false)
        }
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value)
            Spacer(modifier = Modifier.height(50.dp))
            MyScreen(
                flashBool = flash,
                onFlashChange = {
                    flash = it
                },
                previewView = {
                    if (shouldShowCamera.value) {
                        startCamera(cameraPreview = it, context = context, flash = flash) { code ->
                            value = code
                        }
                    }
                    requestCameraPermission()
                    cameraExecutor = Executors.newSingleThreadExecutor()
                }
            )

        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("kilo", "Permission granted")
            shouldShowCamera.value = true // 👈🏻
        }
    }

    private fun requestCameraPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this, CAMERA
            ) -> {
                shouldShowCamera.value = true
                Log.i("kilo", "Permission previously granted")
            }

            /* ActivityCompat.shouldShowRequestPermissionRationale(
                 this,
                 CAMERA
             ) -> {
                 Log.i("kilo", "Show camera permissions dialog")
             }*/
            else -> requestPermissionLauncher.launch(CAMERA)
        }
    }


    private fun startCamera(
        cameraPreview: PreviewView,
        context: Context,
        flash: Boolean,
        code: (String) -> Unit
    ) {

        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }

            // Image analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(
                        cameraExecutor,
                        QrCodeAnalyzer(onBarCodeScannerSuccess = { data ->
                            //it.clearAnalyzer()
                            //cameraProvider.unbindAll()
                            code(data)

                            Log.e("Scanner", "Scanner Success: Data -> $data")

                        }, onBarCodeScannerFailed = { exception ->
                            Log.e("Scanner", "Scanner Failed: ${exception.message}")
                        }),
                    )
                }

            // Select back camera as a default
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @Composable
    fun MyScreen(
        flashBool: Boolean,
        onFlashChange: (Boolean) -> Unit,
        previewView: (PreviewView) -> Unit,
    ) {

        Box(
            modifier = Modifier
                .size(300.dp)
                .border(width = 4.dp, color = Color.Red, shape = RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LayoutInflater.from(context).inflate(R.layout.my_layout, null)
                },
                update = { view ->
                    previewView(view.findViewById(R.id.preview_view))
                }
            )

        }
        Image(
            painter = painterResource(id = R.drawable.ic_flash_on.takeIf { flashBool }
                ?: R.drawable.ic_flash_off),
            contentDescription = "",
            modifier = Modifier
                .size(50.dp)
                .padding(10.dp)
                .clickable {
                    onFlashChange(!flashBool)
                }
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        QrCodeScannerTheme {
            Greeting()
        }
    }
}
