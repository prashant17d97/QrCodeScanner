package com.prashant.qrcodescanner

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    // private val findNavController: NavController,
    private val onBarCodeScannerSuccess: (String) -> Unit,
    private val onBarCodeScannerFailed: (Exception) -> Unit
) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        Log.e("image", image.imageInfo.timestamp.toString())
        val img = image.image
        if (img != null) {
            val inputImage = InputImage.fromMediaImage(img, image.imageInfo.rotationDegrees)

            // Process image searching for barcodes
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_ALL_FORMATS,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_CODE_128
                )
                .build()

            val scanner = BarcodeScanning.getClient(options)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        Log.e("barcode", "${barcode.displayValue}")
//                        val displayValue = barcode.displayValue as String
                        barcode.displayValue?.let { onBarCodeScannerSuccess(it) }
                        // isScanned = true
                        //findNavController.popBackStack()
                    }
                }
                .addOnFailureListener {
                    onBarCodeScannerFailed(it)
                }
                .addOnCompleteListener {
                    image.close()
                    img.close()
                }
        }

        //image.close()
    }
}