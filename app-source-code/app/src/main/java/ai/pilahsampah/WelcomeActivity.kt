package ai.pilahsampah

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import ai.pilahsampah.ui.screens.WelcomeScreen
import ai.pilahsampah.ui.theme.TrashAppTheme

class WelcomeActivity : ComponentActivity() {

    @androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrashAppTheme {
                val isLoading = remember { mutableStateOf(false) }
                
                WelcomeScreen(
                    onStartDetectionClick = {
                        // Set loading state to true first
                        isLoading.value = true
                        
                        // Add delay to ensure loading UI is visible long enough
                        // This is important because camera initialization takes time
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this, ObjectDetectionActivity::class.java)
                            startActivity(intent)
                            
                            // Reset loading state after the activity starts
                            Handler(Looper.getMainLooper()).postDelayed({
                                isLoading.value = false
                            }, 500) // Small delay to prevent flicker on return
                        }, 300) // Small delay to show loading state
                    },
                    onEducationClick = {
                        startActivity(Intent(this, EducationActivity::class.java))
                    },
                    onAboutAppClick = {
                        startActivity(Intent(this, InformationActivity::class.java))
                    },
                    isLoading = isLoading.value
                )
            }
        }
    }
} 