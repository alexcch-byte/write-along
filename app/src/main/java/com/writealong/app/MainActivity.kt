package com.writealong.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.writealong.app.data.AppServices
import com.writealong.app.data.LocalServices
import com.writealong.app.data.ProgressStore
import com.writealong.app.data.Speaker
import com.writealong.app.ui.WriteAlongTheme

class MainActivity : ComponentActivity() {
    private lateinit var services: AppServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Full screen: more room to write, fewer accidental swipes out of the app.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val progress = ProgressStore(this)
        services = AppServices(progress, Speaker(this) { progress.voiceOn })

        setContent {
            WriteAlongTheme {
                CompositionLocalProvider(LocalServices provides services) {
                    WriteAlongApp()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        services.speaker.stop()
    }

    override fun onDestroy() {
        services.speaker.shutdown()
        super.onDestroy()
    }
}
