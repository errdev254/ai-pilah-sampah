package ai.pilahsampah

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import ai.pilahsampah.BuildConfig
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageAnalysis
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Helper class for object detection using MediaPipe Object Detector.
 *
 * This class provides an interface to the MediaPipe Object Detector for detecting objects
 * in images and camera frames. It handles the initialization of the detector, processing of
 * images, and cleanup of resources.
 *
 * The class supports different running modes (LIVE_STREAM, IMAGE) and hardware acceleration
 * options (CPU, GPU).
 *
 * @property context The Android application context
 * @property runningMode The mode in which the detector operates (IMAGE or LIVE_STREAM)
 * @property threshold The confidence threshold for detections (0.0-1.0)
 * @property maxResults The maximum number of detection results to return
 * @property currentDelegate The hardware delegate to use (CPU or GPU)
 * @property objectDetectorListener Listener for detection results and errors
 */
class ObjectDetectorHelper(
    val context: Context,
    val runningMode: RunningMode = RunningMode.IMAGE,
    var threshold: Float = 0.5f,
    var maxResults: Int = 5,
    var currentDelegate: Int = DELEGATE_GPU,
    val objectDetectorListener: DetectorListener? = null
) {

    private var objectDetector: ObjectDetector? = null
    private var imageRotation = 0
    private lateinit var imageProcessingOptions: ImageProcessingOptions
    private var lastProcessingTimeMs = 0L
    private var frameProcessedInOneSecond = 0
    private var lastFpsTimestamp = 0L
    private var currentFps = 0

    // Thread safety
    private val detectorLock = ReentrantLock()
    private val isInitializing = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)

    init {
        // Initialize detector asynchronously to avoid blocking main thread
        setupObjectDetectorAsync()
    }

    /**
     * Returns the current frames per second rate
     */
    fun getCurrentFps(): Int {
        return currentFps
    }

    /**
     * Clears and closes the ObjectDetector to free resources
     */
    fun clearObjectDetector() {
        detectorLock.withLock {
            try {
                objectDetector?.close()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error closing detector: ${e.message}")
                }
            }
            objectDetector = null
            isInitialized.set(false)
        }
    }

    /**
     * Sets up the ObjectDetector asynchronously on a background thread
     */
    private var initThread: android.os.HandlerThread? = null
    private var initHandler: android.os.Handler? = null

    private fun setupObjectDetectorAsync() {
        if (isInitializing.get()) {
            return // Already initializing
        }

        isInitializing.set(true)

        // Use a dedicated HandlerThread with a prepared Looper
        if (initThread == null || !initThread!!.isAlive) {
            initThread = android.os.HandlerThread("MP-Init", android.os.Process.THREAD_PRIORITY_DISPLAY).apply { start() }
            initHandler = android.os.Handler(initThread!!.looper)
        }
        initHandler?.post {
            setupObjectDetector()
        }
    }

    /**
     * Sets up the ObjectDetector with the current configuration
     * This should be called from a background thread for GPU initialization
     * Can be called from any thread, but recommended from background thread
     */
    fun setupObjectDetector() {
        detectorLock.withLock {
            // Clear existing detector first
            try {
                objectDetector?.close()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error closing previous detector: ${e.message}")
                }
            }
            objectDetector = null
            isInitialized.set(false)

            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath("model_pemilah_sampah.tflite")

            when (currentDelegate) {
                DELEGATE_CPU -> {
                    baseOptionsBuilder.setDelegate(Delegate.CPU)
                }
                DELEGATE_GPU -> {
                    baseOptionsBuilder.setDelegate(Delegate.GPU)
                }
            }

            try {
                val baseOptions = baseOptionsBuilder.build()
                val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setScoreThreshold(threshold)
                    .setMaxResults(maxResults)
                    .setRunningMode(runningMode)

                if (runningMode == RunningMode.LIVE_STREAM) {
                    optionsBuilder
                        .setResultListener(this::returnLivestreamResult)
                        .setErrorListener(this::returnLivestreamError)
                }

                val options = optionsBuilder.build()
                objectDetector = ObjectDetector.createFromOptions(context, options)
                // Initialize image processing options for rotation handling
                // We pre-rotate the bitmap before creating MPImage, so keep rotation at 0 here
                imageProcessingOptions = ImageProcessingOptions.builder()
                    .setRotationDegrees(0)
                    .build()
                isInitialized.set(true)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "ObjectDetector initialized successfully")
                } else {
                    objectDetectorListener?.onError(
                        "Object detector initialized successfully",
                        OTHER_ERROR
                    )
                }
            } catch (e: IllegalStateException) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "TFLite failed to load model with error: " + e.message)
                }

                // Try CPU fallback if GPU failed
                if (currentDelegate == DELEGATE_GPU) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Attempting CPU fallback")
                    }
                    currentDelegate = DELEGATE_CPU
                    try {
                        val baseOptions = BaseOptions.builder()
                            .setModelAssetPath("model_pemilah_sampah.tflite")
                            .setDelegate(Delegate.CPU)
                            .build()
                        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setScoreThreshold(threshold)
                            .setMaxResults(maxResults)
                            .setRunningMode(runningMode)
                        if (runningMode == RunningMode.LIVE_STREAM) {
                            optionsBuilder
                                .setResultListener(this::returnLivestreamResult)
                                .setErrorListener(this::returnLivestreamError)
                        }
                        objectDetector = ObjectDetector.createFromOptions(context, optionsBuilder.build())
                        isInitialized.set(true)
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "ObjectDetector initialized with CPU fallback")
                        }
                        objectDetectorListener?.onError(
                            "GPU not available, using CPU",
                            GPU_ERROR
                        )
                    } catch (fallbackError: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "CPU fallback also failed: ${fallbackError.message}")
                        }
                        objectDetectorListener?.onError(
                            "Object detector failed to initialize. See error logs for details",
                            GPU_ERROR
                        )
                    }
                } else {
                    objectDetectorListener?.onError(
                        "Object detector failed to initialize. See error logs for details",
                        GPU_ERROR
                    )
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Unexpected error initializing detector: ${e.message}", e)
                }
                objectDetectorListener?.onError(
                    "Object detector initialization error: ${e.message}",
                    OTHER_ERROR
                )
            } finally {
                isInitializing.set(false)
            }
        }
    }

    /**
     * Processes a frame from the camera for object detection
     * Handles rotation and conversion to the format required by MediaPipe
     */
    fun detectLivestreamFrame(imageProxy: ImageProxy) {
        // Check if detector is initialized
        if (!isInitialized.get() || isInitializing.get()) {
            imageProxy.close()
            return
        }

        val frameTime = SystemClock.uptimeMillis()

        // FPS limiting mechanism (implemented as in the original MediaPipe sample)
        frameProcessedInOneSecond++
        if (frameTime - lastFpsTimestamp >= 1000) {
            currentFps = frameProcessedInOneSecond
            frameProcessedInOneSecond = 0
            lastFpsTimestamp = frameTime
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "FPS: $currentFps")
            }
        }

        // Skip frames if processing is too frequent (dynamic frame skipping)
        // Use a time-based approach so performance scales with device capabilities
        val frameDelta = frameTime - lastProcessingTimeMs
        if (frameDelta < MIN_FRAME_SPACING_MS) {
            // Skip this frame to maintain consistent frame rate
            imageProxy.close()
            return
        }

        // Update input image rotation for MediaPipe processing without reinitializing the detector
        val currentRotation = imageProxy.imageInfo.rotationDegrees
        imageRotation = currentRotation
        imageProcessingOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(imageRotation)
            .build()

        lastProcessingTimeMs = frameTime

        var bitmap: Bitmap? = null
        try {
            // Convert ImageProxy to Bitmap properly
            val bmp = imageProxyToBitmap(imageProxy)
                ?: run {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Invalid bitmap from ImageProxy conversion")
                    }
                    imageProxy.close()
                    return
                }
            bitmap = bmp

            // Do not pre-rotate the bitmap; pass rotation via ImageProcessingOptions to MediaPipe

            // Final validation before passing to MediaPipe
            if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Invalid bitmap, skipping frame")
                }
                imageProxy.close()
                return
            }

            // Create MPImage - MediaPipe creates its own copy for async operations
            val mpImage = try {
                BitmapImageBuilder(bmp).build()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error creating MPImage: ${e.message}", e)
                }
                imageProxy.close()
                return
            }

            // Close imageProxy immediately - MediaPipe has its copy
            imageProxy.close()

            // Detect asynchronously with correct rotation options
            detectAsync(mpImage, frameTime)

            // Note: We don't recycle the bitmap here because MediaPipe's async operations
            // may still need access to the underlying data. MediaPipe will handle cleanup.
            // However, if we need to manage memory more aggressively, we could recycle
            // after a delay, but this is risky with async operations.

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error processing frame: ${e.message}", e)
            }
            // Cleanup on error
            bitmap?.let {
                if (!it.isRecycled) it.recycle()
            }
            try {
                imageProxy.close()
            } catch (closeError: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error closing ImageProxy: ${closeError.message}")
                }
            }
        }
    }

    /**
     * Converts ImageProxy to Bitmap, handling RGBA format from CameraX
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val width = image.width
            val height = image.height

            if (width <= 0 || height <= 0) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Invalid image dimensions: ${width}x${height}")
                }
                return null
            }

            val planes = image.planes
            if (planes.isEmpty()) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Image has no planes")
                }
                return null
            }

            val plane0 = planes[0]
            val pixelStride = plane0.pixelStride
            val rowStride = plane0.rowStride

            // Fast path: RGBA_8888 (pixelStride = 4) in planes[0]
            if (pixelStride == 4 && rowStride >= width * pixelStride) {
                val expectedRowStride = width * pixelStride
                val rowPadding = rowStride - expectedRowStride

                val bitmap = try {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                } catch (e: OutOfMemoryError) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Out of memory creating bitmap: ${e.message}")
                    }
                    // Don't call System.gc() - let Android handle memory management
                    return null
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Error creating bitmap: ${e.message}", e)
                    }
                    return null
                }

                val buffer = plane0.buffer
                buffer.rewind()
                if (rowPadding == 0) {
                    bitmap.copyPixelsFromBuffer(buffer)
                } else {
                    val rowBytes = width * pixelStride
                    val tempRow = ByteArray(rowBytes)
                    var y = 0
                    while (y < height && buffer.remaining() >= rowBytes) {
                        buffer.get(tempRow, 0, rowBytes)
                        // Convert row bytes (RGBA) to int pixels
                        val pixels = IntArray(width)
                        var x = 0
                        var idx = 0
                        while (x < width) {
                            val r = tempRow[idx].toInt() and 0xFF
                            val g = tempRow[idx + 1].toInt() and 0xFF
                            val b = tempRow[idx + 2].toInt() and 0xFF
                            val a = tempRow[idx + 3].toInt() and 0xFF
                            pixels[x] = (a shl 24) or (r shl 16) or (g shl 8) or b
                            x++
                            idx += 4
                        }
                        bitmap.setPixels(pixels, 0, width, 0, y, width, 1)
                        // Skip padding
                        val toSkip = rowPadding.coerceAtMost(buffer.remaining())
                        if (toSkip > 0 && y < height - 1) {
                            buffer.position(buffer.position() + toSkip)
                        }
                        y++
                    }
                }
                return bitmap
            }

            // Fallback path: YUV_420_888 -> ARGB conversion for devices that don't deliver RGBA
            if (planes.size >= 3) {
                return yuv420ToBitmap(image)
            }

            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Unsupported image format: pixelStride=$pixelStride, rowStride=$rowStride, planes=${planes.size}")
            }
            null
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error converting ImageProxy to Bitmap: ${e.message}", e)
            }
            null
        }
    }

    private fun yuv420ToBitmap(image: ImageProxy): Bitmap? {
        val width = image.width
        val height = image.height
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V
        val yRowStride = image.planes[0].rowStride
        val uvRowStride = image.planes[1].rowStride
        val uvPixelStride = image.planes[1].pixelStride

        val out = IntArray(width * height)

        val yData = ByteArray(yBuffer.remaining())
        yBuffer.get(yData)
        val uData = ByteArray(uBuffer.remaining())
        uBuffer.get(uData)
        val vData = ByteArray(vBuffer.remaining())
        vBuffer.get(vData)

        var yp = 0
        for (j in 0 until height) {
            val pY = j * yRowStride
            val uvRow = (j shr 1) * uvRowStride
            for (i in 0 until width) {
                val uvCol = (i shr 1) * uvPixelStride
                val y = (yData[pY + i].toInt() and 0xFF)
                val u = (uData[uvRow + uvCol].toInt() and 0xFF) - 128
                val v = (vData[uvRow + uvCol].toInt() and 0xFF) - 128

                // YUV to RGB conversion
                var r = (y + 1.402f * v).toInt()
                var g = (y - 0.344136f * u - 0.714136f * v).toInt()
                var b = (y + 1.772f * u).toInt()

                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                out[yp++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return try {
            Bitmap.createBitmap(out, width, height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Out of memory creating bitmap from YUV: ${e.message}")
            }
            null
        }
    }

    /**
     * Checks if the ObjectDetector has been closed
     */
    fun isClosed(): Boolean {
        return !isInitialized.get() || objectDetector == null
    }

    /**
     * Checks if the ObjectDetector is ready to use
     */
    fun isReady(): Boolean {
        return isInitialized.get() && !isInitializing.get() && objectDetector != null
    }

    /**
     * Passes an image to the ObjectDetector for asynchronous processing
     */
    private fun detectAsync(mpImage: MPImage, frameTime: Long) {
        if (!isReady()) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Detector not ready, skipping frame")
            }
            return
        }

        detectorLock.withLock {
            try {
                objectDetector?.detectAsync(mpImage, imageProcessingOptions, frameTime)
            } catch (e: com.google.mediapipe.framework.MediaPipeException) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "MediaPipe error: ${e.message}")
                }
                // Detector might not be initialized yet, don't crash
                handleMediaPipeException(e)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error in detectAsync: ${e.message}", e)
                } else {
                    objectDetectorListener?.onError(
                        "Object detector error: ${e.message}",
                        OTHER_ERROR
                    )
                }
            }
        }
    }

    /**
     * Handles MediaPipe exceptions and reinitializes if needed
     */
    private fun handleMediaPipeException(e: com.google.mediapipe.framework.MediaPipeException) {
        val errorMessage = e.message ?: return
        val containsError = errorMessage.contains("task graph hasn't been successfully started")
        when {
            containsError -> {
                isInitialized.set(false)
                // Try to reinitialize
                setupObjectDetectorAsync()
            }
        }
    }

    /**
     * Callback for detection results in live stream mode
     */
    private fun returnLivestreamResult(
        result: ObjectDetectorResult,
        input: MPImage
    ) {
        try {
            val finishTimeMs = SystemClock.uptimeMillis()
            val inferenceTime = finishTimeMs - result.timestampMs()

            objectDetectorListener?.onResults(
                ResultBundle(
                    listOf(result),
                    inferenceTime,
                    input.height,
                    input.width,
                    imageRotation
                )
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error in returnLivestreamResult: ${e.message}", e)
            }
        }
    }

    /**
     * Callback for errors in live stream mode
     */
    private fun returnLivestreamError(error: RuntimeException) {
        objectDetectorListener?.onError(
            error.message ?: "An unknown error has occurred",
            OTHER_ERROR
        )
    }

    /**
     * Interface definition for callbacks related to the Object Detector.
     */
    interface DetectorListener {
        /**
         * Called when detection results are available.
         *
         * @param resultBundle Bundle containing detection results and metadata
         */
        fun onResults(resultBundle: ResultBundle)

        /**
         * Called when an error occurs during detection.
         *
         * @param error Description of the error
         * @param errorCode Error code indicating the type of error
         */
        fun onError(error: String, errorCode: Int)
    }

    /**
     * Data class that holds detection results and relevant metadata.
     *
     * @property results List of ObjectDetectorResults
     * @property inferenceTime Time taken for inference in milliseconds
     * @property inputImageHeight Height of the input image
     * @property inputImageWidth Width of the input image
     * @property inputImageRotation Rotation of the input image in degrees
     */
    data class ResultBundle(
        val results: List<ObjectDetectorResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
        val inputImageRotation: Int
    )

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val MAX_RESULTS_DEFAULT = 5
        const val THRESHOLD_DEFAULT = 0.5F
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
        private const val TAG = "ObjectDetectorHelper"
        private const val MIN_FRAME_SPACING_MS = 66L // Target ~15fps maximum for smoother performance
    }
}
