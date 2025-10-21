package ai.pilahsampah.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define colors as per the design guide
val Primary = Color(0xFF2196F3)  // Main buttons, active states (Vibrant Blue)
val Secondary = Color(0xFF03A9F4)  // Highlights, accents (Light Blue)
val Accent = Color(0xFFFFC107)  // Important actions, warnings (Amber)
val Background = Color(0xFFF5F5F5)  // Light grayish background
val Surface = Color(0xFFFFFFFF)  // UI containers (cards, modals, etc.)
val TextPrimary = Color(0xFF212121)  // Main text (Dark gray for readability)
val TextSecondary = Color(0xFF757575)  // Subtitles, less prominent info
val Divider = Color(0xFFE0E0E0)  // UI dividers, separating elements

// Trash category specific colors
val OrganikColor = Color(0xFF64E100)  // Green for Organik: #64E100
val AnorganikColor = Color(0xFFE9E900)  // Yellow for Anorganik: #E9E900
val B3Color = Color(0xFFFF1A00)  // Red for B3 (Hazardous): #FF1A00

// Define the color scheme
private val TrashAppColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Accent,
    background = Surface, // Use White for background as per updated requirement
    surface = Surface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun TrashAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // This parameter is kept for API compatibility but not used
    content: @Composable () -> Unit
) {
    // We're only implementing light theme as per the requirements
    val colorScheme = TrashAppColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 