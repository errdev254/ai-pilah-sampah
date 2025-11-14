package ai.pilahsampah

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import ai.pilahsampah.BuildConfig
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
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
import ai.pilahsampah.utils.PerformanceMonitor

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
    
    // Adaptive resolution management
    private val availableResolutions = listOf(Size(1280, 720), Size(960, 540), Size(640, 480))
    private var resolutionIndex = 0
    private var currentTargetResolution: Size = availableResolutions[resolutionIndex]
    private var emaInferenceMs: Double = -1.0
    private var lastResolutionChangeMs: Long = 0
    private val resolutionChangeCooldownMs: Long = 5000
    
    // Delegate toggle state (GPU default for performance)
    private val delegateState = mutableStateOf(ObjectDetectorHelper.DELEGATE_GPU)
    
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
            // Permission denied - show explanation and graceful exit
            Toast.makeText(this, "Kamera diperlukan untuk deteksi sampah. Aplikasi akan ditutup.", Toast.LENGTH_LONG).show()
            // Add delay before finishing to let user read the message
            viewFinder.postDelayed({ finish() }, 2000)
        }
    }

    /**
     * Sets up the activity, initializes camera and ML components,
     * and configures the Compose UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            )
        )
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
            scaleType = PreviewView.ScaleType.FILL_START // Match MediaPipe sample (FILL_START) for consistent mapping
        }
        
        // Initialize object detector helper
        objectDetectorHelper = ObjectDetectorHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            threshold = 0.6f,
            maxResults = 10,
            currentDelegate = delegateState.value,
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
                        inferenceTime = inferenceTime,
                        delegate = delegateState.value,
                        onDelegateChange = { newDelegate ->
                            if (delegateState.value != newDelegate) {
                                delegateState.value = newDelegate
                                objectDetectorHelper.currentDelegate = newDelegate
                                // Reinitialize detector on a background thread to keep UI responsive
                                Thread {
                                    objectDetectorHelper.setupObjectDetector()
                                }.start()
                            }
                        }
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
                        try {
                            if (::objectDetectorHelper.isInitialized) {
                                fpsState.value = objectDetectorHelper.getCurrentFps()
                                // Hide loading once detector is ready
                                if (isLoadingState.value && objectDetectorHelper.isReady()) {
                                    isLoadingState.value = false
                                }
                            }
                        } catch (e: Exception) {
                            // Detector might not be initialized yet
                            if (BuildConfig.DEBUG) Log.d(TAG, "Detector not ready: ${e.message}")
                        }
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
            if (BuildConfig.DEBUG) Log.e(TAG, "Error during cleanup: ${e.message}")
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
        // Rebind camera with current target resolution
        currentTargetResolution = availableResolutions[resolutionIndex]
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Set up Preview use case with safe 720p resolution (avoid mixing with setTargetAspectRatio)
        preview = Preview.Builder()
            .setTargetResolution(currentTargetResolution)
            .build()

            // Set up ImageAnalysis use case for object detection with optimized settings
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(currentTargetResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Drop frames when busy
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setImageQueueDepth(1) // Only process the most recent image
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    ImageAnalysis.Analyzer { imageProxy ->
                        // Only process if detector is ready
                        if (::objectDetectorHelper.isInitialized && objectDetectorHelper.isReady()) {
                            objectDetectorHelper.detectLivestreamFrame(imageProxy)
                        } else {
                            // If detector not ready, close the frame
                            // This is expected during initialization or after resume
                            imageProxy.close()
                        }
                    }
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
                
                if (BuildConfig.DEBUG) Log.d(TAG, "Camera sensor rotation: $sensorRotation")
            }
            
            // Hide loading overlay after camera is ready
            isLoadingState.value = false
            
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Use case binding failed", e)
            Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            isLoadingState.value = false // Hide loading on error too
        }
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG) Log.d(TAG, "onResume called - checking detector state")
        
        // Always check if detector needs reinitialization after resume
        // The detector might have been cleared in onPause
        val needsReinit = objectDetectorHelper.isClosed() || !objectDetectorHelper.isReady()
        if (BuildConfig.DEBUG) Log.d(TAG, "Detector state - closed: ${objectDetectorHelper.isClosed()}, ready: ${objectDetectorHelper.isReady()}, needs reinit: $needsReinit")
        
        if (needsReinit) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Reinitializing detector on resume")
            isLoadingState.value = true
            
            // Reinitialize detector on background thread
            Thread {
                try {
                    objectDetectorHelper.setupObjectDetector()
                    if (BuildConfig.DEBUG) Log.d(TAG, "Detector setup completed on resume")
                    
                    // Hide loading once detector is ready
                    runOnUiThread {
                        // Poll for detector readiness with exponential backoff
                        var delay = 500L
                        var attempts = 0
                        val maxAttempts = 6 // Total max wait ~3 seconds
                        
                        fun checkReady() {
                            attempts++
                            if (objectDetectorHelper.isReady()) {
                                if (BuildConfig.DEBUG) Log.d(TAG, "Detector ready after $attempts attempts")
                                isLoadingState.value = false
                            } else if (attempts < maxAttempts) {
                                // Try again with increased delay
                                viewFinder.postDelayed({
                                    checkReady()
                                }, delay)
                                delay = (delay * 1.5).toLong() // Exponential backoff
                            } else {
                                // Max attempts reached, hide loading anyway
                                if (BuildConfig.DEBUG) Log.w(TAG, "Detector not ready after max attempts, hiding loading")
                                isLoadingState.value = false
                            }
                        }
                        
                        checkReady()
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Error reinitializing detector on resume: ${e.message}", e)
                    runOnUiThread {
                        isLoadingState.value = false
                    }
                }
            }.start()
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Detector already ready, no reinitialization needed")
        }
    }

    override fun onPause() {
        super.onPause()
        if (BuildConfig.DEBUG) Log.d(TAG, "onPause called")
        // Clear detector when going to background to free resources
        objectDetectorHelper.clearObjectDetector()
        if (BuildConfig.DEBUG) Log.d(TAG, "Detector cleared in onPause")
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
                // GPU failed or not available: fall back to CPU and update toggle state
                delegateState.value = ObjectDetectorHelper.DELEGATE_CPU
                objectDetectorHelper.currentDelegate = ObjectDetectorHelper.DELEGATE_CPU
                Thread {
                    objectDetectorHelper.setupObjectDetector()
                }.start()
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
            
            // Store original rotation for overlay mapping
            imageRotation = resultBundle.inputImageRotation
            
            // Update detection data state for compose UI
            detectionDataState.value = DetectionData(
                detections = results.detections(),
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                rotation = imageRotation
            )
            
            // Update count state for compose UI
            updateCounts(results)
            
            // Update inference time state and adaptive resolution logic
            val inferenceTime = resultBundle.inferenceTime
            inferenceTimeState.value = inferenceTime
            updateAdaptiveResolution(inferenceTime)
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Inference time: $inferenceTime ms")
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

    private fun updateAdaptiveResolution(inferenceTimeMs: Long) {
        val now = SystemClock.uptimeMillis()
        // EMA with alpha tuned for ~1s half-life
        val alpha = 0.2
        emaInferenceMs = if (emaInferenceMs < 0) inferenceTimeMs.toDouble() else (alpha * inferenceTimeMs + (1 - alpha) * emaInferenceMs)

        val upscaleThreshold = 55 // ms
        val downscaleThreshold = 85 // ms

        // Cooldown to avoid rapid thrash
        if (now - lastResolutionChangeMs < resolutionChangeCooldownMs) return

        if (emaInferenceMs > downscaleThreshold && resolutionIndex < availableResolutions.size - 1) {
            resolutionIndex++
            lastResolutionChangeMs = now
            if (BuildConfig.DEBUG) Log.d(TAG, "Adaptive resolution: downscaling to ${availableResolutions[resolutionIndex]}")
            // Rebind camera with lower resolution
            bindCameraUseCases()
        } else if (emaInferenceMs < upscaleThreshold && resolutionIndex > 0) {
            resolutionIndex--
            lastResolutionChangeMs = now
            if (BuildConfig.DEBUG) Log.d(TAG, "Adaptive resolution: upscaling to ${availableResolutions[resolutionIndex]}")
            // Rebind camera with higher resolution
            bindCameraUseCases()
        }
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