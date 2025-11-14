@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package ai.pilahsampah.ui.screens

import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import ai.pilahsampah.BuildConfig
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import ai.pilahsampah.R
import ai.pilahsampah.ui.theme.Primary
import ai.pilahsampah.ui.theme.OrganikColor
import ai.pilahsampah.ui.theme.AnorganikColor
import ai.pilahsampah.ui.theme.B3Color
import com.google.mediapipe.tasks.components.containers.Detection

/**
 * Data class representing detection results for the UI
 */
data class DetectionData(
    val detections: List<Detection> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val rotation: Int = 0
)

/**
 * Data class representing detection counts by category
 */
data class DetectionCounts(
    val organik: Int = 0,
    val anorganik: Int = 0,
    val b3: Int = 0
)

// Flag to enable performance debugging information
// Performance overlay is now always visible to match the app design

// Maps of trash types for tooltips
private val organikTrashTypes = listOf(
    "Cangkang Pala", "Daun Kering", "Daun Segar", "Kantung Teh", "Kayu", "Kulit Alpukat", "Kulit Bawang Putih", "Kulit Buah Cokelat", "Kulit Buah Naga", "Kulit Kacang", "Kulit Lemon", "Kulit Nanas", "Kulit Pisang", "Kulit Salak", "Kulit Semangka", "Kulit Telur", "Sabut Kelapa", "Tempurung Kelapa", "Tongkol Jagung", "Tulang Ikan"
)
private val anorganikTrashTypes = listOf(
    "Botol", "Bubblewrap", "Busa", "Gelas Plastik", "Garpu", "Kantung Plastik", "Kaleng", "Kardus", "Kertas", "Kemasan Plastik", "Mika Plastik", "Pipa", "Pulpen", "Sandal", "Sepatu", "Sendok", "Sisir", "Styrofoam", "Thinwall", "Seng"
)
private val b3TrashTypes = listOf(
    "Aerosol", "Baterai", "Botol Infus", "Kemasan Salep", "Lampu", "Masker", "Obat-obatan Strip", "Ponsel", "Suntik", "Termometer"
)

// Constants for label categories
private val ORGANIK_LABELS = setOf(
    "cangkang_pala", "daun_kering", "daun_segar", "kantung_teh", "kayu", "kulit_alpukat", "kulit_bawang_putih", "kulit_buah_cokelat", "kulit_buah_naga", "kulit_kacang", "kulit_lemon", "kulit_nanas", "kulit_pisang", "kulit_salak", "kulit_semangka", "kulit_telur", "sabut_kelapa", "tempurung_kelapa", "tongkol_jagung", "tulang_ikan"
)
private val ANORGANIK_LABELS = setOf(
    "botol", "bubblewrap", "busa", "gelas_plastik", "garpu", "kantung_plastik", "kaleng", "kardus", "kertas", "kemasan_plastik", "mika_plastik", "pipa", "pulpen", "sandal", "sepatu", "sendok", "sisir", "styrofoam", "thinwall", "seng"
)
private val B3_LABELS = setOf(
    "aerosol", "baterai", "botol_infus", "kemasan_salep", "lampu", "masker", "obat_obatan_strip", "ponsel", "suntik", "termometer"
)

/**
 * Main detection screen composable that displays camera preview with detection overlays
 * and category counters
 */
@Composable
fun DetectionScreen(
    previewView: PreviewView,
    detectionData: DetectionData,
    detectionCounts: DetectionCounts,
    onBackClick: () -> Unit,
    fps: Int = 0,
    inferenceTime: Long = 0,
    delegate: Int = 0,
    onDelegateChange: (Int) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val bucket = when {
        screenWidth < 360 -> "S"
        screenWidth < 600 -> "M"
        else -> "L"
    }
    val topBarHeight = when (bucket) {
        "S" -> 48.dp
        "M" -> 56.dp
        else -> 64.dp
    }
    val titleSize = when (bucket) {
        "S" -> 16.sp
        "M" -> 18.sp
        else -> 20.sp
    }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = { 
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(topBarHeight)
                        .shadow(4.dp)
                        .background(Color.White)
                ) {
                    // Back button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Primary
                        )
                    }
                    
                    // Centered title
                    Text(
                        text = "Pindai Sampah",
                        color = Primary,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp,
                    start = 0.dp,
                    end = 0.dp
                )
        ) {
            // Camera Preview with detection overlay
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Camera Preview
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Detection overlay
                DetectionOverlay(
                    detectionData = detectionData,
                    modifier = Modifier.fillMaxSize()
                )

                // CPU/GPU Toggle - transparent, responsive chip
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable { 
                            val newDelegate = if (delegate == 0) 1 else 0
                            onDelegateChange(newDelegate)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Icon to indicate current delegate
                        val iconColor = Color.White
                        if (delegate == 0) {
                            // CPU icon representation
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(iconColor.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                            )
                        } else {
                            // GPU icon representation
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 3.dp, height = 12.dp)
                                            .background(iconColor.copy(alpha = 0.9f), RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (delegate == 0) "CPU" else "GPU",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Performance stats overlay - always visible, responsive design
                val performanceFontSize = when (bucket) {
                    "S" -> 12.sp
                    "M" -> 13.sp
                    else -> 14.sp
                }
                val performancePadding = when (bucket) {
                    "S" -> 8.dp
                    "M" -> 10.dp
                    else -> 12.dp
                }
                
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(performancePadding)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "FPS: $fps",
                        color = Color.White,
                        fontSize = performanceFontSize,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Inference: ${inferenceTime}ms",
                        color = Color.White,
                        fontSize = performanceFontSize,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val detectionCount = detectionData.detections.size
                    Text(
                        text = "Detections: $detectionCount",
                        color = Color.White,
                        fontSize = performanceFontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Position the counters panel based on orientation
            if (!isLandscape) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(IntrinsicSize.Min)
                ) {
                    CountersPanel(
                        counts = detectionCounts,
                        modifier = Modifier.fillMaxWidth(),
                        isLandscape = false
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .width(IntrinsicSize.Min)
                ) {
                    CountersPanel(
                        counts = detectionCounts,
                        modifier = Modifier.fillMaxHeight(),
                        isLandscape = true
                    )
                }
            }
        }
    }
}

/**
 * Canvas-based overlay that draws bounding boxes and labels for detected objects
 */
@Composable
fun DetectionOverlay(
    detectionData: DetectionData,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier) {
        if (detectionData.imageWidth <= 0 || detectionData.imageHeight <= 0) return@Canvas
        
        // Match PreviewView.ScaleType.FILL_START scaling exactly
        // FILL_START: Scale the image uniformly so that both dimensions will be equal to or larger than the view
        // IMPORTANT: MediaPipe applies rotation internally based on ImageProcessingOptions.
        // The returned bounding boxes are already in the rotated-upright coordinate space.
        // Therefore, we should NOT rotate the boxes again here. We only need to
        // map from the effective (possibly rotated) image size to the canvas size.

        // Compute the rotated source dimensions based on image rotation
        val rotatedWidthHeight = when (detectionData.rotation % 360) {
            0, 180 -> Pair(detectionData.imageWidth.toFloat(), detectionData.imageHeight.toFloat())
            90, 270 -> Pair(detectionData.imageHeight.toFloat(), detectionData.imageWidth.toFloat())
            else -> Pair(detectionData.imageWidth.toFloat(), detectionData.imageHeight.toFloat())
        }

        // For live stream with PreviewView.ScaleType.FILL_START, use max scaling (like MediaPipe sample)
        val scaleFactor = kotlin.math.max(
            if (rotatedWidthHeight.first > 0f) size.width / rotatedWidthHeight.first else 1f,
            if (rotatedWidthHeight.second > 0f) size.height / rotatedWidthHeight.second else 1f
        )

        // FILL_START aligns to top-start, so no offsets
        val offsetX = 0f
        val offsetY = 0f

        // Draw each detection
        detectionData.detections.forEach { detection ->
            val bbox = detection.boundingBox()

            val category = detection.categories()[0]
            val score = category.score()
            val label = category.categoryName().lowercase()

            // Follow MediaPipe sample: rotate the bbox by the output rotation around image center
            val outputWidth = detectionData.imageWidth.toFloat()
            val outputHeight = detectionData.imageHeight.toFloat()
            val boxRect = RectF(bbox.left, bbox.top, bbox.right, bbox.bottom)
            val matrix = Matrix().apply {
                // Translate to image center
                postTranslate(-outputWidth / 2f, -outputHeight / 2f)
                // Rotate by the image rotation
                postRotate(detectionData.rotation.toFloat())
                // Translate back accounting for 90/270 swap
                if (detectionData.rotation == 90 || detectionData.rotation == 270) {
                    postTranslate(outputHeight / 2f, outputWidth / 2f)
                } else {
                    postTranslate(outputWidth / 2f, outputHeight / 2f)
                }
            }
            matrix.mapRect(boxRect)

            // Scale to canvas coordinates (FILL_START -> top-start; no offsets)
            val finalLeft = boxRect.left * scaleFactor + offsetX
            val finalTop = boxRect.top * scaleFactor + offsetY
            val finalRight = boxRect.right * scaleFactor + offsetX
            val finalBottom = boxRect.bottom * scaleFactor + offsetY
            
            // Only draw if bounding box is in view
            if (finalBottom >= 0 && finalTop <= size.height && finalRight >= 0 && finalLeft <= size.width) {
                // Set box color based on category
                val boxColor = when {
                    label in ORGANIK_LABELS -> OrganikColor
                    label in ANORGANIK_LABELS -> AnorganikColor
                    label in B3_LABELS -> B3Color
                    else -> Color.White
                }
                
                // Draw rounded rectangle bounding box with curved corners
                val cornerRadius = 20f
                val rect = RoundRect(
                    left = finalLeft,
                    top = finalTop,
                    right = finalRight,
                    bottom = finalBottom,
                    radiusX = cornerRadius,
                    radiusY = cornerRadius
                )
                
                drawPath(
                    path = Path().apply { addRoundRect(rect) },
                    color = boxColor,
                    style = Stroke(width = 8f),
                    alpha = 0.7f
                )
                
                // Draw label and score
                val labelAndScore = "$label (${String.format("%.2f", score)})"
                val textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // Measure text to get accurate width for label background
                val maxAvailableWidth = size.width.coerceAtLeast(1f)
                val maxTextHeight = size.height.coerceAtLeast(1f)
                
                try {
                    val textLayoutResult = textMeasurer.measure(
                        text = labelAndScore,
                        style = textStyle,
                        constraints = androidx.compose.ui.unit.Constraints(
                            maxWidth = maxAvailableWidth.toInt().coerceAtLeast(1),
                            maxHeight = maxTextHeight.toInt().coerceAtLeast(1)
                        )
                    )
                    
                    val textWidth = textLayoutResult.size.width.toFloat()
                    val textHeight = textLayoutResult.size.height.toFloat()
                    
                    if (textWidth > 0 && textHeight > 0 && finalTop - textHeight - 12 >= 0) {
                        val leftPadding = 6f
                        val rightPadding = 6f
                        val horizontalPadding = leftPadding + rightPadding
                        
                        val labelBackgroundWidth = textWidth + horizontalPadding
                        val labelLeft = finalLeft.coerceAtLeast(0f)
                        val labelRight = (labelLeft + labelBackgroundWidth).coerceAtMost(size.width)
                        
                        val labelRect = RoundRect(
                            left = labelLeft,
                            top = finalTop - textHeight - 12,
                            right = labelRight,
                            bottom = finalTop - 4,
                            radiusX = 12f,
                            radiusY = 12f
                        )
                        
                        drawPath(
                            path = Path().apply { addRoundRect(labelRect) },
                            color = boxColor,
                            alpha = 0.7f
                        )
                        
                        val textX = labelLeft + leftPadding
                        val textY = finalTop - textHeight - 8
                        
                        drawText(
                            textMeasurer = textMeasurer,
                            text = labelAndScore,
                            topLeft = Offset(textX, textY),
                            style = textStyle
                        )
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.w("DetectionOverlay", "Error drawing text: ${e.message}")
                }
            }
        }
    }
}

/**
 * Panel displaying detection counts for each trash category
 */
@Composable
fun CountersPanel(
    counts: DetectionCounts,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    if (isLandscape) {
        Column(
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(vertical = 20.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CounterItem(
                color = OrganikColor,
                label = "Organik",
                count = counts.organik,
                imageResId = R.drawable.green_bin,
                trashTypes = organikTrashTypes
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CounterItem(
                color = AnorganikColor,
                label = "Anorganik",
                count = counts.anorganik,
                imageResId = R.drawable.yellow_bin,
                trashTypes = anorganikTrashTypes
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CounterItem(
                color = B3Color,
                label = "B3",
                count = counts.b3,
                imageResId = R.drawable.red_bin,
                trashTypes = b3TrashTypes
            )
        }
    } else {
        Row(
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CounterItem(
                color = OrganikColor,
                label = "Organik",
                count = counts.organik,
                imageResId = R.drawable.green_bin,
                trashTypes = organikTrashTypes
            )
            
            CounterItem(
                color = AnorganikColor,
                label = "Anorganik",
                count = counts.anorganik,
                imageResId = R.drawable.yellow_bin,
                trashTypes = anorganikTrashTypes
            )
            
            CounterItem(
                color = B3Color,
                label = "B3",
                count = counts.b3,
                imageResId = R.drawable.red_bin,
                trashTypes = b3TrashTypes
            )
        }
    }
}

/**
 * Single counter item displaying a trash bin category icon and count with tooltip functionality
 */
@Composable
fun CounterItem(
    color: Color,
    label: String,
    count: Int,
    imageResId: Int,
    trashTypes: List<String>
) {
   val configuration = LocalConfiguration.current
   val screenWidth = configuration.screenWidthDp
   val bucket = when {
       screenWidth < 360 -> "S"
       screenWidth < 600 -> "M"
       else -> "L"
   }
   val binSize = when (bucket) {
       "S" -> 40.dp
       "M" -> 48.dp
       else -> 56.dp
   }
   val infoBgSize = when (bucket) {
       "S" -> 16.dp
       "M" -> 18.dp
       else -> 20.dp
   }
   val infoIconSize = when (bucket) {
       "S" -> 12.dp
       "M" -> 14.dp
       else -> 16.dp
   }
    val counterTextSize = when (bucket) {
        "S" -> 14.sp
        "M" -> 16.sp
        else -> 18.sp
    }
    var showTooltip by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        // Bin image with click handler
        Box {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = "$label Bin",
                modifier = Modifier
                    .size(binSize)
                    .padding(bottom = 4.dp)
                    .clickable { showTooltip = !showTooltip },
                contentScale = ContentScale.Fit
            )
            
            // Small info icon to indicate it's clickable
            Box(
                modifier = Modifier
                    .size(infoBgSize)
                    .align(Alignment.TopEnd)
                    .background(color.copy(alpha = 0.8f), shape = RoundedCornerShape(9.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Show trash types",
                    modifier = Modifier
                        .size(infoIconSize)
                        .align(Alignment.Center),
                    tint = Color.White
                )
            }
            
            // Tooltip popup
            if (showTooltip) {
                Popup(
                    onDismissRequest = { showTooltip = false },
                    alignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .widthIn(max = 200.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Jenis $label:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            trashTypes.forEach { trashType ->
                                Text(
                                    text = "• $trashType",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Count text
        Text(
            text = "$label: $count",
            color = color,
            fontSize = counterTextSize,
            fontWeight = FontWeight.Bold
        )
    }
}