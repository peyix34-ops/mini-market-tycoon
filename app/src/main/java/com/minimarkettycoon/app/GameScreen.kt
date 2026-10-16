package com.minimarkettycoon.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

private class JoyState {
    var active by mutableStateOf(false)
    var origin by mutableStateOf(Offset.Zero)
    var thumb by mutableStateOf(Offset.Zero)
}

@Composable
fun GameScreen() {
    val world = remember { World() }
    val joy = remember { JoyState() }
    var tick by remember { mutableLongStateOf(0L) }
    var viewSize by remember { mutableStateOf(IntSize(1080, 2340)) }
    val textMeasurer = rememberTextMeasurer()
    val joyRadius = 130f   // piksel

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now

                var jx = 0f
                var jy = 0f
                if (joy.active) {
                    val dx = (joy.thumb.x - joy.origin.x) / joyRadius
                    val dy = (joy.thumb.y - joy.origin.y) / joyRadius
                    val len = hypot(dx, dy)
                    if (len > 0.12f) {
                        val m = len.coerceAtMost(1f)
                        jx = dx / len * m
                        jy = dy / len * m
                    }
                }
                val zoom = viewSize.width / 900f
                world.update(dt, jx, jy, viewSize.width / zoom, viewSize.height / zoom)
                tick = now
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    joy.origin = down.position
                    joy.thumb = down.position
                    joy.active = true
                    drag(down.id) { change ->
                        val d = change.position - joy.origin
                        val len = hypot(d.x, d.y)
                        // thumb, yarıçapı aşarsa merkez onu takip etsin (kayan joystick)
                        if (len > joyRadius) {
                            val k = (len - joyRadius) / len
                            joy.origin = joy.origin + d * k
                        }
                        joy.thumb = change.position
                        change.consume()
                    }
                    joy.active = false
                }
            }
    ) {
        // DÜNYA
        Canvas(Modifier.fillMaxSize()) {
            val t = tick   // her karede yeniden çiz
            val zoom = size.width / 900f
            val viewW = size.width / zoom
            val viewH = size.height / zoom
            val vis = Rect(world.camX - viewW / 2f - 200f, world.camY - viewH / 2f - 350f,
                world.camX + viewW / 2f + 200f, world.camY + viewH / 2f + 200f)

            withTransform({
                scale(zoom, zoom, Offset.Zero)
                translate(viewW / 2f - world.camX, viewH / 2f - world.camY)
            }) {
                drawGround(world, vis)
                drawPond(world.pond, world.time)
                drawRoads(world, vis)
                for (d in world.decos) {
                    if (vis.contains(Offset(d.x, d.y))) drawDeco(d, world.time)
                }
                drawDust(world)

                val player = world.player
                var playerDrawn = false
                for (prop in world.props) {
                    if (!playerDrawn && prop.sortY > player.y) {
                        drawPlayer(player, world.time)
                        playerDrawn = true
                    }
                    when (prop) {
                        is TreeProp -> if (vis.contains(Offset(prop.x, prop.y))) drawTree(prop, world.time)
                        is BuildingProp -> if (vis.overlaps(prop.rect.inflate(60f))) drawBuilding(prop, textMeasurer)
                    }
                }
                if (!playerDrawn) drawPlayer(player, world.time)
            }
        }

        // JOYSTICK
        Canvas(Modifier.fillMaxSize()) {
            if (joy.active) {
                drawCircle(Color(0x33FFFFFF), joyRadius, joy.origin)
                drawCircle(Color(0x88FFFFFF), joyRadius, joy.origin, style = Stroke(4f))
                val d = joy.thumb - joy.origin
                val len = hypot(d.x, d.y)
                val clamped = if (len > joyRadius) joy.origin + d * (joyRadius / len) else joy.thumb
                drawCircle(Color(0xCCFFFFFF), 52f, clamped)
                drawCircle(Color(0x55000000), 52f, clamped, style = Stroke(3f))
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
    }
}
