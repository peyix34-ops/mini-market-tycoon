package com.minimarkettycoon.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Outline = Color(0xFF5A2E0F)

@Composable
fun MainMenuScreen(onPlay: () -> Unit) {
    val t = rememberInfiniteTransition(label = "menu")
    val cloudShift by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(70000, easing = LinearEasing)),
        label = "clouds"
    )
    val sunRotation by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing)),
        label = "sunRot"
    )
    val sunScale by t.animateFloat(
        initialValue = 0.95f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sunScale"
    )
    val bob by t.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )

    Box(Modifier.fillMaxSize()) {
        SkyBackground(cloudShift, sunRotation, sunScale)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.9f))   // başlık ortanın biraz üstünde
            GameTitle(bob)
            Spacer(Modifier.height(48.dp))
            PlayButton(onPlay)
            Spacer(Modifier.weight(1.4f))
        }
    }
}

/* ---------------- ARKA PLAN ---------------- */

@Composable
private fun SkyBackground(cloudShift: Float, sunRotation: Float, sunScale: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Gökyüzü
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF3FA9F5), Color(0xFF8AD3FF), Color(0xFFFFF1C9)),
                startY = 0f, endY = h * 0.85f
            )
        )

        // Güneş
        val sunC = Offset(w * 0.78f, h * 0.13f)
        val r = w * 0.10f * sunScale
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x88FFE680), Color.Transparent),
                center = sunC, radius = r * 3.4f
            ),
            radius = r * 3.4f, center = sunC
        )
        rotate(sunRotation, pivot = sunC) {
            for (i in 0 until 12) {
                rotate(i * 30f, pivot = sunC) {
                    drawLine(
                        color = Color(0xFFFFD84D),
                        start = Offset(sunC.x, sunC.y - r * 1.35f),
                        end = Offset(sunC.x, sunC.y - r * 1.9f),
                        strokeWidth = r * 0.18f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        drawCircle(Color(0xFFFFE066), r, sunC)
        drawCircle(Color(0xFFFFC93C), r, sunC, style = Stroke(r * 0.08f))

        // Bulutlar
        fun cx(base: Float) = ((base + cloudShift) % 1f) * (w + 500f) - 250f
        cloud(cx(0.05f), h * 0.10f, w / 420f, 0.95f)
        cloud(cx(0.40f), h * 0.22f, w / 330f, 0.90f)
        cloud(cx(0.70f), h * 0.07f, w / 500f, 0.85f)
        cloud(cx(0.88f), h * 0.30f, w / 380f, 0.80f)

        // Uzak tepe
        val farHill = Path().apply {
            moveTo(0f, h * 0.80f)
            cubicTo(w * 0.20f, h * 0.70f, w * 0.60f, h * 0.84f, w, h * 0.74f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(
            farHill,
            Brush.verticalGradient(
                listOf(Color(0xFF9BE37A), Color(0xFF5FBF55)),
                startY = h * 0.70f, endY = h
            )
        )

        // Yakın tepe
        val nearHill = Path().apply {
            moveTo(0f, h * 0.88f)
            cubicTo(w * 0.30f, h * 0.80f, w * 0.65f, h * 0.95f, w, h * 0.84f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(
            nearHill,
            Brush.verticalGradient(
                listOf(Color(0xFF7ED957), Color(0xFF3E9B3C)),
                startY = h * 0.80f, endY = h
            )
        )

        // Küçük market
        val sw = w * 0.36f
        val sh = h * 0.06f
        val sx = w * 0.5f - sw / 2f
        val sy = h * 0.885f
        drawRoundRect(Color(0xFFFFF3D6), Offset(sx, sy), Size(sw, sh), CornerRadius(10f))
        val n = 6
        val aw = sw * 1.1f / n
        for (i in 0 until n) {
            drawRect(
                if (i % 2 == 0) Color(0xFFE5484D) else Color.White,
                Offset(sx - sw * 0.05f + i * aw, sy - sh * 0.35f),
                Size(aw, sh * 0.45f)
            )
        }
        drawRoundRect(
            Color(0xFF8B5A2B),
            Offset(sx + sw * 0.42f, sy + sh * 0.3f),
            Size(sw * 0.16f, sh * 0.7f),
            CornerRadius(6f)
        )
        drawRoundRect(
            Color(0xFF9AD8FF),
            Offset(sx + sw * 0.10f, sy + sh * 0.25f),
            Size(sw * 0.24f, sh * 0.4f),
            CornerRadius(6f)
        )
        drawRoundRect(
            Color(0xFF9AD8FF),
            Offset(sx + sw * 0.66f, sy + sh * 0.25f),
            Size(sw * 0.24f, sh * 0.4f),
            CornerRadius(6f)
        )
    }
}

private fun DrawScope.cloud(cx: Float, cy: Float, s: Float, alpha: Float) {
    val c = Color.White.copy(alpha = alpha)
    drawCircle(c, 38f * s, Offset(cx - 45f * s, cy + 8f * s))
    drawCircle(c, 52f * s, Offset(cx, cy - 8f * s))
    drawCircle(c, 40f * s, Offset(cx + 50f * s, cy + 6f * s))
    drawRoundRect(c, Offset(cx - 80f * s, cy + 10f * s), Size(160f * s, 36f * s), CornerRadius(18f * s))
}

/* ---------------- LOGO ---------------- */

@Composable
private fun GameTitle(bob: Float) {
    Column(
        modifier = Modifier.graphicsLayer {
            translationY = bob
            rotationZ = -3f
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedText(
            text = "MINI",
            fontSize = 44.sp,
            colors = listOf(Color(0xFFFFF176), Color(0xFFFFB300)),
            letterSpacing = 8.sp
        )
        OutlinedText(
            text = "MARKET",
            fontSize = 62.sp,
            colors = listOf(Color(0xFFFFEE58), Color(0xFFFF8F00)),
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(6.dp))
        val ribbon = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .shadow(8.dp, ribbon)
                .clip(ribbon)
                .background(Brush.verticalGradient(listOf(Color(0xFFFF5A5F), Color(0xFFC62828))))
                .border(3.dp, Color.White, ribbon)
                .padding(horizontal = 26.dp, vertical = 4.dp)
        ) {
            Text(
                text = "TYCOON",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = Color.White,
                    shadow = Shadow(Outline, Offset(0f, 4f), 6f)
                )
            )
        }
    }
}

@Composable
private fun OutlinedText(
    text: String,
    fontSize: TextUnit,
    colors: List<Color>,
    letterSpacing: TextUnit
) {
    Box(contentAlignment = Alignment.Center) {
        // Alt gölge
        Text(
            text = text,
            modifier = Modifier.offset(y = 6.dp),
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = letterSpacing,
                color = Color(0x66000000),
                drawStyle = Stroke(width = 18f, join = StrokeJoin.Round)
            )
        )
        // Koyu çerçeve
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = letterSpacing,
                color = Outline,
                drawStyle = Stroke(width = 16f, join = StrokeJoin.Round)
            )
        )
        // Renkli iç dolgu
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = letterSpacing,
                brush = Brush.verticalGradient(colors)
            )
        )
    }
}

/* ---------------- OYNA BUTONU ---------------- */

@Composable
private fun PlayButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "press"
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseValue"
    )
    val shape = RoundedCornerShape(40.dp)

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale * pulse
                scaleY = pressScale * pulse
            }
            .shadow(14.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF9BEA55), Color(0xFF2FA83A))))
            .border(4.dp, Color.White, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 54.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(26.dp)) {
                val p = Path().apply {
                    moveTo(size.width * 0.15f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(size.width * 0.15f, size.height)
                    close()
                }
                drawPath(p, Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = "OYNA",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = Color.White,
                    shadow = Shadow(Color(0xFF1B5E20), Offset(0f, 4f), 6f)
                )
            )
        }
    }
}
