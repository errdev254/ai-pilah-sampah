package ai.pilahsampah

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

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
    var currentDelegate: Int = DELEGATE_CPU,
    val objectDetectorListener: DetectorListener? = null
) {

    private var objectDetector: ObjectDetector? = null
    private var imageRotation = 0
    private var lastProcessingTimeMs = 0L
    private var frameProcessedInOneSecond = 0
    private var lastFpsTimestamp = 0L
    private var currentFps = 0
    
    init {
        setupObjectDetector()
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
        objectDetector?.close()
        objectDetector = null
    }

    /**
     * Sets up the ObjectDetector with the current configuration
     */
    fun setupObjectDetector() {
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
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details",
                GPU_ERROR
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    /**
     * Processes a frame from the camera for object detection
     * Handles rotation and conversion to the format required by MediaPipe
     */
    fun detectLivestreamFrame(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()
        
        // FPS limiting mechanism (implemented as in the original MediaPipe sample)
        frameProcessedInOneSecond++
        if (frameTime - lastFpsTimestamp >= 1000) {
            currentFps = frameProcessedInOneSecond
            frameProcessedInOneSecond = 0
            lastFpsTimestamp = frameTime
            Log.d(TAG, "FPS: $currentFps")
        }

        // Skip frames if processing is too frequent (dynamic frame skipping)
        // Use a time-based approach so performance scales with device capabilities
        val frameDelta = frameTime - lastProcessingTimeMs
        if (frameDelta < MIN_FRAME_SPACING_MS) {
            // Skip this frame to maintain consistent frame rate
            imageProxy.close()
            return
        }
        
        // If the input image rotation changes, reinitialize the detector
        if (imageProxy.imageInfo.rotationDegrees != imageRotation) {
            imageRotation = imageProxy.imageInfo.rotationDegrees
            clearObjectDetector()
            setupObjectDetector()
            imageProxy.close()
            return
        }
        
        lastProcessingTimeMs = frameTime
        
        // Create the bitmap from the image proxy
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )

        imageProxy.use { proxy ->
            val buffer = proxy.planes[0].buffer
            buffer.rewind()  // Rewind the buffer to ensure we read from the start
            bitmapBuffer.copyPixelsFromBuffer(buffer)
        }
        
        // Rotate the bitmap before analysis to match the expected orientation for the model
        // This ensures the model receives correctly oriented input regardless of device orientation
        val rotatedBitmap = when (imageRotation) {
            0 -> bitmapBuffer // No rotation needed
            90, 180, 270 -> {
                val matrix = Matrix().apply {
                    postRotate(imageRotation.toFloat())
                }
                val rotated = Bitmap.createBitmap(
                    bitmapBuffer,
                    0,
                    0,
                    bitmapBuffer.width,
                    bitmapBuffer.height,
                    matrix,
                    true
                )
                bitmapBuffer.recycle() // Clean up the original bitmap
                rotated
            }
            else -> bitmapBuffer // Default case, no rotation
        }

        // Pass the rotated bitmap to the detector
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        detectAsync(mpImage, frameTime)
        
        // Clean up bitmap
        rotatedBitmap.recycle()
    }

    /**
     * Checks if the ObjectDetector has been closed
     */
    fun isClosed(): Boolean {
        return objectDetector == null
    }

    /**
     * Passes an image to the ObjectDetector for asynchronous processing
     */
    private fun detectAsync(mpImage: MPImage, frameTime: Long) {
        objectDetector?.detectAsync(mpImage, frameTime)
    }

    /**
     * Callback for detection results in live stream mode
     */
    private fun returnLivestreamResult(
        result: ObjectDetectorResult,
        input: MPImage
    ) {
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