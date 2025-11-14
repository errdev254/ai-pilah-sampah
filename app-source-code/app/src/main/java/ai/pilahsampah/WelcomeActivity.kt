package ai.pilahsampah

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ai.pilahsampah.ui.screens.WelcomeScreen
import ai.pilahsampah.ui.theme.TrashAppTheme
import androidx.activity.SystemBarStyle

class WelcomeActivity : ComponentActivity() {
    private var isLoadingState = mutableStateOf(false)

    @androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
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
        setContent {
            TrashAppTheme {
                WelcomeScreen(
                    onStartDetectionClick = {
                        // Set loading state to true
                        isLoadingState.value = true
                        
                        // Start detection activity with small delay to show loading
                        lifecycleScope.launch {
                            delay(100) // Brief delay to show loading state
                            val intent = Intent(this@WelcomeActivity, ObjectDetectionActivity::class.java)
                            startActivity(intent)
                        }
                    },
                    onEducationClick = {
                        startActivity(Intent(this@WelcomeActivity, EducationActivity::class.java))
                    },
                    onAboutAppClick = {
                        startActivity(Intent(this@WelcomeActivity, InformationActivity::class.java))
                    },
                    isLoading = isLoadingState.value
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reset loading state when returning to this activity
        isLoadingState.value = false
    }
} 