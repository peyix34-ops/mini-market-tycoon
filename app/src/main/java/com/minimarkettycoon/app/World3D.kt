package com.minimarkettycoon.app

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.random.Random

class Puff(var x: Float, var z: Float, var life: Float)
class Tree3(val x: Float, val z: Float, val s: Float, val kind: Int, val seed: Float)
class Tuft3(val x: Float, val z: Float, val s: Float)
class Flower3(val x: Float, val z: Float, val color: Int, val h: Float)
class Rock3(val x: Float, val z: Float, val s: Float)
class Patch3(val x: Float, val z: Float, val r: Float, val color: Int)

class Hero {
    var x = 0f
    var z = 6f
    var vx = 0f
    var vz = 0f
    var yaw = 0f
    var phase = 0f
    var moveAmt = 0f
}

class World3D {
    val hero = Hero()
    val trees = ArrayList<Tree3>()
    val tufts = ArrayList<Tuft3>()
    val flowers = ArrayList<Flower3>()
    val rocks = ArrayList<Rock3>()
    val patches = ArrayList<Patch3>()
    val puffs = ArrayList<Puff>()

    var time = 0f
    var camX = hero.x
    var camZ = hero.z

    // minX, minZ, maxX, maxZ
    private val shop = floatArrayOf(9.4f, -9.2f, 16.6f, -2.7f)
    private val plot = floatArrayOf(19.6f, -9.4f, 34.4f, 3.4f)

    private val speed = 6.5f
    private var puffTimer = 0f

    private fun free(x: Float, z: Float, m: Float): Boolean {
        if (abs(x) < 4f + m) return false                                          
        return true
    }

    init {
        val rnd = Random(21)
        // başlangıç yakınına elle birkaç ağaç
        trees += Tree3(-10f, 4f, 1.2f, 0, 0.5f)
        trees += Tree3(-15f, -6f, 1.1f, 1, 2.0f)
        trees += Tree3(10f, 12f, 1.0f, 1, 4.0f)
        var guard = 0
        while (trees.size < 22 && guard < 4000) {
            guard++
            val x = (rnd.nextFloat() - 0.5f) * 90f
            val z = (rnd.nextFloat() - 0.5f) * 70f
            if (!free(x, z, 3f)) continue
            if (trees.any { hypot(it.x - x, it.z - z) < 9f }) continue
            trees += Tree3(x, z, 0.9f + rnd.nextFloat() * 0.5f, rnd.nextInt(2), rnd.nextFloat() * 6.28f)
        }
        guard = 0
        while (tufts.size < 90 && guard < 1000) {
            guard++
            val x = (rnd.nextFloat() - 0.5f) * 100f
            val z = (rnd.nextFloat() - 0.5f) * 80f
            if (!free(x, z, 0.5f)) continue
            tufts += Tuft3(x, z, 0.7f + rnd.nextFloat() * 0.7f)
        }
        val palette = intArrayOf(0xFF6B8B, 0xFFD84D, 0xFFFFFF, 0xB388FF)
        for (i in 0 until 14) {
            val cx = (rnd.nextFloat() - 0.5f) * 90f
            val cz = (rnd.nextFloat() - 0.5f) * 70f
            if (!free(cx, cz, 2f)) continue
            for (k in 0 until 4) {
                flowers += Flower3(
                    cx + (rnd.nextFloat() - 0.5f) * 4f, cz + (rnd.nextFloat() - 0.5f) * 4f,
                    palette[rnd.nextInt(4)], 0.5f + rnd.nextFloat() * 0.3f
                )
            }
        }
        guard = 0
        while (rocks.size < 10 && guard < 1000) {
            guard++
            val x = (rnd.nextFloat() - 0.5f) * 80f
            val z = (rnd.nextFloat() - 0.5f) * 60f
            if (!free(x, z, 1.5f)) continue
            rocks += Rock3(x, z, 0.8f + rnd.nextFloat() * 0.8f)
        }
        guard = 0
        while (patches.size < 30 && guard < 1000) {
            guard++
            val x = (rnd.nextFloat() - 0.5f) * 110f
            val z = (rnd.nextFloat() - 0.5f) * 90f
            if (!free(x, z, 3f)) continue
            patches += Patch3(
                x, z, 3f + rnd.nextFloat() * 5f,
                if (rnd.nextBoolean()) 0x7ED55A else 0x5DB03F
            )
        }
    }

    fun update(dt: Float, jx: Float, jy: Float) {
        time += dt
        val h = hero

        val k = 1f - exp(-12f * dt)
        h.vx += (jx * speed - h.vx) * k
        h.vz += (jy * speed - h.vz) * k
        h.x += h.vx * dt
        h.z += h.vz * dt
        collide()
        h.x = h.x.coerceIn(-60f, 60f)
        h.z = h.z.coerceIn(-60f, 60f)

        val sp = hypot(h.vx, h.vz)
        val target = (sp / speed).coerceIn(0f, 1f)
        h.moveAmt += (target - h.moveAmt) * (1f - exp(-10f * dt))
        h.phase += dt * sp * 1.7f

        if (sp > 0.4f) {
            val ty = Math.toDegrees(atan2(h.vx.toDouble(), h.vz.toDouble())).toFloat()
            var d = (ty - h.yaw) % 360f
            if (d > 180f) d -= 360f
            if (d < -180f) d += 360f
            h.yaw += d * (1f - exp(-14f * dt))
        }

        // toz bulutları
        puffTimer -= dt
        if (sp > 3f && puffTimer <= 0f) {
            puffTimer = 0.12f
            puffs += Puff(h.x - h.vx * 0.05f, h.z - h.vz * 0.05f, 0.5f)
        }
        val it = puffs.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) it.remove()
        }

        // kamera
        val ck = 1f - exp(-5f * dt)
        camX += (h.x + h.vx * 0.25f - camX) * ck
        camZ += (h.z + h.vz * 0.25f - camZ) * ck
    }

    private fun pushCircle(cx: Float, cz: Float, r: Float) {
        val h = hero
        val dx = h.x - cx
        val dz = h.z - cz
        val d = hypot(dx, dz)
        if (d < r && d > 0.0001f) {
            h.x = cx + dx / d * r
            h.z = cz + dz / d * r
        }
    }

    private fun pushRect(r: FloatArray, rad: Float) {
        val h = hero
        val cx = h.x.coerceIn(r[0], r[2])
        val cz = h.z.coerceIn(r[1], r[3])
        val dx = h.x - cx
        val dz = h.z - cz
        val d = hypot(dx, dz)
        if (d < rad && d > 0.0001f) {
            h.x = cx + dx / d * rad
            h.z = cz + dz / d * rad
        }
    }

    private fun collide() {
        for (t in trees) {
            if (abs(t.x - hero.x) > 5f || abs(t.z - hero.z) > 5f) continue
            pushCircle(t.x, t.z, 0.35f * t.s + 0.45f)
        }
        for (r in rocks) {
            if (abs(r.x - hero.x) > 4f || abs(r.z - hero.z) > 4f) continue
            pushCircle(r.x, r.z, 0.55f * r.s + 0.35f)
        }
        
    }
}
