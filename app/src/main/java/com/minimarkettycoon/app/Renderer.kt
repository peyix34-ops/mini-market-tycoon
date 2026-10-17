package com.minimarkettycoon.app

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

private val Ink = Color(0xFF2B1B12)

/* ---------------- ZEMİN ---------------- */

fun DrawScope.drawGround(world: World, vis: Rect) {
    drawRect(Color(0xFF6CC24A), Offset.Zero, Size(world.size, world.size))
    val tile = 160f
    val i0 = floor(vis.left / tile).toInt().coerceAtLeast(0)
    val i1 = floor(vis.right / tile).toInt()
    val j0 = floor(vis.top / tile).toInt().coerceAtLeast(0)
    val j1 = floor(vis.bottom / tile).toInt()
    for (i in i0..i1) for (j in j0..j1) {
        if ((i + j) % 2 == 0) {
            drawRect(Color(0xFF65BA44), Offset(i * tile, j * tile), Size(tile, tile))
        }
    }
}

fun DrawScope.drawPond(pond: Pond, time: Float) {
    drawOval(Color(0xFFE9D9A6), Offset(pond.x - pond.rx - 16f, pond.y - pond.ry - 12f),
        Size((pond.rx + 16f) * 2f, (pond.ry + 12f) * 2f))
    drawOval(
        Brush.verticalGradient(
            listOf(Color(0xFF6FD3F2), Color(0xFF2E93D6)),
            startY = pond.y - pond.ry, endY = pond.y + pond.ry
        ),
        Offset(pond.x - pond.rx, pond.y - pond.ry), Size(pond.rx * 2f, pond.ry * 2f)
    )
    for (i in 0 until 3) {
        val f = (time * 0.25f + i / 3f) % 1f
        val rx = pond.rx * (0.25f + f * 0.7f)
        val ry = pond.ry * (0.25f + f * 0.7f)
        drawOval(
            Color.White.copy(alpha = (1f - f) * 0.45f),
            Offset(pond.x - rx, pond.y - ry), Size(rx * 2f, ry * 2f),
            style = Stroke(4f)
        )
    }
    drawOval(Color(0x55FFFFFF), Offset(pond.x - pond.rx * 0.6f, pond.y - pond.ry * 0.55f),
        Size(pond.rx * 0.5f, pond.ry * 0.16f))
}

fun DrawScope.drawRoads(world: World, vis: Rect) {
    // Kaldırımlar
    for (r in world.roads) drawRect(Color(0xFFDAD5C8), Offset(r.left - 24f, r.top - 24f),
        Size(r.width + 48f, r.height + 48f))
    // Asfalt
    for (r in world.roads) drawRect(Color(0xFF4B5059), Offset(r.left, r.top), Size(r.width, r.height))
    // Orta çizgiler
    for (r in world.roads) {
        val horizontal = r.width > r.height
        val dash = 60f
        val gap = 50f
        if (horizontal) {
            val cy = (r.top + r.bottom) / 2f
            var x = r.left + floor((max(vis.left, r.left) - r.left) / (dash + gap)) * (dash + gap)
            while (x < minOf(vis.right, r.right)) {
                val mid = Offset(x + dash / 2f, cy)
                if (world.roads.none { it !== r && it.contains(mid) }) {
                    drawLine(Color(0xFFFFE27A), Offset(x, cy), Offset(x + dash, cy), 6f, StrokeCap.Round)
                }
                x += dash + gap
            }
        } else {
            val cx = (r.left + r.right) / 2f
            var y = r.top + floor((max(vis.top, r.top) - r.top) / (dash + gap)) * (dash + gap)
            while (y < minOf(vis.bottom, r.bottom)) {
                val mid = Offset(cx, y + dash / 2f)
                if (world.roads.none { it !== r && it.contains(mid) }) {
                    drawLine(Color(0xFFFFE27A), Offset(cx, y), Offset(cx, y + dash), 6f, StrokeCap.Round)
                }
                y += dash + gap
            }
        }
    }
}

/* ---------------- DEKOR ---------------- */

fun DrawScope.drawDeco(d: Deco, time: Float) {
    when (d.kind) {
        0, 1, 2 -> { // çiçek
            val c = when (d.kind) {
                0 -> Color(0xFFFF6B8B)
                1 -> Color(0xFFFFD84D)
                else -> Color(0xFFFFFFFF)
            }
            val sw = sin(time * 2f + d.seed) * 2f
            drawLine(Color(0xFF3F9A3C), Offset(d.x, d.y), Offset(d.x + sw, d.y - 14f), 3f, StrokeCap.Round)
            for (a in 0 until 5) {
                val ang = a * 2f * PI.toFloat() / 5f
                drawCircle(c, 5f, Offset(d.x + sw + cos(ang) * 7f, d.y - 16f + sin(ang) * 7f))
            }
            drawCircle(Color(0xFFFFA000), 4f, Offset(d.x + sw, d.y - 16f))
        }
        3 -> { // ot
            for (i in -1..1) {
                val sw = sin(time * 2.5f + d.seed + i) * 3f
                drawLine(Color(0xFF3F9A3C), Offset(d.x + i * 6f, d.y),
                    Offset(d.x + i * 9f + sw, d.y - 18f), 4f, StrokeCap.Round)
            }
        }
        4 -> { // taş
            drawOval(Color(0x33000000), Offset(d.x - 18f, d.y - 4f), Size(40f, 12f))
            drawOval(Color(0xFF9AA0A8), Offset(d.x - 16f, d.y - 20f), Size(32f, 24f))
            drawOval(Color(0xFFBCC2C9), Offset(d.x - 10f, d.y - 18f), Size(16f, 10f))
        }
        else -> { // çalı
            drawOval(Color(0x33000000), Offset(d.x - 28f, d.y - 4f), Size(60f, 14f))
            drawCircle(Color(0xFF2F8F3A), 20f, Offset(d.x - 14f, d.y - 16f))
            drawCircle(Color(0xFF2F8F3A), 20f, Offset(d.x + 14f, d.y - 16f))
            drawCircle(Color(0xFF45B04B), 22f, Offset(d.x, d.y - 24f))
        }
    }
}

/* ---------------- AĞAÇ ---------------- */

fun DrawScope.drawTree(t: TreeProp, time: Float) {
    val s = t.scale
    val sway = sin(time * 1.5f + t.seed) * 5f * s
    // gölge (güneş sağ üstte, gölge sol altta)
    drawOval(Color(0x33000000), Offset(t.x - 70f * s, t.y - 10f * s), Size(130f * s, 36f * s))
    // gövde
    drawRoundRect(Color(0xFF7A4B26), Offset(t.x - 13f * s, t.y - 70f * s), Size(26f * s, 72f * s), CornerRadius(8f * s))
    drawRect(Color(0x22000000), Offset(t.x + 3f * s, t.y - 70f * s), Size(10f * s, 70f * s))

    if (t.kind == 0) {
        val dark = Color(0xFF2E8B3A)
        val mid = Color(0xFF3FA84A)
        val light = Color(0xFF63CC62)
        drawCircle(dark, 50f * s, Offset(t.x - 42f * s + sway * 0.6f, t.y - 92f * s))
        drawCircle(dark, 50f * s, Offset(t.x + 42f * s + sway * 0.6f, t.y - 92f * s))
        drawCircle(mid, 62f * s, Offset(t.x + sway * 0.9f, t.y - 125f * s))
        drawCircle(light, 34f * s, Offset(t.x - 20f * s + sway, t.y - 145f * s))
        drawCircle(Color(0x33FFFFFF), 14f * s, Offset(t.x - 28f * s + sway, t.y - 155f * s))
    } else {
        fun tri(cy: Float, w: Float, h: Float, c: Color, sw: Float) {
            val path = Path().apply {
                moveTo(t.x + sw, cy - h)
                lineTo(t.x + w / 2f, cy)
                lineTo(t.x - w / 2f, cy)
                close()
            }
            drawPath(path, c)
        }
        tri(t.y - 40f * s, 150f * s, 80f * s, Color(0xFF2F7D4F), sway * 0.4f)
        tri(t.y - 85f * s, 125f * s, 80f * s, Color(0xFF3A9360), sway * 0.7f)
        tri(t.y - 130f * s, 95f * s, 80f * s, Color(0xFF47A86F), sway)
    }
}

/* ---------------- BİNALAR ---------------- */

fun DrawScope.drawBuilding(b: BuildingProp, tm: TextMeasurer) {
    val r = b.rect
    val wallH = 130f
    // zemin gölgesi
    drawRect(Color(0x30000000), Offset(r.left - 24f, r.bottom - 6f), Size(r.width + 24f, 26f))

    val wallColor = when (b.kind) { 0 -> Color(0xFFFFF0D2); 1 -> Color(0xFFBFE3F5); else -> Color(0xFFFFE3A3) }
    val roofColor = when (b.kind) { 0 -> Color(0xFF5E7C94); 1 -> Color(0xFFC4553B); else -> Color(0xFF8C5A3C) }
    val roofLight = when (b.kind) { 0 -> Color(0xFF7C9BB3); 1 -> Color(0xFFE27A5B); else -> Color(0xFFB57A55) }

    // ön duvar
    drawRect(wallColor, Offset(r.left, r.bottom - wallH), Size(r.width, wallH))
    drawRect(Color(0x14000000), Offset(r.left, r.bottom - 22f), Size(r.width, 22f))

    // çatı (eğik izdüşüm)
    val roofTop = r.top - wallH
    val roofBottom = r.bottom - wallH + 10f
    drawRoundRect(
        Brush.verticalGradient(listOf(roofLight, roofColor), startY = roofTop, endY = roofBottom),
        Offset(r.left - 14f, roofTop), Size(r.width + 28f, roofBottom - roofTop), CornerRadius(14f)
    )
    drawRoundRect(Ink.copy(alpha = 0.35f), Offset(r.left - 14f, roofTop), Size(r.width + 28f, roofBottom - roofTop),
        CornerRadius(14f), style = Stroke(5f))

    if (b.kind == 0) {
        // çatı detayları
        drawRoundRect(Color(0xFFA9BFD2), Offset(r.left + 40f, roofTop + 30f), Size(90f, 60f), CornerRadius(8f))
        drawRoundRect(Color(0xFFA9BFD2), Offset(r.right - 140f, roofTop + 50f), Size(70f, 50f), CornerRadius(8f))
        // tabela
        val style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
        val layout = tm.measure("MARKET", style)
        val bw = layout.size.width + 40f
        val bx = (r.left + r.right) / 2f - bw / 2f
        val by = r.bottom - wallH - 4f
        drawRoundRect(Color(0xFFE5484D), Offset(bx, by - 52f), Size(bw, 48f), CornerRadius(12f))
        drawRoundRect(Color.White, Offset(bx, by - 52f), Size(bw, 48f), CornerRadius(12f), style = Stroke(4f))
        drawText(layout, topLeft = Offset(bx + 20f, by - 52f + (48f - layout.size.height) / 2f))

        // tente
        val n = 10
        val aw = (r.width + 20f) / n
        for (i in 0 until n) {
            drawRect(if (i % 2 == 0) Color(0xFFE5484D) else Color.White,
                Offset(r.left - 10f + i * aw, r.bottom - wallH + 6f), Size(aw, 34f))
        }
        drawRect(Color(0x22000000), Offset(r.left - 10f, r.bottom - wallH + 40f), Size(r.width + 20f, 8f))
        // vitrinler + kapı
        val cx = (r.left + r.right) / 2f
        drawRoundRect(Color(0xFF9AD8FF), Offset(r.left + 30f, r.bottom - 85f), Size(120f, 55f), CornerRadius(8f))
        drawRoundRect(Color(0xFF9AD8FF), Offset(r.right - 150f, r.bottom - 85f), Size(120f, 55f), CornerRadius(8f))
        drawRoundRect(Color(0xFF8B5A2B), Offset(cx - 32f, r.bottom - 88f), Size(64f, 88f), CornerRadius(10f))
        drawRoundRect(Color(0xFF9AD8FF), Offset(cx - 24f, r.bottom - 80f), Size(48f, 40f), CornerRadius(6f))
    } else {
        // ev: pencereler + kapı
        val cx = (r.left + r.right) / 2f
        drawRoundRect(Color(0xFF9AD8FF), Offset(r.left + 26f, r.bottom - 95f), Size(56f, 50f), CornerRadius(8f))
        drawRoundRect(Color.White, Offset(r.left + 26f, r.bottom - 95f), Size(56f, 50f), CornerRadius(8f), style = Stroke(4f))
        drawRoundRect(Color(0xFF9AD8FF), Offset(r.right - 82f, r.bottom - 95f), Size(56f, 50f), CornerRadius(8f))
        drawRoundRect(Color.White, Offset(r.right - 82f, r.bottom - 95f), Size(56f, 50f), CornerRadius(8f), style = Stroke(4f))
        drawRoundRect(Color(0xFF8B5A2B), Offset(cx - 26f, r.bottom - 82f), Size(52f, 82f), CornerRadius(10f))
        drawCircle(Color(0xFFFFD84D), 4f, Offset(cx + 14f, r.bottom - 40f))
        // baca
        drawRect(Color(0xFF8A4B38), Offset(r.right - 80f, roofTop - 30f), Size(34f, 50f))
    }
}

/* ---------------- TOZ ---------------- */

fun DrawScope.drawDust(world: World) {
    for (d in world.dust) {
        val f = (d.life / 0.5f).coerceIn(0f, 1f)
        drawCircle(Color.White.copy(alpha = f * 0.45f), 6f + (1f - f) * 12f, Offset(d.x, d.y))
    }
}

/* ---------------- KARAKTER ---------------- */

private fun DrawScope.limb(a: Offset, b: Offset, w: Float, c: Color) {
    drawLine(Ink, a, b, w + 5f, StrokeCap.Round)
    drawLine(c, a, b, w, StrokeCap.Round)
}

fun DrawScope.drawPlayer(p: Player, time: Float) {
    val s = 1.25f
    val sp = sin(p.walkPhase) * p.moveAmt
    val cp = cos(p.walkPhase) * p.moveAmt
    val bob = abs(sin(p.walkPhase)) * 5f * p.moveAmt + sin(time * 2.2f) * 1.4f * (1f - p.moveAmt)
    val lean = (p.vx / 360f) * 4f
    val breathe = sin(time * 2.2f) * 1.2f * (1f - p.moveAmt)

    val skin = Color(0xFFFFCBA4)
    val skinDark = Color(0xFFE8A97E)
    val hair = Color(0xFF3B2416)
    val shirt = Color(0xFF1FA2A6)
    val shirtLight = Color(0xFF39C7C9)
    val pants = Color(0xFF2F3E6B)
    val shoe = Color(0xFF5B3A22)
    val cap = Color(0xFFFF9F1C)
    val capDark = Color(0xFFE07F00)

    fun P(dx: Float, dy: Float) = Offset(p.x + dx * s, p.y + dy * s)

    // Gölge (zıplarken küçülür)
    val shw = 54f * (1f - bob * 0.012f)
    drawOval(Color(0x44000000), Offset(p.x - shw / 2f * s, p.y - 8f * s), Size(shw * s, 16f * s))

    val hipY = -28f - bob
    val shY = -58f - bob + breathe
    val headY = -81f - bob + breathe * 1.4f

    val side = p.dir == 1
    val fx = p.faceX

    fun shoeAt(o: Offset, forward: Float) {
        drawOval(Ink, Offset(o.x - 11f * s + forward * 2f, o.y - 7f * s), Size(22f * s, 13f * s))
        drawOval(shoe, Offset(o.x - 9f * s + forward * 2f, o.y - 6f * s), Size(18f * s, 10f * s))
    }

    if (!side) {
        // ----- ÖN / ARKA GÖRÜNÜM -----
        val legA = P(-9f, -3f + sp * 8f - max(0f, cp) * 5f)
        val legB = P(9f, -3f - sp * 8f - max(0f, -cp) * 5f)
        limb(P(-9f, hipY), legA, 13f * s, pants)
        limb(P(9f, hipY), legB, 13f * s, pants)
        shoeAt(legA, 0f)
        shoeAt(legB, 0f)

        // kollar (gövdenin arkasında kalan kısım için önce)
        val armIdle = sin(time * 2.2f) * 1.5f * (1f - p.moveAmt)
        val handL = P(-25f + lean, -34f - bob * 0.5f - sp * 8f + armIdle)
        val handR = P(25f + lean, -34f - bob * 0.5f + sp * 8f - armIdle)
        val shL = P(-20f + lean, shY + 6f)
        val shR = P(20f + lean, shY + 6f)
        if (p.dir == 2) {
            limb(shL, handL, 11f * s, shirt); drawCircle(skin, 6f * s, handL)
            limb(shR, handR, 11f * s, shirt); drawCircle(skin, 6f * s, handR)
        }

        // gövde
        val torsoTL = P(-19f + lean, shY - 6f)
        val torsoSize = Size(38f * s, 38f * s)
        val br = Brush.verticalGradient(listOf(shirtLight, shirt),
            startY = torsoTL.y, endY = torsoTL.y + torsoSize.height)
        drawRoundRect(Ink, Offset(torsoTL.x - 2.5f, torsoTL.y - 2.5f),
            Size(torsoSize.width + 5f, torsoSize.height + 5f), CornerRadius(16f * s))
        drawRoundRect(br, torsoTL, torsoSize, CornerRadius(14f * s))
        // kemer
        drawRect(Color(0xFF1E2A4A), Offset(torsoTL.x, torsoTL.y + torsoSize.height - 9f * s),
            Size(torsoSize.width, 5f * s))
        if (p.dir == 0) {
            // yaka + isimlik
            drawRoundRect(Color.White, P(-9f + lean, shY - 6f), Size(18f * s, 7f * s), CornerRadius(4f * s))
            drawRoundRect(Color.White, P(5f + lean, shY + 10f), Size(11f * s, 7f * s), CornerRadius(2f * s))
        }

        if (p.dir == 0) {
            limb(shL, handL, 11f * s, shirt); drawCircle(skin, 6f * s, handL)
            limb(shR, handR, 11f * s, shirt); drawCircle(skin, 6f * s, handR)
        }

        // baş
        val hc = P(lean * 1.4f, headY)
        drawCircle(Ink, 21.5f * s, hc)
        drawCircle(skin, 19f * s, hc)
        drawCircle(skinDark, 4.5f * s, P(-19f + lean * 1.4f, headY + 2f))   // kulaklar
        drawCircle(skinDark, 4.5f * s, P(19f + lean * 1.4f, headY + 2f))
        if (p.dir == 0) {
            // saç kakülü + yüz
            val blink = if ((time % 3.4f) < 0.12f) 1f else 4.2f
            drawOval(Ink, P(-9f + lean * 1.4f, headY + 2f), Size(5.5f * s, blink * s))
            drawOval(Ink, P(4f + lean * 1.4f, headY + 2f), Size(5.5f * s, blink * s))
            drawCircle(Color(0x55FF7A7A), 4f * s, P(-13f + lean * 1.4f, headY + 9f))
            drawCircle(Color(0x55FF7A7A), 4f * s, P(13f + lean * 1.4f, headY + 9f))
            drawArc(Ink, 20f, 140f, false, P(-6f + lean * 1.4f, headY + 7f), Size(12f * s, 8f * s),
                style = Stroke(2.5f * s, cap = StrokeCap.Round))
            // şapka
            drawArc(Ink, 180f, 180f, true, P(-22f + lean * 1.4f, headY - 20f), Size(44f * s, 36f * s))
            drawArc(cap, 180f, 180f, true, P(-20f + lean * 1.4f, headY - 18f), Size(40f * s, 32f * s))
            drawRoundRect(Ink, P(-24f + lean * 1.4f, headY - 4f), Size(48f * s, 11f * s), CornerRadius(6f * s))
            drawRoundRect(capDark, P(-22f + lean * 1.4f, headY - 3f), Size(44f * s, 8f * s), CornerRadius(5f * s))
        } else {
            // arka: saç + şapka
            drawCircle(hair, 17f * s, P(lean * 1.4f, headY + 2f))
            drawArc(Ink, 180f, 180f, true, P(-22f + lean * 1.4f, headY - 20f), Size(44f * s, 40f * s))
            drawArc(cap, 180f, 180f, true, P(-20f + lean * 1.4f, headY - 18f), Size(40f * s, 36f * s))
            drawCircle(capDark, 3.5f * s, P(lean * 1.4f, headY - 20f))
        }
    } else {
        // ----- YAN GÖRÜNÜM -----
        val farLeg = P(-fx * sp * 14f, -3f - max(0f, -cp) * 7f)
        val nearLeg = P(fx * sp * 14f, -3f - max(0f, cp) * 7f)
        val farHand = P(-fx * sp * 14f * 0.9f + fx * 2f, -36f - bob * 0.5f)
        val nearHand = P(fx * sp * 14f * 0.9f + fx * 2f, -36f - bob * 0.5f)
        val armIdle = sin(time * 2.2f) * 1.5f * (1f - p.moveAmt)

        // uzak kol + bacak
        limb(P(0f, shY + 6f), P(farHand.x - p.x, 0f).let { Offset(farHand.x, farHand.y + armIdle) }, 10f * s, Color(0xFF168286))
        drawCircle(skinDark, 5.5f * s, Offset(farHand.x, farHand.y + armIdle))
        limb(P(0f, hipY), farLeg, 12f * s, Color(0xFF263257))
        shoeAt(farLeg, fx * 3f)

        // gövde
        val torsoTL = P(-14f + lean, shY - 6f)
        val torsoSize = Size(28f * s, 38f * s)
        val br = Brush.verticalGradient(listOf(shirtLight, shirt),
            startY = torsoTL.y, endY = torsoTL.y + torsoSize.height)
        drawRoundRect(Ink, Offset(torsoTL.x - 2.5f, torsoTL.y - 2.5f),
            Size(torsoSize.width + 5f, torsoSize.height + 5f), CornerRadius(15f * s))
        drawRoundRect(br, torsoTL, torsoSize, CornerRadius(13f * s))
        drawRect(Color(0xFF1E2A4A), Offset(torsoTL.x, torsoTL.y + torsoSize.height - 9f * s),
            Size(torsoSize.width, 5f * s))

        // yakın bacak + kol
        limb(P(0f, hipY), nearLeg, 13f * s, pants)
        shoeAt(nearLeg, fx * 3f)
        limb(P(lean, shY + 6f), Offset(nearHand.x, nearHand.y - armIdle), 11f * s, shirt)
        drawCircle(skin, 6f * s, Offset(nearHand.x, nearHand.y - armIdle))

        // baş
        val hc = P(lean * 1.4f + fx * 2f, headY)
        drawCircle(Ink, 21.5f * s, hc)
        drawCircle(skin, 19f * s, hc)
        drawCircle(hair, 12f * s, P(-fx * 9f + lean * 1.4f, headY - 2f))
        drawCircle(skinDark, 5f * s, P(-fx * 2f + lean * 1.4f, headY + 3f))
        val blink = if ((time % 3.4f) < 0.12f) 1f else 4.2f
        drawOval(Ink, P(fx * 9f + lean * 1.4f - 2.7f, headY + 2f), Size(5.5f * s, blink * s))
        drawCircle(Color(0x55FF7A7A), 4f * s, P(fx * 11f + lean * 1.4f, headY + 9f))
        drawArc(Ink, 20f, 140f, false, P(fx * 9f - 6f + lean * 1.4f, headY + 7f), Size(10f * s, 7f * s),
            style = Stroke(2.5f * s, cap = StrokeCap.Round))
        // şapka + siper
        drawArc(Ink, 180f, 180f, true, P(-22f + lean * 1.4f + fx * 2f, headY - 20f), Size(44f * s, 38f * s))
        drawArc(cap, 180f, 180f, true, P(-20f + lean * 1.4f + fx * 2f, headY - 18f), Size(40f * s, 34f * s))
        val brimX = if (fx > 0f) 8f else -34f
        drawRoundRect(Ink, P(brimX + lean * 1.4f + fx * 2f, headY - 5f), Size(26f * s, 10f * s), CornerRadius(5f * s))
        drawRoundRect(capDark, P(brimX + 1.5f + lean * 1.4f + fx * 2f, headY - 4f), Size(23f * s, 7f * s), CornerRadius(4f * s))
    }
}
