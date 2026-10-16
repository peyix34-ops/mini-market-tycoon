package com.minimarkettycoon.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

sealed class Prop { abstract val sortY: Float }

class TreeProp(
    val x: Float, val y: Float, val scale: Float, val kind: Int, val seed: Float
) : Prop() {
    override val sortY: Float get() = y
}

class BuildingProp(val rect: Rect, val kind: Int) : Prop() {
    override val sortY: Float get() = rect.bottom
}

class Deco(val x: Float, val y: Float, val kind: Int, val seed: Float)
class Pond(val x: Float, val y: Float, val rx: Float, val ry: Float)
class Dust(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float)

class Player {
    var x = 1600f
    var y = 1820f
    var vx = 0f
    var vy = 0f
    var dir = 0            // 0 = ön, 1 = yan, 2 = arka
    var faceX = 1f         // yan bakışta yön (+1 sağ, -1 sol)
    var walkPhase = 0f
    var moveAmt = 0f
}

class World {
    val size = 3200f

    val roads = listOf(
        Rect(0f, 1500f, size, 1700f),
        Rect(1500f, 0f, 1700f, size),
        Rect(300f, 700f, 1500f, 820f),
        Rect(2400f, 1700f, 2520f, 3000f)
    )

    val buildings = listOf(
        BuildingProp(Rect(1780f, 1090f, 2240f, 1440f), 0),   // market
        BuildingProp(Rect(900f, 1180f, 1200f, 1420f), 1),
        BuildingProp(Rect(2650f, 1050f, 2950f, 1300f), 2)
    )

    val pond = Pond(700f, 2400f, 260f, 170f)

    val trees = ArrayList<TreeProp>()
    val decos = ArrayList<Deco>()
    val props: List<Prop>

    val player = Player()
    val dust = ArrayList<Dust>()

    var time = 0f
    var camX = player.x
    var camY = player.y
    private var dustTimer = 0f
    private val maxSpeed = 360f
    private val playerRadius = 22f

    init {
        val rnd = Random(7)
        var attempts = 0
        while (trees.size < 240 && attempts < 6000) {
            attempts++
            val x = rnd.nextFloat() * (size - 200f) + 100f
            val y = rnd.nextFloat() * (size - 200f) + 100f
            if (!isFree(x, y, 70f)) continue
            if (trees.any { hypot(it.x - x, it.y - y) < 120f }) continue
            trees += TreeProp(x, y, 0.8f + rnd.nextFloat() * 0.5f, rnd.nextInt(2), rnd.nextFloat() * 6.28f)
        }
        attempts = 0
        while (decos.size < 520 && attempts < 4000) {
            attempts++
            val x = rnd.nextFloat() * (size - 100f) + 50f
            val y = rnd.nextFloat() * (size - 100f) + 50f
            if (!isFree(x, y, 18f)) continue
            decos += Deco(x, y, rnd.nextInt(6), rnd.nextFloat() * 6.28f)
        }
        props = (trees + buildings).sortedBy { it.sortY }
    }

    private fun isFree(x: Float, y: Float, r: Float): Boolean {
        if (hypot(x - 1600f, y - 1820f) < 170f) return false
        for (rd in roads) if (rd.inflate(r + 40f).contains(Offset(x, y))) return false
        for (b in buildings) if (b.rect.inflate(r + 40f).translate(0f, -60f).contains(Offset(x, y))) return false
        val nx = (x - pond.x) / (pond.rx + r + 10f)
        val ny = (y - pond.y) / (pond.ry + r + 10f)
        if (nx * nx + ny * ny < 1f) return false
        return true
    }

    fun update(dt: Float, jx: Float, jy: Float, viewW: Float, viewH: Float) {
        time += dt
        val p = player

        // Hız (yumuşak ivmelenme)
        val k = 1f - exp(-14f * dt)
        p.vx += (jx * maxSpeed - p.vx) * k
        p.vy += (jy * maxSpeed - p.vy) * k
        p.x += p.vx * dt
        p.y += p.vy * dt
        collide(p)
        p.x = p.x.coerceIn(60f, size - 60f)
        p.y = p.y.coerceIn(60f, size - 60f)

        val speed = hypot(p.vx, p.vy)
        val target = (speed / maxSpeed).coerceIn(0f, 1f)
        p.moveAmt += (target - p.moveAmt) * (1f - exp(-10f * dt))
        p.walkPhase += dt * speed * 0.032f

        if (speed > 25f) {
            if (abs(p.vy) > abs(p.vx) * 1.1f) {
                p.dir = if (p.vy > 0f) 0 else 2
            } else {
                p.dir = 1
                p.faceX = if (p.vx > 0f) 1f else -1f
            }
        }

        // Toz bulutları
        dustTimer -= dt
        if (speed > 120f && dustTimer <= 0f) {
            dustTimer = 0.11f
            dust += Dust(p.x - p.vx * 0.04f, p.y - 2f, -p.vx * 0.05f, -p.vy * 0.05f - 8f, 0.5f)
        }
        val it = dust.iterator()
        while (it.hasNext()) {
            val d = it.next()
            d.life -= dt
            d.x += d.vx * dt
            d.y += d.vy * dt
            if (d.life <= 0f) it.remove()
        }

        // Kamera (yumuşak takip + hafif ileri bakış)
        val tx = p.x + p.vx * 0.3f
        val ty = p.y + p.vy * 0.3f
        val ck = 1f - exp(-5f * dt)
        camX += (tx - camX) * ck
        camY += (ty - camY) * ck
        camX = camX.coerceIn(viewW / 2f, size - viewW / 2f)
        camY = camY.coerceIn(viewH / 2f, size - viewH / 2f)
    }

    private fun collide(p: Player) {
        // Ağaç gövdeleri
        for (t in trees) {
            if (abs(t.x - p.x) > 120f || abs(t.y - p.y) > 120f) continue
            pushOutCircle(p, t.x, t.y, 16f * t.scale + playerRadius)
        }
        // Binalar (zemin izi)
        for (b in buildings) {
            val r = b.rect
            val cx = p.x.coerceIn(r.left, r.right)
            val cy = p.y.coerceIn(r.top + 40f, r.bottom)
            val dx = p.x - cx
            val dy = p.y - cy
            val d = hypot(dx, dy)
            if (d < playerRadius && d > 0.001f) {
                p.x = cx + dx / d * playerRadius
                p.y = cy + dy / d * playerRadius
            }
        }
        // Göl
        val rx = pond.rx - 10f
        val ry = pond.ry - 10f
        val nx = (p.x - pond.x) / rx
        val ny = (p.y - pond.y) / ry
        val kk = sqrt(nx * nx + ny * ny)
        if (kk < 1f && kk > 0.0001f) {
            p.x = pond.x + (p.x - pond.x) / kk
            p.y = pond.y + (p.y - pond.y) / kk
        }
    }

    private fun pushOutCircle(p: Player, cx: Float, cy: Float, r: Float) {
        val dx = p.x - cx
        val dy = p.y - cy
        val d = hypot(dx, dy)
        if (d < r && d > 0.001f) {
            p.x = cx + dx / d * r
            p.y = cy + dy / d * r
        }
    }
}
