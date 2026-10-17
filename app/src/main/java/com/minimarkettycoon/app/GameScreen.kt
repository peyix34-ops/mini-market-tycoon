package com.minimarkettycoon.app

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import kotlin.math.hypot

object GameHolder {
    var view: GLSurfaceView? = null
}

/** Önce 24 bit derinlik tamponu dener, olmazsa 16 bit. */
class DepthConfigChooser : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        for (depth in intArrayOf(24, 16)) {
            val attribs = intArrayOf(
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, depth,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_NONE
            )
            val num = IntArray(1)
            egl.eglChooseConfig(display, attribs, null, 0, num)
            if (num[0] > 0) {
                val configs = arrayOfNulls<EGLConfig>(num[0])
                egl.eglChooseConfig(display, attribs, configs, num[0], num)
                val c = configs[0]
                if (c != null) return c
            }
        }
        throw IllegalStateException("Uygun EGL ayari bulunamadi")
    }
}

private class JoyState {
    var active by mutableStateOf(false)
    var thumb by mutableStateOf(Offset.Zero)
}

private const val DEF_HEIGHT = 24f
private const val DEF_BACK = 15f
private const val DEF_FOV = 45f

private fun loadCam(ctx: Context, cam: CameraConfig) {
    val p = ctx.getSharedPreferences("camera", Context.MODE_PRIVATE)
    cam.height = p.getFloat("height", DEF_HEIGHT)
    cam.back = p.getFloat("back", DEF_BACK)
    cam.fov = p.getFloat("fov", DEF_FOV)
}

private fun saveCam(ctx: Context, cam: CameraConfig) {
    ctx.getSharedPreferences("camera", Context.MODE_PRIVATE).edit()
        .putFloat("height", cam.height)
        .putFloat("back", cam.back)
        .putFloat("fov", cam.fov)
        .apply()
}

@Composable
fun GameScreen() {
    val ctx = LocalContext.current
    val input = remember { GameInput() }
    val cam = remember { CameraConfig().also { loadCam(ctx, it) } }
    val joy = remember { JoyState() }
    val joyRadius = with(LocalDensity.current) { 56.dp.toPx() }

    var viewSize by remember { mutableStateOf(IntSize(1080, 2340)) }
    var panelOpen by remember { mutableStateOf(false) }
    var sH by remember { mutableStateOf(cam.height) }
    var sB by remember { mutableStateOf(cam.back) }
    var sF by remember { mutableStateOf(cam.fov) }

    val origin = Offset(
        joyRadius * 1.4f + 30f,
        viewSize.height - joyRadius * 1.4f - 120f
    )

    DisposableEffect(Unit) {
        onDispose {
            GameHolder.view = null
            input.jx = 0f
            input.jy = 0f
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                GLSurfaceView(c).apply {
                    setEGLContextClientVersion(2)
                    setEGLConfigChooser(DepthConfigChooser())
                    setRenderer(GameRenderer(input, cam))
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    GameHolder.view = this
                }
            }
        )

        // Sabit joystick görseli
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0x33FFFFFF), joyRadius, origin)
            drawCircle(Color(0x88FFFFFF), joyRadius, origin, style = Stroke(4f))
            val d = joy.thumb - origin
            val len = hypot(d.x, d.y)
            val c = if (!joy.active) origin
            else if (len > joyRadius) origin + d * (joyRadius / len)
            else joy.thumb
            drawCircle(Color(0xCCFFFFFF), 52f, c)
            drawCircle(Color(0x55000000), 52f, c, style = Stroke(3f))
        }

        if (!panelOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(viewSize) {
                        fun apply(pos: Offset) {
                            val dd = pos - origin
                            val l = hypot(dd.x, dd.y)
                            if (l > 10f) {
                                val m = (l / joyRadius).coerceAtMost(1f)
                                input.jx = dd.x / l * m
                                input.jy = dd.y / l * m
                            } else {
                                input.jx = 0f
                                input.jy = 0f
                            }
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val d0 = down.position - origin
                            if (hypot(d0.x, d0.y) > joyRadius * 1.8f) {
                                return@awaitEachGesture
                            }
                            joy.active = true
                            joy.thumb = down.position
                            apply(down.position)
                            drag(down.id) { change ->
                                joy.thumb = change.position
                                apply(change.position)
                                change.consume()
                            }
                            joy.active = false
                            input.jx = 0f
                            input.jy = 0f
                        }
                    }
            )

            // Çark (ayarlar) butonu
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 36.dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable {
                        sH = cam.height
                        sB = cam.back
                        sF = cam.fov
                        panelOpen = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(26.dp)) {
                    val c = center
                    val r = size.minDimension / 2f
                    for (i in 0 until 8) {
                        rotate(i * 45f, pivot = c) {
                            drawRoundRect(
                                Color.White,
                                topLeft = Offset(c.x - r * 0.17f, c.y - r),
                                size = Size(r * 0.34f, r * 0.5f),
                                cornerRadius = CornerRadius(r * 0.06f)
                            )
                        }
                    }
                    val gear = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addOval(Rect(c, r * 0.74f))
                        addOval(Rect(c, r * 0.30f))
                    }
                    drawPath(gear, Color.White)
                }
            }
        }

        // Kamera ayarları paneli
        if (panelOpen) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Color(0xEE1B1F2A),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(20.dp)
            ) {
                Text(
                    "Kamera Ayarları",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Text("Yükseklik: ${sH.toInt()}", color = Color.White, fontSize = 14.sp)
                Slider(
                    value = sH,
                    onValueChange = { sH = it; cam.height = it },
                    valueRange = 10f..40f
                )
                Text("Uzaklık: ${sB.toInt()}", color = Color.White, fontSize = 14.sp)
                Slider(
                    value = sB,
                    onValueChange = { sB = it; cam.back = it },
                    valueRange = 2f..30f
                )
                Text("Görüş açısı: ${sF.toInt()}", color = Color.White, fontSize = 14.sp)
                Slider(
                    value = sF,
                    onValueChange = { sF = it; cam.fov = it },
                    valueRange = 30f..70f
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sH = DEF_HEIGHT; sB = DEF_BACK; sF = DEF_FOV
                            cam.height = DEF_HEIGHT; cam.back = DEF_BACK; cam.fov = DEF_FOV
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("Varsayılan", fontSize = 12.sp) }

                    OutlinedButton(
                        onClick = {
                            loadCam(ctx, cam)
                            panelOpen = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("İptal", fontSize = 12.sp) }

                    Button(
                        onClick = {
                            saveCam(ctx, cam)
                            panelOpen = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Kaydet", fontSize = 12.sp) }
                }
            }
        }
    }
}
