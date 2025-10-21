package ai.pilahsampah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.pilahsampah.ui.screens.EducationScreen
import ai.pilahsampah.ui.theme.TrashAppTheme

class EducationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrashAppTheme {
                EducationScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
} 