package com.example.qrcanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.qrcanner.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isScanning = false
    private var isLaserAnimating = false
    
    // CameraX components
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    
    // Background executor
    private lateinit var cameraExecutor: ExecutorService
    
    // Barcode scanner from ML Kit
    private val barcodeScanner = BarcodeScanning.getClient()
    
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupUI()
        
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    private fun setupUI() {
        // Scan button click listener
        binding.scanButton.setOnClickListener {
            if (!isScanning) {
                startScanning()
            } else {
                stopScanning()
            }
        }
        
        // Copy button click listener
        binding.copyButton.setOnClickListener {
            val resultText = binding.resultText.text.toString()
            if (resultText.isNotEmpty() && resultText != getString(R.string.no_result)) {
                copyToClipboard(resultText)
            } else {
                Toast.makeText(this, "Сначала отсканируйте код", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Grant permission button
        binding.grantPermissionButton.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                showPermissionDeniedView()
            }
        }
    }
    
    private fun showPermissionDeniedView() {
        binding.cameraCard.visibility = View.GONE
        binding.resultCard.visibility = View.GONE
        binding.buttonContainer.visibility = View.GONE
        binding.permissionDeniedView.visibility = View.VISIBLE
    }
    
    private fun startCamera() {
        binding.permissionDeniedView.visibility = View.GONE
        binding.cameraCard.visibility = View.VISIBLE
        binding.resultCard.visibility = View.VISIBLE
        binding.buttonContainer.visibility = View.VISIBLE
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Preview use case
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }
                
                // Image analysis use case for barcode scanning
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processBarcode(imageProxy)
                        }
                    }
                
                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                
                // Start scanning automatically
                startScanning()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Ошибка запуска камеры: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun processBarcode(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val barcode = barcodes.first()
                    val rawValue = barcode.rawValue
                    
                    if (!rawValue.isNullOrEmpty()) {
                        runOnUiThread {
                            displayResult(rawValue, barcode.formatToString())
                            
                            // Vibrate or play sound (optional)
                            // You can add haptic feedback here
                            
                            // Pause scanning briefly
                            pauseScanning()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    
    private fun displayResult(result: String, format: String) {
        binding.resultText.text = result
        binding.barcodeType.text = "Тип: ${getBarcodeFormatName(format)}"
        binding.barcodeType.visibility = View.VISIBLE
        
        // Show success animation
        binding.resultCard.setCardBackgroundColor(getColor(R.color.success))
        
        // Reset card color after delay
        binding.resultCard.postDelayed({
            binding.resultCard.setCardBackgroundColor(getColor(android.R.color.white))
        }, 500)
    }
    
    private fun getBarcodeFormatName(format: String): String {
        return when (format) {
            "QR_CODE" -> "QR Code"
            "EAN_13" -> "EAN-13"
            "EAN_8" -> "EAN-8"
            "UPC_A" -> "UPC-A"
            "UPC_E" -> "UPC-E"
            "CODE_128" -> "Code 128"
            "CODE_39" -> "Code 39"
            "CODE_93" -> "Code 93"
            "ITF" -> "ITF"
            "AZTEC" -> "Aztec"
            "DATA_MATRIX" -> "Data Matrix"
            "PDF_417" -> "PDF417"
            else -> format
        }
    }
    
    private fun startScanning() {
        isScanning = true
        binding.scanButton.text = getString(R.string.stop_scan)
        binding.resultText.text = getString(R.string.no_result)
        binding.barcodeType.visibility = View.GONE
        binding.resultCard.setCardBackgroundColor(getColor(android.R.color.white))
        
        // Start laser animation
        startLaserAnimation()
    }
    
    private fun stopScanning() {
        isScanning = false
        binding.scanButton.text = getString(R.string.start_scan)
        stopLaserAnimation()
    }
    
    private fun pauseScanning() {
        // Briefly pause scanning after successful scan
        stopLaserAnimation()
        binding.scanButton.isEnabled = false
        
        binding.scanButton.postDelayed({
            if (isScanning) {
                binding.scanButton.isEnabled = true
                startLaserAnimation()
            }
        }, 1000)
    }
    
    private fun startLaserAnimation() {
        if (!isLaserAnimating) {
            isLaserAnimating = true
            binding.laserLine.visibility = View.VISIBLE
            
            val laserAnimation = AnimationUtils.loadAnimation(this, R.anim.laser_animation)
            binding.laserLine.startAnimation(laserAnimation)
        }
    }
    
    private fun stopLaserAnimation() {
        isLaserAnimating = false
        binding.laserLine.clearAnimation()
        binding.laserLine.visibility = View.GONE
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR/Barcode Result", text)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
        stopLaserAnimation()
    }
}
