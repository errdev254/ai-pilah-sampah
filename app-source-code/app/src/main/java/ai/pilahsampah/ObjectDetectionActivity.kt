package ai.pilahsampah

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ai.pilahsampah.ui.screens.DetectionCounts
import ai.pilahsampah.ui.screens.DetectionData
import ai.pilahsampah.ui.screens.DetectionScreen
//import ai.pilahsampah.ui.screens.resetPerfData
import ai.pilahsampah.ui.theme.TrashAppTheme
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.Timer
import java.util.TimerTask

/**
 * Main activity for waste object detection functionality.
 * 
 * This activity handles the camera setup, real-time waste detection,
 * and display of detection results using Jetpack Compose UI. It integrates
 * CameraX for camera preview and the MediaPipe object detection model
 * to identify and categorize waste items into three categories: Organic,
 * Inorganic, and B3 (Hazardous).
 * 
 * Key features:
 * - Real-time camera preview with object detection
 * - Category counting for detected waste items
 * - Performance monitoring (FPS and inference time)
 * - Proper resource management and cleanup
 * 
 * The activity implements ObjectDetectorHelper.DetectorListener to receive
 * detection results and error notifications from the ML model.
 */
@androidx.camera.core.ExperimentalGetImage
class ObjectDetectionActivity : ComponentActivity(), ObjectDetectorHelper.DetectorListener {
    // Camera and detection related properties
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var viewFinder: PreviewView

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // State for Jetpack Compose
    private val detectionDataState = mutableStateOf(DetectionData())
    private val detectionCountsState = mutableStateOf(DetectionCounts())
    private val fpsState = mutableStateOf(0)
    private val inferenceTimeState = mutableStateOf(0L)
    private val isLoadingState = mutableStateOf(true)
    
    // Track image dimensions for proper scaling
    private var imageWidth = 0
    private var imageHeight = 0
    private var imageRotation = 0
    
    // Performance monitoring timer
    private var fpsUpdateTimer: Timer? = null

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // All permissions granted
            startCamera()
        } else {
            // Permission denied
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Sets up the activity, initializes camera and ML components,
     * and configures the Compose UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                cleanupAndFinish()
            }
        })
        
        // Initialize PreviewView with proper scale type for better bounding box alignment
        viewFinder = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER // Changed to FILL_CENTER for better alignment
        }
        
        // Initialize object detector helper
        objectDetectorHelper = ObjectDetectorHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            threshold = 0.6f,
            maxResults = 10,
            currentDelegate = ObjectDetectorHelper.DELEGATE_GPU,
            objectDetectorListener = this
        )

        // Set up Compose UI
        setContent {
            TrashAppTheme {
                val detectionData by remember { detectionDataState }
                val detectionCounts by remember { detectionCountsState }
                val fps by remember { fpsState }
                val inferenceTime by remember { inferenceTimeState }
                val isLoading by remember { isLoadingState }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    DetectionScreen(
                        previewView = viewFinder,
                        detectionData = detectionData,
                        detectionCounts = detectionCounts,
                        onBackClick = { cleanupAndFinish() },
                        fps = fps,
                        inferenceTime = inferenceTime
                    )
                    
                    // Loading overlay
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(56.dp),
                                    color = Color.White,
                                    strokeWidth = 4.dp
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Inisialisasi Kamera...",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
        
        // Start FPS update timer
        startFpsUpdateTimer()
    }

    /**
     * Starts timer to update FPS value in UI at regular intervals.
     */
    private fun startFpsUpdateTimer() {
        fpsUpdateTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    // Update FPS value on UI thread
                    runOnUiThread {
                        fpsState.value = objectDetectorHelper.getCurrentFps()
                    }
                }
            }, 0, 500) // Update every 500ms
        }
    }

    /**
     * Properly cleans up resources before finishing the activity.
     * This includes stopping timers, unbinding camera use cases,
     * shutting down executors, and clearing detector resources.
     */
    private fun cleanupAndFinish() {
        try {
            // Cancel FPS update timer
            fpsUpdateTimer?.cancel()
            fpsUpdateTimer = null
            
            // Unbind use cases before shutting down
            cameraProvider?.unbindAll()
            
            // Shutdown camera executor
            cameraExecutor.shutdown()
            try {
                if (!cameraExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    cameraExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                cameraExecutor.shutdownNow()
            }

            // Clear detector resources
            objectDetectorHelper.clearObjectDetector()

            // Finish activity
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
            finish()
        }
//        resetPerfData()
    }

    /**
     * Initializes and starts the camera using CameraX.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    /**
     * Binds camera use cases (preview and analysis) to the lifecycle.
     * Configures camera for optimal detection performance.
     */
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

        // Set up Preview use case with more resolution options
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()

        // Set up ImageAnalysis use case for object detection with optimized settings
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Drop frames when busy
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setImageQueueDepth(1) // Only process the most recent image
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    objectDetectorHelper::detectLivestreamFrame
                )
            }

        try {
            // Unbind all use cases before binding
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            
            // Set preferred framerate range if supported by device
            camera?.cameraControl?.enableTorch(false) // Ensure torch is off to save power
            
            // Note: We don't use setFrameRateRange as it's only available in newer CameraX versions
            // Instead rely on our frame skipping mechanism in ObjectDetectorHelper
            
            // Set surface provider for preview
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
            
            // Get camera sensor rotation information
            val cameraInfo = camera?.cameraInfo
            cameraInfo?.let {
                // Get the sensor rotation
                val sensorRotation = it.sensorRotationDegrees
                
                // Store rotation for later use
                imageRotation = sensorRotation
                
                Log.d(TAG, "Camera sensor rotation: $sensorRotation")
            }
            
            // Hide loading overlay after camera is ready
            isLoadingState.value = false
            
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            isLoadingState.value = false // Hide loading on error too
        }
    }

    override fun onResume() {
        super.onResume()
        if (objectDetectorHelper.isClosed()) {
            objectDetectorHelper.setupObjectDetector()
        }
    }

    override fun onPause() {
        super.onPause()
        objectDetectorHelper.clearObjectDetector()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupAndFinish()
    }

    /**
     * Checks if all required permissions are granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Called when object detection encounters an error.
     * Handles error display and fallback to CPU if GPU fails.
     * 
     * @param error Error message
     * @param errorCode Error code indicating the type of error
     */
    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            if (errorCode == ObjectDetectorHelper.GPU_ERROR) {
                objectDetectorHelper.currentDelegate = ObjectDetectorHelper.DELEGATE_CPU
                objectDetectorHelper.setupObjectDetector()
            }
        }
    }

    /**
     * Called when object detection produces results.
     * Updates UI with detection data and category counts.
     * 
     * @param resultBundle Bundle containing detection results and metadata
     */
    override fun onResults(resultBundle: ObjectDetectorHelper.ResultBundle) {
        runOnUiThread {
            val results = resultBundle.results[0]
            
            // Store image dimensions for proper scaling
            imageWidth = resultBundle.inputImageWidth
            imageHeight = resultBundle.inputImageHeight
            
            // Store original rotation for debugging if needed
            imageRotation = resultBundle.inputImageRotation
            
            // Update detection data state for compose UI
            // Pass rotation as 0 since the image has already been rotated
            detectionDataState.value = DetectionData(
                detections = results.detections(),
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                rotation = 0 // No further rotation needed in UI since bitmap was pre-rotated
            )
            
            // Update count state for compose UI
            updateCounts(results)
            
            // Update inference time state
            inferenceTimeState.value = resultBundle.inferenceTime
            
            // Calculate and display inference time (useful for performance monitoring)
            val inferenceTime = resultBundle.inferenceTime
            Log.d(TAG, "Inference time: $inferenceTime ms")
        }
    }

    /**
     * Updates detection counts for each waste category based on detection results.
     * 
     * @param results The object detection results from MediaPipe
     */
    private fun updateCounts(results: ObjectDetectorResult) {
        var organikCount = 0
        var anorganikCount = 0
        var b3Count = 0

        results.detections().forEach { detection ->
            when (detection.categories()[0].categoryName().lowercase()) {
                in ORGANIK_LABELS -> organikCount++
                in ANORGANIK_LABELS -> anorganikCount++
                in B3_LABELS -> b3Count++
            }
        }

        detectionCountsState.value = DetectionCounts(
            organik = organikCount,
            anorganik = anorganikCount,
            b3 = b3Count
        )
    }

    companion object {
        private const val TAG = "ObjectDetectionActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private val ORGANIK_LABELS = setOf(
            "cangkang_pala", "daun_kering", "daun_segar", "kantung_teh", "kayu", "kulit_alpukat", "kulit_bawang_putih", "kulit_buah_cokelat", "kulit_buah_naga", "kulit_kacang", "kulit_lemon", "kulit_nanas", "kulit_pisang", "kulit_salak", "kulit_semangka", "kulit_telur", "sabut_kelapa", "tempurung_kelapa", "tongkol_jagung", "tulang_ikan"
        )
        private val ANORGANIK_LABELS = setOf(
            "botol", "bubblewrap", "busa", "gelas_plastik", "garpu", "kantung_plastik", "kaleng", "kardus", "kertas", "kemasan_plastik", "mika_plastik", "pipa", "pulpen", "sandal", "sepatu", "sendok", "sisir", "styrofoam", "thinwall", "seng"
        )
        private val B3_LABELS = setOf(
            "aerosol", "baterai", "botol_infus", "kemasan_salep", "lampu", "masker", "obat_obatan_strip", "ponsel", "suntik", "termometer"
        )
    }
} 