package ai.pilahsampah

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import kotlin.math.max
import kotlin.math.min

// Use color constants directly instead of importing from Compose
private val ORGANIK_COLOR = Color.rgb(100, 225, 0)  // #64E100
private val ANORGANIK_COLOR = Color.rgb(233, 233, 0)  // #E9E900
private val B3_COLOR = Color.rgb(255, 26, 0)  // #FF1A00

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: ObjectDetectorResult? = null
    private var boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        alpha = 180
    }
    private var textBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 180
    }
    private var textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
    }
    private var bounds = Rect()
    private var runningMode: RunningMode = RunningMode.IMAGE
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var imageRotation: Int = 0
    private var needUpdateTransformation = false
    private var scaleFactor: Float = 1f
    private var postScaleWidthOffset: Float = 0f
    private var postScaleHeightOffset: Float = 0f

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        
        if (needUpdateTransformation) {
            updateTransformationIfNeeded()
        }
        
        results?.let {
            visualizeDetectionResults(
                canvas = canvas,
                detectorResults = it
            )
        }
    }
    
    private fun updateTransformationIfNeeded() {
        val width = width.toFloat()
        val height = height.toFloat()
        
        var postScaleFactor = 1.0f
        
        if (width * imageHeight > height * imageWidth) {
            // The view is wider than the image
            postScaleFactor = width / imageWidth
            postScaleWidthOffset = 0f
            postScaleHeightOffset = (height - imageHeight * postScaleFactor) / 2
        } else {
            // The image is wider than the view
            postScaleFactor = height / imageHeight
            postScaleHeightOffset = 0f
            postScaleWidthOffset = (width - imageWidth * postScaleFactor) / 2
        }
        
        scaleFactor = postScaleFactor
        needUpdateTransformation = false
    }
    
    private fun visualizeDetectionResults(
        canvas: Canvas,
        detectorResults: ObjectDetectorResult
    ) {
        detectorResults.detections().forEach { detection ->
            val boundingBox = detection.boundingBox()
            val category = detection.categories()[0]
            val score = category.score()
            val label = category.categoryName()

            // Maps the bounding box coordinates from the model to the coordinates on screen
            val left = boundingBox.left * scaleFactor + postScaleWidthOffset
            val top = boundingBox.top * scaleFactor + postScaleHeightOffset
            val right = boundingBox.right * scaleFactor + postScaleWidthOffset
            val bottom = boundingBox.bottom * scaleFactor + postScaleHeightOffset

            // Set box color based on category
            when (label.lowercase()) {
                in ORGANIK_LABELS -> {
                    boxPaint.color = ORGANIK_COLOR
                    textBackgroundPaint.color = ORGANIK_COLOR
                }
                in ANORGANIK_LABELS -> {
                    boxPaint.color = ANORGANIK_COLOR
                    textBackgroundPaint.color = ANORGANIK_COLOR
                }
                in B3_LABELS -> {
                    boxPaint.color = B3_COLOR
                    textBackgroundPaint.color = B3_COLOR
                }
                else -> {
                    boxPaint.color = Color.WHITE
                    textBackgroundPaint.color = Color.WHITE
                }
            }

            // Draw bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Draw label and score
            val labelAndScore = "$label (${String.format("%.2f", score)})"
            textPaint.getTextBounds(labelAndScore, 0, labelAndScore.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            // Position the text above the bounding box
            val textLeft = left
            val textTop = max(0f, top - textHeight - 8f)

            canvas.drawRect(
                textLeft,
                textTop,
                textLeft + textWidth + 8f,
                textTop + textHeight + 8f,
                textBackgroundPaint
            )
            canvas.drawText(labelAndScore, textLeft + 4f, textTop + textHeight + 4f, textPaint)
        }
    }

    fun setRunningMode(mode: RunningMode) {
        runningMode = mode
    }

    fun setResults(
        detectorResults: ObjectDetectorResult,
        imageHeight: Int,
        imageWidth: Int,
        imageRotation: Int = 0
    ) {
        results = detectorResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        this.imageRotation = imageRotation
        needUpdateTransformation = true
        invalidate()
    }

    companion object {
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