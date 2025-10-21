@file:OptIn(ExperimentalMaterial3Api::class)

package ai.pilahsampah.ui.screens

import android.content.res.Configuration
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
import ai.pilahsampah.ui.theme.Surface
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
private const val SHOW_PERFORMANCE_DEBUG = true

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
 * Function for performance testing
 */
//private val fpsList = mutableListOf<Int>()
//private val inferenceList = mutableListOf<Long>()
//
//fun perfTest(fps: Int, inference: Long){
//    // Add new values to the lists
//    fpsList.add(fps)
//    inferenceList.add(inference)
//
//    // Calculate averages
//    val fpsAverage = fpsList.average()
//    val inferenceAverage = inferenceList.average()
//
//    println("FPS count ${fpsList.size}")
//    println("Inference count ${inferenceList.size}")
//    println("FPS Average: {$fpsAverage}")
//    println("Inference Time Average: {$inferenceAverage}")
//}
//
//// Helper function to reset data if needed
//fun resetPerfData() {
//    fpsList.clear()
//    inferenceList.clear()
//}


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
    inferenceTime: Long = 0
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                // Add divider for visual separation
                Divider(
                    color = Color.LightGray,
                    thickness = 1.dp
                )
            }
        },
        containerColor = Color.White
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
            // Camera Preview with detection overlay - without rounded corners
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Camera Preview - configured to fill the space
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Detection overlay that matches camera preview
                DetectionOverlay(
                    detectionData = detectionData,
                    modifier = Modifier.fillMaxSize()
                )

//                perfTest(fps, inferenceTime)
                // Performance debug info - only shown if enabled
                if (SHOW_PERFORMANCE_DEBUG) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "FPS: $fps",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Inference: ${inferenceTime}ms",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        val detectionCount = detectionData.detections.size
                        Text(
                            text = "Detections: $detectionCount",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
        
        // Calculate aspect ratios
        val viewAspectRatio = size.width / size.height
        val imageAspectRatio = detectionData.imageWidth.toFloat() / detectionData.imageHeight.toFloat()
        
        // Calculate scaling factors to maintain aspect ratio (using FILL approach)
        var scaleFactor: Float
        var offsetX = 0.0f
        var offsetY = 0.0f
        var horizontalAdjustment = 0.0f // Adjustment factor for horizontal alignment
        
        // Match the FILL_START scale type for consistent alignment
        if (viewAspectRatio > imageAspectRatio) {
            // View is wider than image - scale up to match width
            scaleFactor = size.width / detectionData.imageWidth.toFloat()
            // Add a slight rightward adjustment to correct leftward bias
            horizontalAdjustment = 0.05f * size.width // 5% rightward adjustment 
            offsetX = horizontalAdjustment
            offsetY = 0.0f
        } else {
            // View is taller than image - scale up to match height
            scaleFactor = size.height / detectionData.imageHeight.toFloat()
            // Center horizontally with the adjustment factor
            offsetX = (size.width - (detectionData.imageWidth.toFloat() * scaleFactor)) / 2.0f + horizontalAdjustment
            offsetY = 0.0f
        }
        
        // Debug indicators - draw boundaries to visualize the coordinate system
        if (false) { // Set to true only for debugging
            // Draw a center crosshair 
            drawLine(
                color = Color.Red,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 2f
            )
            
            drawLine(
                color = Color.Red,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2f
            )
            
            // Draw camera frame outline
            drawRect(
                color = Color.Yellow,
                topLeft = Offset(offsetX, offsetY),
                size = androidx.compose.ui.geometry.Size(
                    detectionData.imageWidth * scaleFactor,
                    detectionData.imageHeight * scaleFactor
                ),
                style = Stroke(width = 2f)
            )
        }
        
        // Draw each detection
        detectionData.detections.forEach { detection ->
            val boundingBox = detection.boundingBox()
            val category = detection.categories()[0]
            val score = category.score()
            val label = category.categoryName().lowercase()
            
            // Map model coordinates to canvas coordinates with horizontal adjustment
            val left = boundingBox.left * scaleFactor + offsetX
            val top = boundingBox.top * scaleFactor + offsetY
            val right = boundingBox.right * scaleFactor + offsetX
            val bottom = boundingBox.bottom * scaleFactor + offsetY
            
            // Only draw if bounding box is in view
            if (bottom >= 0 && top <= size.height && right >= 0 && left <= size.width) {
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
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
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
                
                val textSize = textMeasurer.measure(labelAndScore, textStyle)
                val textWidth = textSize.size.width
                val textHeight = textSize.size.height
                
                // Only draw the label if it would be visible (not off-screen)
                if (top - textHeight - 12 >= 0) {
                    // Draw rounded label background with more curved corners (increased from 8f to 12f)
                    val labelRect = RoundRect(
                        left = left,
                        top = top - textHeight - 12,
                        right = left + textWidth + 12,
                        bottom = top - 4,
                        radiusX = 12f, // Increased from 8f to 12f for more curved corners
                        radiusY = 12f  // Increased from 8f to 12f for more curved corners
                    )
                    
                    drawPath(
                        path = Path().apply { addRoundRect(labelRect) },
                        color = boxColor,
                        alpha = 0.7f // Match the bounding box transparency (changed from 0.5f to 0.7f)
                    )
                    
                    // Draw label text
                    drawText(
                        textMeasurer = textMeasurer,
                        text = labelAndScore,
                        topLeft = Offset(left + 6, top - textHeight - 8),
                        style = textStyle
                    )
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
                    .size(48.dp)
                    .padding(bottom = 4.dp)
                    .clickable { showTooltip = !showTooltip },
                contentScale = ContentScale.Fit
            )
            
            // Small info icon to indicate it's clickable
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                    .background(color.copy(alpha = 0.8f), shape = RoundedCornerShape(9.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Show trash types",
                    modifier = Modifier
                        .size(14.dp)
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
        
        // Count text - with larger size
        Text(
            text = "$label: $count",
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

