package com.letify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.letify.app.ui.AnketnicaApp
import com.letify.app.ui.icons.SolarIconLoader
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.rememberAnketnicaState
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Prewarm the Solar icon bitmaps on a worker thread so the first
        // frame paints with cached glyphs. Missing asset names simply fail
        // to decode (runCatching) and are skipped — safe.
        SolarIconLoader.prewarmAll(applicationContext)
        SolarIconLoader.awaitNavbarReady()
        setContent {
            val state = rememberAnketnicaState()
            CompositionLocalProvider(LocalAppState provides state) {
                LetifyTheme(mode = state.themeMode, accent = state.accent) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
                            AnketnicaApp(state)
                        }
                    }
                }
            }
        }
    }
}