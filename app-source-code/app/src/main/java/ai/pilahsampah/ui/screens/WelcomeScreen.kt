@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.pilahsampah.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon

@Composable
fun WelcomeScreen(
    onStartDetectionClick: () -> Unit,
    onEducationClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    isLoading: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val bucket = when {
        screenWidth < 360 -> "S"
        screenWidth < 600 -> "M"
        else -> "L"
    }

    val horizontalPadding = when (bucket) {
        "S" -> 16.dp
        "M" -> 24.dp
        else -> 32.dp
    }
    val titleSize = when (bucket) {
        "S" -> 22.sp
        "M" -> 28.sp
        else -> 32.sp
    }
    val buttonHeight = when (bucket) {
        "S" -> 48.dp
        "M" -> 56.dp
        else -> 64.dp
    }
    val dialogIconSize = when (bucket) {
        "S" -> 32.dp
        "M" -> 40.dp
        else -> 48.dp
    }
    val dialogTextSize = when (bucket) {
        "S" -> 14.sp
        "M" -> 16.sp
        else -> 18.sp
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome illustration - positioned at the top
            Image(
                painter = painterResource(id = R.drawable.welcome_screen_ilustration),
                contentDescription = "Welcome Illustration",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 32.dp),
                contentScale = ContentScale.Fit
            )
            
            // App title
            Text(
                text = "AI Pilah Sampah",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = Primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = "AI Pilah Sampah hadir untuk membantu serta mengedukasi tentang pemilahan sampah menggunakan teknologi kecerdasan buatan.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Primary Button: Scan Trash (recode to show dialog)
            var showDialog by remember { mutableStateOf(false) }
            var dialogLoading by remember { mutableStateOf(false) }

            // Reset dialog state when screen is shown or when isLoading becomes false
            LaunchedEffect(isLoading) {
                if (!isLoading) {
                    showDialog = false
                    dialogLoading = false
                }
            }

            ElevatedButton(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                elevation = ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 4.dp
                ),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Pindai Sampah",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showDialog) {
                Dialog(
                    onDismissRequest = { if (!dialogLoading) showDialog = false },
                    properties = DialogProperties(dismissOnClickOutside = !dialogLoading, dismissOnBackPress = !dialogLoading)
                ) {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFFFA726), // A common amber/orange for warnings
                                modifier = Modifier.size(dialogIconSize)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Gunakan masker dan sarung tangan untuk memilah sampah!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            ElevatedButton(
                                onClick = {
                                    dialogLoading = true
                                    // Simulate loading, then call onStartDetectionClick
                                    onStartDetectionClick()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !dialogLoading,
                                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Primary,
                                    contentColor = Color.White
                                )
                            ) {
                                if (dialogLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Memuat...",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Sudah Digunakan",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { if (!dialogLoading) showDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !dialogLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Primary
                                )
                            ) {
                                Text(
                                    text = "Belum Digunakan",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Second Button: Education
            OutlinedButton(
                onClick = onEducationClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                )
            ) {
                Text(
                    text = "Edukasi Pemilahan Sampah",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Third Button: About App
            OutlinedButton(
                onClick = onAboutAppClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                )
            ) {
                Text(
                    text = "Tentang Aplikasi",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Full-screen loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
            )
        }
    }
}