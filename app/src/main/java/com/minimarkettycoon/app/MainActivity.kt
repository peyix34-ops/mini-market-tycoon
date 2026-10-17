package com.minimarkettycoon.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Çökme yakalayıcı
        val crashFile = File(filesDir, "crash.txt")
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                crashFile.writeText(Log.getStackTraceString(e))
            } catch (_: Exception) {
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
        val lastCrash: String? =
            if (crashFile.exists()) crashFile.readText().also { crashFile.delete() } else null

        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            MaterialTheme {
                var crash by remember { mutableStateOf(lastCrash) }
                var inGame by remember { mutableStateOf(false) }
                val c = crash
                if (c != null) {
                    CrashScreen(c) { crash = null }
                } else if (inGame) {
                    BackHandler { inGame = false }
                    GameScreen()
                } else {
                    MainMenuScreen(onPlay = { inGame = true })
                }
            }
        }
    }
}

@Composable
private fun CrashScreen(text: String, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .padding(top = 24.dp)
    ) {
        Text("Oyun çöktü. Hata:", color = Color.Red, fontSize = 18.sp)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text.take(4000), color = Color.Black, fontSize = 11.sp)
        }
        Button(onClick = onClose) { Text("Kapat") }
    }
}
