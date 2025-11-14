package ai.pilahsampah

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ai.pilahsampah.ui.screens.AboutScreen
import ai.pilahsampah.ui.theme.TrashAppTheme
import androidx.activity.SystemBarStyle

class InformationActivity : ComponentActivity() {

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
                AboutScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
} 