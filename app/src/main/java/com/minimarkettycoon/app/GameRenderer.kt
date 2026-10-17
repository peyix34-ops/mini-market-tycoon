package com.minimarkettycoon.app

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.sin

class CameraConfig {
    @Volatile var height = 24f
    @Volatile var back = 15f
    @Volatile var fov = 45f
}

private const val OUT = 0.035f

private const val GRASS = 0x6CC24A
private const val SHADOW = 0x4E9A3A
private const val TRUNK = 0x7A4B26
private const val LEAF1 = 0x3FA84A
private const val LEAF2 = 0x2E8B3A
private const val LEAF3 = 0x63CC62
private const val PINE1 = 0x2F7D4F
private const val PINE2 = 0x3A9360
private const val PINE3 = 0x47A86F
private const val ROAD = 0x4B5059
private const val CURB = 0xD9D4C7

private const val SKIN = 0xFFCBA4
private const val SKIN_D = 0xE8A97E
private const val SHIRT = 0x1FA2A6
private const val PANTS = 0x2F3E6B
private const val SHOE = 0x5B3A22
private const val HAIR = 0x3B2416
private const val CAP = 0xFF9F1C
private const val CAP_D = 0xE07F00
private const val BELT = 0x1E2A4A

class GameRenderer(
    private val input: GameInput,
    private val cam: CameraConfig
) : GLSurfaceView.Renderer {

    private val world = World3D()
    private val ms = MatStack()
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val inv = FloatArray(16)
    private val nrm = FloatArray(16)

    private var prog = 0
    private var aPos = 0
    private var aNrm = 0
    private var uVP = 0
    private var uModel = 0
    private var uNormal = 0
    private var uColor = 0
    private var uOutline = 0
    private var uLight = 0
    private var uFog = 0
    private var uCam = 0

    private lateinit var cube: Mesh
    private lateinit var sphere: Mesh
    private lateinit var cylinder: Mesh
    private lateinit var cone: Mesh

    private var aspect = 0.5f
    private var last = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.56f, 0.80f, 0.98f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glFrontFace(GLES20.GL_CCW)

        prog = GlUtil.program(Shaders.VS, Shaders.FS)
        aPos = GLES20.glGetAttribLocation(prog, "aPos")
        aNrm = GLES20.glGetAttribLocation(prog, "aNormal")
        uVP = GLES20.glGetUniformLocation(prog, "uVP")
        uModel = GLES20.glGetUniformLocation(prog, "uModel")
        uNormal = GLES20.glGetUniformLocation(prog, "uNormal")
        uColor = GLES20.glGetUniformLocation(prog, "uColor")
        uOutline = GLES20.glGetUniformLocation(prog, "uOutline")
        uLight = GLES20.glGetUniformLocation(prog, "uLight")
        uFog = GLES20.glGetUniformLocation(prog, "uFog")
        uCam = GLES20.glGetUniformLocation(prog, "uCam")

        cube = Prims.cube()
        sphere = Prims.sphere()
        cylinder = Prims.cylinder()
        cone = Prims.cone()
        last = 0L
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        if (last == 0L) last = now
        val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = now
        world.update(dt, input.jx, input.jy)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // kamera ayarları (ayarlar menüsünden canlı güncellenir)
        val camH = cam.height
        val camB = cam.back
        Matrix.perspectiveM(proj, 0, cam.fov, aspect, 1f, 400f)

        val cx = world.camX
        val cz = world.camZ
        val ex = cx
        val ey = camH
        val ez = cz + camB
        Matrix.setLookAtM(view, 0, ex, ey, ez, cx, 0f, cz, 0f, 0f, -1f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

        GLES20.glUseProgram(prog)
        GLES20.glUniformMatrix4fv(uVP, 1, false, vp, 0)
        GLES20.glUniform3f(uLight, 0.45f, 0.85f, 0.35f)
        GLES20.glUniform3f(uFog, 0.56f, 0.80f, 0.98f)
        GLES20.glUniform3f(uCam, ex, ey, ez)

        ms.reset()
        drawScene()
    }

    /* ---------------- yardımcılar ---------------- */

    private fun obj(m: Mesh, color: Int, outline: Float) {
        val model = ms.cur
        Matrix.invertM(inv, 0, model, 0)
        Matrix.transposeM(nrm, 0, inv, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniformMatrix4fv(uNormal, 1, false, nrm, 0)
        GLES20.glUniform3f(
            uColor,
            ((color shr 16) and 255) / 255f,
            ((color shr 8) and 255) / 255f,
            (color and 255) / 255f
        )
        GLES20.glUniform1f(uOutline, 0f)
        m.draw(aPos, aNrm)
        if (outline > 0f) {
            GLES20.glCullFace(GLES20.GL_FRONT)
            GLES20.glUniform1f(uOutline, outline)
            m.draw(aPos, aNrm)
            GLES20.glCullFace(GLES20.GL_BACK)
            GLES20.glUniform1f(uOutline, 0f)
        }
    }

    private fun part(
        m: Mesh, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float,
        color: Int, outline: Float = OUT
    ) {
        ms.push()
        ms.translate(x, y, z)
        ms.scale(sx, sy, sz)
        obj(m, color, outline)
        ms.pop()
    }

    private fun near(x: Float, z: Float, d: Float) =
        abs(x - world.camX) < d && abs(z - world.camZ) < d

    /* ---------------- sahne ---------------- */

    private fun drawScene() {
        val w = world

        // zemin
        part(cube, 0f, -0.25f, 0f, 500f, 0.5f, 500f, GRASS, 0f)
        for (p in w.patches) {
            if (near(p.x, p.z, 60f)) part(cylinder, p.x, 0.012f, p.z, p.r * 2f, 0.02f, p.r * 2f, p.color, 0f)
        }

        // yol + kaldırım + orta çizgiler
        part(cube, 0f, 0f, 0f, 6f, 0.1f, 500f, ROAD, 0f)
        part(cube, -3.45f, 0f, 0f, 0.9f, 0.16f, 500f, CURB, 0f)
        part(cube, 3.45f, 0f, 0f, 0.9f, 0.16f, 500f, CURB, 0f)
        var zi = -100
        while (zi <= 100) {
            if (abs(zi - w.camZ) < 70f) part(cube, 0f, 0.01f, zi.toFloat(), 0.22f, 0.1f, 2.4f, 0xFFE27A, 0f)
            zi += 5
        }

        // çiçek, çimen, taş
        for (f in w.flowers) {
            if (!near(f.x, f.z, 45f)) continue
            val sw = sin(w.time * 2f + f.x) * 4f
            ms.push()
            ms.translate(f.x, 0f, f.z)
            ms.rotate(sw, 0f, 0f, 1f)
            part(cylinder, 0f, f.h * 0.5f, 0f, 0.06f, f.h, 0.06f, 0x3F9A3C, 0f)
            part(sphere, 0f, f.h, 0f, 0.34f, 0.34f, 0.34f, f.color, 0.02f)
            part(sphere, 0f, f.h + 0.06f, 0f, 0.15f, 0.15f, 0.15f, 0xFFC107, 0f)
            ms.pop()
        }
        for (t in w.tufts) {
            if (!near(t.x, t.z, 45f)) continue
            val sw = sin(w.time * 2.5f + t.x) * 6f
            ms.push()
            ms.translate(t.x, 0f, t.z)
            ms.rotate(sw, 0f, 0f, 1f)
            part(cone, 0f, 0.3f * t.s, 0f, 0.35f * t.s, 0.6f * t.s, 0.35f * t.s, 0x3F9A3C, 0f)
            part(cone, 0.15f, 0.22f * t.s, 0.05f, 0.25f * t.s, 0.45f * t.s, 0.25f * t.s, 0x4DB04A, 0f)
            ms.pop()
        }
        for (r in w.rocks) {
            if (!near(r.x, r.z, 50f)) continue
            part(cylinder, r.x - 0.3f, 0.03f, r.z - 0.2f, 1.8f * r.s, 0.02f, 1.4f * r.s, SHADOW, 0f)
            part(sphere, r.x, 0.3f * r.s, r.z, 1.4f * r.s, 0.9f * r.s, 1.1f * r.s, 0x9AA0A8, 0.03f)
            part(sphere, r.x + 0.5f * r.s, 0.2f * r.s, r.z + 0.3f * r.s, 0.7f * r.s, 0.5f * r.s, 0.6f * r.s, 0xB4BAC1, 0.025f)
        }

        for (t in w.trees) {
            if (near(t.x, t.z, 60f)) drawTree(t)
        }
        drawHero()
    }

    private fun drawTree(t: Tree3) {
        val s = t.s
        val sway = sin(world.time * 1.3f + t.seed) * 1.6f
        part(cylinder, t.x - 0.9f * s, 0.03f, t.z - 0.6f * s, 3.8f * s, 0.02f, 3.3f * s, SHADOW, 0f)
        ms.push()
        ms.translate(t.x, 0f, t.z)
        if (t.kind == 0) {
            part(cylinder, 0f, 1.1f * s, 0f, 0.6f * s, 2.2f * s, 0.6f * s, TRUNK)
            ms.push()
            ms.translate(0f, 2f * s, 0f)
            ms.rotate(sway, 0f, 0f, 1f)
            ms.rotate(sway * 0.6f, 1f, 0f, 0f)
            part(sphere, 0f, 1.5f * s, 0f, 3.4f * s, 3.0f * s, 3.4f * s, LEAF1)
            part(sphere, -1.1f * s, 0.9f * s, 0.3f * s, 2.2f * s, 2.0f * s, 2.2f * s, LEAF2)
            part(sphere, 1.1f * s, 1.0f * s, -0.2f * s, 2.4f * s, 2.1f * s, 2.4f * s, LEAF2)
            part(sphere, 0.2f * s, 2.3f * s, 0.2f * s, 1.8f * s, 1.5f * s, 1.8f * s, LEAF3)
            ms.pop()
        } else {
            part(cylinder, 0f, 0.8f * s, 0f, 0.5f * s, 1.6f * s, 0.5f * s, TRUNK)
            ms.push()
            ms.translate(0f, 1.4f * s, 0f)
            ms.rotate(sway, 0f, 0f, 1f)
            part(cone, 0f, 1.2f * s, 0f, 3.2f * s, 2.6f * s, 3.2f * s, PINE1)
            part(cone, 0f, 2.5f * s, 0f, 2.6f * s, 2.4f * s, 2.6f * s, PINE2)
            part(cone, 0f, 3.7f * s, 0f, 1.9f * s, 2.2f * s, 1.9f * s, PINE3)
            ms.pop()
        }
        ms.pop()
    }

    private fun drawHero() {
        val h = world.hero
        val t = world.time
        val sw = sin(h.phase) * h.moveAmt
        val bob = abs(sin(h.phase)) * 0.09f * h.moveAmt
        val idle = 1f - h.moveAmt
        val breathe = sin(t * 2.4f) * 0.025f * idle

        // gölge
        part(cylinder, h.x - 0.15f, 0.04f, h.z - 0.1f, 1.3f - bob * 2f, 0.02f, 1.1f - bob * 2f, SHADOW, 0f)
        // toz
        for (p in world.puffs) {
            val f = p.life / 0.5f
            val sz = 0.55f * f + 0.05f
            part(sphere, p.x, 0.25f, p.z, sz, sz, sz, 0xFFFFFF, 0f)
        }

        ms.push()
        ms.translate(h.x, bob, h.z)
        ms.rotate(h.yaw, 0f, 1f, 0f)

        // bacaklar
        for (side in intArrayOf(-1, 1)) {
            val s = side.toFloat()
            ms.push()
            ms.translate(0.17f * s, 0.95f, 0f)
            ms.rotate(sw * s * 42f, 1f, 0f, 0f)
            part(cylinder, 0f, -0.45f, 0f, 0.26f, 0.9f, 0.26f, PANTS)
            part(cube, 0f, -0.93f, 0.08f, 0.3f, 0.16f, 0.46f, SHOE)
            ms.pop()
        }

        // üst gövde
        ms.push()
        ms.translate(0f, 0.95f, 0f)
        ms.rotate(8f * h.moveAmt, 1f, 0f, 0f)
        ms.rotate(sw * 5f, 0f, 1f, 0f)
        ms.translate(0f, -0.95f, 0f)

        part(cube, 0f, 1.32f, 0f, 0.68f, 0.78f + breathe * 2f, 0.4f, SHIRT)
        part(cube, 0f, 0.98f, 0f, 0.7f, 0.12f, 0.42f, BELT)

        // kollar
        for (side in intArrayOf(-1, 1)) {
            val s = side.toFloat()
            ms.push()
            ms.translate(0.44f * s, 1.62f, 0f)
            ms.rotate(-sw * s * 38f + sin(t * 2.4f) * 2f * idle, 1f, 0f, 0f)
            part(sphere, 0f, 0f, 0f, 0.28f, 0.28f, 0.28f, SHIRT)
            part(cylinder, 0f, -0.36f, 0f, 0.2f, 0.62f, 0.2f, SHIRT)
            part(sphere, 0f, -0.74f, 0f, 0.22f, 0.22f, 0.22f, SKIN)
            ms.pop()
        }

        // boyun + kafa
        part(cylinder, 0f, 1.74f, 0f, 0.2f, 0.14f, 0.2f, SKIN, 0f)
        ms.push()
        ms.translate(0f, 2.04f + breathe, 0f)
        ms.rotate(sin(t * 1.3f) * 2f * idle + sw * 3f, 0f, 0f, 1f)
        part(sphere, 0f, 0f, 0f, 0.58f, 0.6f, 0.56f, SKIN)
        part(sphere, 0f, 0.02f, -0.06f, 0.6f, 0.52f, 0.56f, HAIR)
        part(sphere, -0.29f, 0f, 0f, 0.1f, 0.14f, 0.08f, SKIN_D, 0.015f)
        part(sphere, 0.29f, 0f, 0f, 0.1f, 0.14f, 0.08f, SKIN_D, 0.015f)
        // yüz
        val blink = if ((t % 3.6f) < 0.12f) 0.015f else 0.11f
        part(sphere, -0.11f, 0.02f, 0.255f, 0.07f, blink, 0.05f, 0x1A1A1A, 0f)
        part(sphere, 0.11f, 0.02f, 0.255f, 0.07f, blink, 0.05f, 0x1A1A1A, 0f)
        part(cube, -0.11f, 0.12f, 0.265f, 0.11f, 0.03f, 0.03f, HAIR, 0f)
        part(cube, 0.11f, 0.12f, 0.265f, 0.11f, 0.03f, 0.03f, HAIR, 0f)
        part(sphere, 0f, -0.05f, 0.285f, 0.07f, 0.07f, 0.07f, SKIN_D, 0f)
        part(cube, 0f, -0.14f, 0.26f, 0.1f, 0.025f, 0.02f, 0x8A3B3B, 0f)
        // şapka
        part(sphere, 0f, 0.15f, 0f, 0.62f, 0.38f, 0.6f, CAP)
        part(sphere, 0f, 0.31f, 0f, 0.1f, 0.1f, 0.1f, CAP_D, 0.015f)
        ms.push()
        ms.translate(0f, 0.11f, 0.3f)
        ms.rotate(10f, 1f, 0f, 0f)
        ms.scale(0.42f, 0.05f, 0.32f)
        obj(cube, CAP_D, 0.025f)
        ms.pop()
        ms.pop() // kafa

        ms.pop() // üst gövde
        ms.pop() // root
    }
}
