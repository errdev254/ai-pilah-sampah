@file:OptIn(ExperimentalMaterial3Api::class)

package ai.pilahsampah.ui.screens

import android.os.Build
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Stable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.pilahsampah.R
import ai.pilahsampah.ui.theme.Primary
import ai.pilahsampah.ui.theme.Surface
import ai.pilahsampah.ui.theme.OrganikColor
import ai.pilahsampah.ui.theme.AnorganikColor
import ai.pilahsampah.ui.theme.B3Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Info

@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    var showTujuanDialog by remember { mutableStateOf(false) }
    var showCaraPenggunaanDialog by remember { mutableStateOf(false) }
    var showKemampuanDeteksiDialog by remember { mutableStateOf(false) }

    // Responsive sizing
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val bucket = when {
        screenWidth < 360 -> "S"
        screenWidth < 600 -> "M"
        else -> "L"
    }
    val paddingHorizontal = when (bucket) {
        "S" -> 16.dp
        "M" -> 24.dp
        else -> 32.dp
    }
    val logoSize = when (bucket) {
        "S" -> 120.dp
        "M" -> 150.dp
        else -> 192.dp
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
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
                        text = "Tentang Aplikasi",
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
        containerColor = Surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = paddingHorizontal, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(logoSize)
                    .padding(bottom = 32.dp),
                contentScale = ContentScale.Fit
            )
            
            // Info buttons with elevation
            Button(
                onClick = { showTujuanDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Text(
                    text = "Tujuan Aplikasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { showCaraPenggunaanDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Text(
                    text = "Cara Penggunaan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { showKemampuanDeteksiDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Text(
                    text = "Kemampuan Deteksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // App version and developer info at the bottom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = "Version 1.0.2",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Dibuat Oleh:\nEroldy Rumayar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    // Dialogs
    if (showTujuanDialog) {
        TujuanAppDialog(onDismiss = { showTujuanDialog = false })
    }
    
    if (showCaraPenggunaanDialog) {
        CaraPenggunaanDialog(onDismiss = { showCaraPenggunaanDialog = false })
    }
    
    if (showKemampuanDeteksiDialog) {
        KemampuanDeteksiDialog(onDismiss = { showKemampuanDeteksiDialog = false })
    }
}

@Composable
fun TujuanAppDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Tujuan Aplikasi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Aplikasi ini bertujuan untuk membantu pengguna dalam mengidentifikasi dan memilah sampah berdasarkan jenisnya secara real-time menggunakan kamera smartphone, juga sebagai media edukasi mengenai pemilahan sampah.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
fun CaraPenggunaanDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Cara Penggunaan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Steps for usage instructions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    val steps = listOf(
                        "1. Tekan tombol \"Pindai Sampah\"",
                        "2. Izinkan akses kamera",
                        "3. Arahkan kamera ke objek sampah",
                        "4. Aplikasi akan mendeteksi sampah secara otomatis dan mengklasifikasi jenisnya menggunakan warna bingkai."
                    )
                    steps.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
fun KemampuanDeteksiDialog(onDismiss: () -> Unit) {
    var selectedTrashLabel by remember { mutableStateOf<String?>(null) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Kemampuan Deteksi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Announcement
                AnnouncementBox()

                Spacer(modifier = Modifier.height(24.dp))
                
                // Bounding box color explanation
                BoundingBoxExplanation()

                Divider(
                    color = Color.LightGray,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                // Detailed list of detectable items
                Text(
                    text = "Objek Sampah yang Dapat Dideteksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                TrashCategoryCard(
                    title = "Sampah Organik",
                    items = remember { listOf(
                        "Cangkang Pala", "Daun Kering", "Daun Segar", "Kantung Teh", "Kayu", "Kulit Alpukat", "Kulit Bawang Putih", "Kulit Buah Cokelat", "Kulit Buah Naga", "Kulit Kacang", "Kulit Lemon", "Kulit Nanas", "Kulit Pisang", "Kulit Salak", "Kulit Semangka", "Kulit Telur", "Sabut Kelapa", "Tempurung Kelapa", "Tongkol Jagung", "Tulang Ikan"
                    ) },
                    color = OrganikColor,
                    binImageId = R.drawable.green_bin,
                    onLabelClick = { selectedTrashLabel = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TrashCategoryCard(
                    title = "Sampah Anorganik",
                    items = remember { listOf(
                        "Botol", "Bubblewrap", "Busa", "Gelas Plastik", "Garpu", "Kantung Plastik", "Kaleng", "Kardus", "Kertas", "Kemasan Plastik", "Mika Plastik", "Pipa", "Pulpen", "Sandal", "Sepatu", "Sendok", "Sisir", "Styrofoam", "Thinwall", "Seng"
                    ) },
                    color = AnorganikColor,
                    binImageId = R.drawable.yellow_bin,
                    onLabelClick = { selectedTrashLabel = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TrashCategoryCard(
                    title = "Sampah B3 (Bahan Berbahaya & Beracun)",
                    items = remember { listOf(
                        "Aerosol", "Baterai", "Botol Infus", "Kemasan Salep", "Lampu", "Masker", "Obat-obatan Strip", "Ponsel", "Suntik", "Termometer"
                    ) },
                    color = B3Color,
                    binImageId = R.drawable.red_bin,
                    onLabelClick = { selectedTrashLabel = it }
                )
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
    if (selectedTrashLabel != null) {
        TrashLabelImageDialog(label = selectedTrashLabel!!, onDismiss = { selectedTrashLabel = null })
    }
}

@Composable
private fun BoundingBoxExplanation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Primary.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Text(
            text = "Cara Kerja Deteksi",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
            color = Primary
        )
        
        Text(
            text = "Aplikasi akan menampilkan kotak pembatas (bounding box) dengan warna sesuai kategori sampah yang terdeteksi:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Bounding box examples
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BoundingBoxExample(
                color = OrganikColor,
                text = "Organik"
            )
            
            BoundingBoxExample(
                color = AnorganikColor,
                text = "Anorganik"
            )
            
            BoundingBoxExample(
                color = B3Color,
                text = "B3"
            )
        }
    }
}

@Composable
private fun BoundingBoxExample(color: Color, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bounding box representation
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(width = 4.dp, color = color, shape = RoundedCornerShape(8.dp))
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun TrashCategoryCard(
    title: String,
    items: List<String>,
    color: Color,
    binImageId: Int,
    onLabelClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                // Small bin icon
                Image(
                    painter = painterResource(id = binImageId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 12.dp),
                    contentScale = ContentScale.Fit
                )
                // Category title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            // Numbered list of items (exactly like AboutScreenR.kt but without info icon)
            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                items.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Primary.copy(alpha = 0.08f))
                            .clickable { onLabelClick(item) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Removed Icon(Icons.Default.Info) from AboutScreenR.kt
                    }
                }
            }
        }
    }
}


 

@Composable
fun AnnouncementBox() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF3CD))
            .border(1.dp, Color(0xFFFFEEBA), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Perhatian!",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF856404),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Aplikasi ini optimal dalam mendeteksi sampah jika mengikuti ketentuan sebagai berikut:",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF856404),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val points = listOf(
            "Menggunakan objek sampah yang dilatihkan pada model",
            "Bidang latar belakang lantai keramik berwarna putih atau lantai cor beton",
            "Jarak kamera dan objek sampah 30-100 cm (tergantung ukuran dan jumlah objek)",
            "Pencahayaan gambar terang"
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            points.forEachIndexed { idx, point ->
                Row(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = "${idx + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF856404),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF856404)
                    )
                }
            }
        }
    }
}

@Composable
fun TrashLabelImageDialog(label: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                val imageName = "obj_" + label.lowercase()
                    .replace(" ", "_")
                    .replace("-", "_")
                    .replace(".", "")
                val context = LocalContext.current
                val imageId = remember(imageName) {
                    context.resources.getIdentifier(imageName, "drawable", context.packageName)
                }
                if (imageId != 0) {
                    Image(
                        painter = painterResource(id = imageId),
                        contentDescription = label,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "Gambar tidak ditemukan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
} 