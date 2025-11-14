@file:OptIn(ExperimentalMaterial3Api::class)

package ai.pilahsampah.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.pilahsampah.R
import ai.pilahsampah.ui.theme.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush

private data class DialogContent(
    val title: String,
    val color: Color,
    val binImage: Int?,
    val definition: String,
    val disposal: String?,
    val management: List<String>
)

private enum class DialogType {
    ORGANIK, ANORGANIK, B3, MANFAAT, DAMPAK
}

@Composable
fun EducationScreen(
    onBackClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf<DialogType?>(null) }

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

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = "Edukasi Pemilahan Sampah",
                onBackClick = onBackClick
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = paddingHorizontal, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Waste Category Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(
                    title = "Sampah\nOrganik",
                    color = OrganikColor,
                    modifier = Modifier.weight(1f),
                    onClick = { showDialog = DialogType.ORGANIK }
                )
                CategoryCard(
                    title = "Sampah\nAnorganik",
                    color = AnorganikColor,
                    modifier = Modifier.weight(1f),
                    onClick = { showDialog = DialogType.ANORGANIK }
                )
                CategoryCard(
                    title = "Sampah\nB3",
                    color = B3Color,
                    modifier = Modifier.weight(1f),
                    onClick = { showDialog = DialogType.B3 }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info Buttons
            InfoButton(
                text = "Manfaat Pemilahan Sampah",
                onClick = { showDialog = DialogType.MANFAAT }
            )

            Spacer(modifier = Modifier.height(16.dp))

            InfoButton(
                text = "Dampak Tidak Memilah Sampah",
                onClick = { showDialog = DialogType.DAMPAK }
            )
        }
    }

    // Show dialog based on state
    showDialog?.let { type ->
        EducationDialog(
            type = type,
            onDismiss = { showDialog = null }
        )
    }
}

@Composable
private fun TopAppBar(title: String, onBackClick: () -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .shadow(4.dp)
                .background(Color.White)
        ) {
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
            Text(
                text = title,
                color = Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCard(
   title: String,
   color: Color,
   modifier: Modifier = Modifier,
   onClick: () -> Unit
) {
   val configuration = LocalConfiguration.current
   val screenWidth = configuration.screenWidthDp
   val bucket = when {
       screenWidth < 360 -> "S"
       screenWidth < 600 -> "M"
       else -> "L"
   }
   val binIconSize = when (bucket) {
       "S" -> 28.dp
       "M" -> 32.dp
       else -> 40.dp
   }
    // Select bin icon based on color
    val binIcon = when (color) {
        OrganikColor -> R.drawable.green_bin
        AnorganikColor -> R.drawable.yellow_bin
        B3Color -> R.drawable.red_bin
        else -> R.drawable.green_bin
    }
    // Use a slightly transparent Primary color for the card background

    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .border(
                width = 3.dp,
                color = color,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.80f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = binIcon),
                contentDescription = null,
                modifier = Modifier.size(binIconSize)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun InfoButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun EducationDialog(type: DialogType, onDismiss: () -> Unit) {
    val content = when (type) {
        DialogType.ORGANIK -> DialogContent(
            title = "Sampah Organik",
            color = OrganikColor,
            binImage = R.drawable.green_bin,
            definition = "Sampah yang berasal dari bahan-bahan hayati yang mudah terurai secara alami.",
            disposal = "Buang sampah pada tempat sampah dengan warna hijau.",
            management = listOf("Pupuk kompos", "Pakan ternak", "Biogas")
        )
        DialogType.ANORGANIK -> DialogContent(
            title = "Sampah Anorganik",
            color = AnorganikColor,
            binImage = R.drawable.yellow_bin,
            definition = "Sampah yang berasal dari material hasil proses manusia atau proses industrial. Sampah jenis ini bersifat sulit untuk terurai secara alami.",
            disposal = "Buang sampah pada tempat sampah dengan warna kuning.",
            management = listOf("Daur ulang (Recycle)", "Digunakan ulang (Reuse)", "Kerajinan tangan")
        )
        DialogType.B3 -> DialogContent(
            title = "Sampah B3 (Bahan Berbahaya & Beracun)",
            color = B3Color,
            binImage = R.drawable.red_bin,
            definition = "Sampah yang mengandung zat atau bahan berbahaya dan beracun yang dapat membahayakan kesehatan serta lingkungan.",
            disposal = "Buang sampah pada tempat sampah dengan warna merah.",
            management = listOf("Diserahkan ke fasilitas penanganan B3", "Tidak dicampur dengan sampah organik dan anorganik")
        )
        DialogType.MANFAAT -> DialogContent(
            title = "Manfaat Pemilahan Sampah",
            color = Primary,
            binImage = null,
            definition = "Memilah sampah memiliki banyak manfaat penting antara lain:",
            disposal = null,
            management = listOf("Mengurangi pencemaran lingkungan (tanah, air, udara).", "Menghemat sumber daya alam melalui daur ulang.", "Memiliki nilai ekonomis dari sampah yang bisa dijual.", "Menjaga kesehatan masyarakat dan kebersihan lingkungan.")
        )
        DialogType.DAMPAK -> DialogContent(
            title = "Dampak Tidak Memilah Sampah",
            color = Primary,
            binImage = null,
            definition = "Tidak memilah sampah dapat menimbulkan dampak negatif yang serius antara lain:",
            disposal = null,
            management = listOf("Menyebabkan pencemaran lingkungan yang merusak ekosistem.", "Menjadi sumber penyebaran penyakit.", "Mengurangi usia pakai Tempat Pembuangan Akhir (TPA).", "Menghilangkan potensi ekonomis dari sampah daur ulang.")
        )
    }

    val dialogAccentColor = if (type in listOf(DialogType.ORGANIK, DialogType.ANORGANIK, DialogType.B3)) Primary else content.color

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = dialogAccentColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Divider(color = dialogAccentColor.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 12.dp))

                // Definition
                Text(
                    text = content.definition,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Disposal Info
                if (content.disposal != null && content.binImage != null) {
                    DialogSection(title = "Tempat Membuang Sampah") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = content.binImage),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(end = 16.dp)
                            )
                            Text(text = content.disposal, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Management Info
                DialogSection(title = if (type in listOf(DialogType.ORGANIK, DialogType.ANORGANIK, DialogType.B3)) "Penanganan Sampah" else "") {
                    Column {
                        content.management.forEachIndexed { index, text ->
                            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(
                                    text = "${index + 1}. ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = dialogAccentColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DialogSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    if (title.isNotBlank()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 8.dp, end = 8.dp),
        content = content
    )
}


