package com.prashant.qrcodescanner

import android.Manifest.permission.CAMERA
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Camera
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.green
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value)
            Spacer(modifier = Modifier.height(50.dp))
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .border(width = 4.dp, color = Color.Red, shape = RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (shouldShowCamera.value) {
                    CameraView(
                        modifier = Modifier
                            .size(350.dp)
                            .padding(5.dp),
                        onValueChane = { value = it }
                    )
                }
                requestCameraPermission()
                cameraExecutor = Executors.newSingleThreadExecutor()
            }

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


    @Composable
    fun CameraView(
        modifier: Modifier = Modifier,
        onValueChane: (String) -> Unit
    ) {
        // 1
        val lensFacing = CameraSelector.LENS_FACING_BACK
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        val preview = androidx.camera.core.Preview.Builder().setTargetResolution(Size(1080, 1080)) .build()
        val cameraProvider = cameraProviderFuture.get()
        val previewView = remember { PreviewView(context) }
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        /*
        *  private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder()
                .setTargetResolution(Size(1080, 1080)) // Set preview size here
                .build()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.cameraInfo))
        }, ContextCompat.getMainExecutor(context))
    }*/
        // 2
        LaunchedEffect(lensFacing) {

            cameraProviderFuture.addListener({

                // Image analyzer
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    //.setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                    //.setImageQueueDepth(1)
                    .setTargetResolution(Size(720, 720))

                    .build().also {
                        it.setAnalyzer(
                            cameraExecutor,
                            QrCodeAnalyzer(onBarCodeScannerSuccess = { data ->
                                //it.clearAnalyzer()
                                //cameraProvider.unbindAll()
                                Log.e("Scanner", "Scanner Success: Data -> $data")
                                onValueChane(data.trim())

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
                        lifecycleOwner, cameraSelector, preview, imageAnalyzer
                    )

                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
            preview.setSurfaceProvider(previewView.createSurfaceProvider((cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalyzer
            )).cameraInfo))
        }

        // 3
        Box(
            modifier = modifier
                .border(width = 0.dp, color = Color.Transparent, shape = RoundedCornerShape(20.dp))
        ) {
            AndroidView({ previewView }, modifier = modifier
                .border(
                    width = 0.dp,
                    color = Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                ),
                update = {
                    it.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    it.scaleType = PreviewView.ScaleType.FILL_CENTER
                })
        }

    }



    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        QrCodeScannerTheme {
            Greeting()
        }
    }
}
