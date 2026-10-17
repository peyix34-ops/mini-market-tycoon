package com.minimarkettycoon.app

import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.hypot

object GameHolder {
    var view: GLSurfaceView? = null
}

private class JoyState {
    var active by mutableStateOf(false)
    var origin by mutableStateOf(Offset.Zero)
    var thumb by mutableStateOf(Offset.Zero)
}

@Composable
fun GameScreen() {
    val input = remember { GameInput() }
    val joy = remember { JoyState() }
    val joyRadius = 130f

    DisposableEffect(Unit) {
        onDispose {
            GameHolder.view = null
            input.jx = 0f
            input.jy = 0f
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                    setRenderer(GameRenderer(input))
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    GameHolder.view = this
                }
            }
        )

        // Joystick görseli
        Canvas(Modifier.fillMaxSize()) {
            if (joy.active) {
                drawCircle(Color(0x33FFFFFF), joyRadius, joy.origin)
                drawCircle(Color(0x88FFFFFF), joyRadius, joy.origin, style = Stroke(4f))
                val d = joy.thumb - joy.origin
                val len = hypot(d.x, d.y)
                val c = if (len > joyRadius) joy.origin + d * (joyRadius / len) else joy.thumb
                drawCircle(Color(0xCCFFFFFF), 52f, c)
                drawCircle(Color(0x55000000), 52f, c, style = Stroke(3f))
            }
        }

        if (!joy.active) {
            Text(
                text = "Hareket etmek için ekrana dokun ve sürükle",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }

        // Dokunma katmanı (en üstte)
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        joy.origin = down.position
                        joy.thumb = down.position
                        joy.active = true
                        drag(down.id) { change ->
                            val d = change.position - joy.origin
                            val len = hypot(d.x, d.y)
                            if (len > joyRadius) {
                                joy.origin = joy.origin + d * ((len - joyRadius) / len)
                            }
                            joy.thumb = change.position
                            val dd = joy.thumb - joy.origin
                            val l = hypot(dd.x, dd.y)
                            if (l > 16f) {
                                val m = (l / joyRadius).coerceAtMost(1f)
                                input.jx = dd.x / l * m
                                input.jy = dd.y / l * m
                            } else {
                                input.jx = 0f
                                input.jy = 0f
                            }
                            change.consume()
                        }
                        joy.active = false
                        input.jx = 0f
                        input.jy = 0f
                    }
                }
        )
    }
}
