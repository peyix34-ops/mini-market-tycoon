package com.minimarkettycoon.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GameInput {
    @Volatile var jx = 0f
    @Volatile var jy = 0f
}

class MatStack {
    private val stack = Array(24) { FloatArray(16) }
    private var top = 0
    init { Matrix.setIdentityM(stack[0], 0) }
    val cur: FloatArray get() = stack[top]
    fun reset() { top = 0; Matrix.setIdentityM(stack[0], 0) }
    fun push() { System.arraycopy(stack[top], 0, stack[top + 1], 0, 16); top++ }
    fun pop() { top-- }
    fun translate(x: Float, y: Float, z: Float) = Matrix.translateM(stack[top], 0, x, y, z)
    fun rotate(a: Float, x: Float, y: Float, z: Float) = Matrix.rotateM(stack[top], 0, a, x, y, z)
    fun scale(x: Float, y: Float, z: Float) = Matrix.scaleM(stack[top], 0, x, y, z)
}

object GlUtil {
    private fun shader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) throw RuntimeException("Shader hatasi: " + GLES20.glGetShaderInfoLog(s))
        return s
    }

    fun program(vs: String, fs: String): Int {
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, shader(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(p, shader(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) throw RuntimeException("Program hatasi: " + GLES20.glGetProgramInfoLog(p))
        return p
    }
}

object Shaders {
    val VS = """
        uniform mat4 uVP;
        uniform mat4 uModel;
        uniform mat4 uNormal;
        uniform float uOutline;
        attribute vec3 aPos;
        attribute vec3 aNormal;
        varying vec3 vN;
        varying vec3 vWorld;
        void main() {
            vec4 w = uModel * vec4(aPos, 1.0);
            if (uOutline > 0.0) {
                vec3 d = normalize((uModel * vec4(normalize(aPos), 0.0)).xyz);
                w.xyz += d * uOutline;
            }
            vN = (uNormal * vec4(aNormal, 0.0)).xyz;
            vWorld = w.xyz;
            gl_Position = uVP * w;
        }
    """.trimIndent()

    val FS = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        uniform vec3 uColor;
        uniform vec3 uLight;
        uniform vec3 uFog;
        uniform vec3 uCam;
        uniform float uOutline;
        varying vec3 vN;
        varying vec3 vWorld;
        void main() {
            vec3 col;
            if (uOutline > 0.0) {
                col = vec3(0.10, 0.07, 0.05);
            } else {
                float ndl = dot(normalize(vN), normalize(uLight));
                float b = ndl > 0.55 ? 1.0 : (ndl > 0.1 ? 0.80 : 0.62);
                col = uColor * b;
            }
            float f = clamp((distance(vWorld, uCam) - 45.0) / 110.0, 0.0, 1.0);
            gl_FragColor = vec4(mix(col, uFog, f), 1.0);
        }
    """.trimIndent()

    val SIGN_VS = """
        uniform mat4 uMVP;
        attribute vec3 aPos;
        attribute vec2 aUV;
        varying vec2 vUV;
        void main() {
            vUV = aUV;
            gl_Position = uMVP * vec4(aPos, 1.0);
        }
    """.trimIndent()

    val SIGN_FS = """
        precision mediump float;
        uniform sampler2D uTex;
        varying vec2 vUV;
        void main() {
            gl_FragColor = texture2D(uTex, vUV);
        }
    """.trimIndent()
}

class Mesh(vertices: FloatArray, indices: ShortArray) {
    private val vb: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(vertices); position(0) }
    private val ib: ShortBuffer = ByteBuffer.allocateDirect(indices.size * 2)
        .order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(indices); position(0) }
    private val count = indices.size
    private var vbo = 0
    private var ibo = 0

    private fun upload() {
        val ids = IntArray(2)
        GLES20.glGenBuffers(2, ids, 0)
        vbo = ids[0]
        ibo = ids[1]
        vb.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vb.capacity() * 4, vb, GLES20.GL_STATIC_DRAW)
        ib.position(0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, ib.capacity() * 2, ib, GLES20.GL_STATIC_DRAW)
    }

    fun draw(posLoc: Int, nrmLoc: Int) {
        if (vbo == 0) upload()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 24, 0)
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(nrmLoc, 3, GLES20.GL_FLOAT, false, 24, 12)
        GLES20.glEnableVertexAttribArray(nrmLoc)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, 0)
    }
}

object Prims {
    fun cube(): Mesh {
        val v = ArrayList<Float>()
        val ix = ArrayList<Short>()
        // her yüz: normal, u, v  (u x v = normal)
        val faces = arrayOf(
            floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
            floatArrayOf(-1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f),
            floatArrayOf(0f, 1f, 0f, 0f, 0f, 1f, 1f, 0f, 0f),
            floatArrayOf(0f, -1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f),
            floatArrayOf(0f, 0f, 1f, 1f, 0f, 0f, 0f, 1f, 0f),
            floatArrayOf(0f, 0f, -1f, 0f, 1f, 0f, 1f, 0f, 0f)
        )
        val corners = arrayOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)
        for (f in faces) {
            val base = v.size / 6
            for ((a, b) in corners) {
                v += f[0] * 0.5f + f[3] * 0.5f * a + f[6] * 0.5f * b
                v += f[1] * 0.5f + f[4] * 0.5f * a + f[7] * 0.5f * b
                v += f[2] * 0.5f + f[5] * 0.5f * a + f[8] * 0.5f * b
                v += f[0]; v += f[1]; v += f[2]
            }
            ix += base.toShort(); ix += (base + 1).toShort(); ix += (base + 2).toShort()
            ix += base.toShort(); ix += (base + 2).toShort(); ix += (base + 3).toShort()
        }
        return Mesh(v.toFloatArray(), ix.toShortArray())
    }

    fun sphere(lat: Int = 14, lon: Int = 22): Mesh {
        val v = ArrayList<Float>()
        val ix = ArrayList<Short>()
        val pi = PI.toFloat()
        for (i in 0..lat) {
            val th = pi * i / lat
            val st = sin(th)
            val ct = cos(th)
            for (j in 0..lon) {
                val ph = 2f * pi * j / lon
                val x = st * cos(ph)
                val y = ct
                val z = st * sin(ph)
                v += x * 0.5f; v += y * 0.5f; v += z * 0.5f
                v += x; v += y; v += z
            }
        }
        for (i in 0 until lat) for (j in 0 until lon) {
            val a = i * (lon + 1) + j
            val b = a + lon + 1
            ix += a.toShort(); ix += (a + 1).toShort(); ix += b.toShort()
            ix += (a + 1).toShort(); ix += (b + 1).toShort(); ix += b.toShort()
        }
        return Mesh(v.toFloatArray(), ix.toShortArray())
    }

    private fun addCaps(v: ArrayList<Float>, ix: ArrayList<Short>, seg: Int, withTop: Boolean) {
        val pi = PI.toFloat()
        if (withTop) {
            val c = v.size / 6
            v += 0f; v += 0.5f; v += 0f; v += 0f; v += 1f; v += 0f
            val r = v.size / 6
            for (j in 0..seg) {
                val ph = 2f * pi * j / seg
                v += cos(ph) * 0.5f; v += 0.5f; v += sin(ph) * 0.5f; v += 0f; v += 1f; v += 0f
            }
            for (j in 0 until seg) {
                ix += c.toShort(); ix += (r + j + 1).toShort(); ix += (r + j).toShort()
            }
        }
        val c2 = v.size / 6
        v += 0f; v += -0.5f; v += 0f; v += 0f; v += -1f; v += 0f
        val r2 = v.size / 6
        for (j in 0..seg) {
            val ph = 2f * pi * j / seg
            v += cos(ph) * 0.5f; v += -0.5f; v += sin(ph) * 0.5f; v += 0f; v += -1f; v += 0f
        }
        for (j in 0 until seg) {
            ix += c2.toShort(); ix += (r2 + j).toShort(); ix += (r2 + j + 1).toShort()
        }
    }

    fun cylinder(seg: Int = 24): Mesh {
        val v = ArrayList<Float>()
        val ix = ArrayList<Short>()
        val pi = PI.toFloat()
        for (j in 0..seg) {
            val ph = 2f * pi * j / seg
            val c = cos(ph)
            val s = sin(ph)
            v += c * 0.5f; v += 0.5f; v += s * 0.5f; v += c; v += 0f; v += s
            v += c * 0.5f; v += -0.5f; v += s * 0.5f; v += c; v += 0f; v += s
        }
        for (j in 0 until seg) {
            val t0 = 2 * j
            val b0 = t0 + 1
            val t1 = t0 + 2
            val b1 = t0 + 3
            ix += t0.toShort(); ix += t1.toShort(); ix += b0.toShort()
            ix += t1.toShort(); ix += b1.toShort(); ix += b0.toShort()
        }
        addCaps(v, ix, seg, true)
        return Mesh(v.toFloatArray(), ix.toShortArray())
    }

    fun cone(seg: Int = 24): Mesh {
        val v = ArrayList<Float>()
        val ix = ArrayList<Short>()
        val pi = PI.toFloat()
        val inv = 1f / sqrt(1.25f)
        for (j in 0..seg) {
            val ph = 2f * pi * j / seg
            val c = cos(ph)
            val s = sin(ph)
            val nx = c * inv
            val ny = 0.5f * inv
            val nz = s * inv
            v += 0f; v += 0.5f; v += 0f; v += nx; v += ny; v += nz
            v += c * 0.5f; v += -0.5f; v += s * 0.5f; v += nx; v += ny; v += nz
        }
        for (j in 0 until seg) {
            val apex = 2 * j
            val b0 = apex + 1
            val b1 = apex + 3
            ix += apex.toShort(); ix += b1.toShort(); ix += b0.toShort()
        }
        addCaps(v, ix, seg, false)
        return Mesh(v.toFloatArray(), ix.toShortArray())
    }
}

/** Yazı dokulu tabela (MARKET, SATILIK ARSA). GL thread'inde oluşturulmalı. */
class SignQuad(
    text1: String, text2: String?, bg: Int, fg: Int, border: Int, w: Int, h: Int
) {
    private var tex = 0
    private val vb: FloatBuffer
    private val ib: ShortBuffer

    init {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val hf = h.toFloat()
        val wf = w.toFloat()
        p.color = (0xFF shl 24) or border
        c.drawRoundRect(RectF(0f, 0f, wf, hf), hf * 0.16f, hf * 0.16f, p)
        val m = hf * 0.07f
        p.color = (0xFF shl 24) or bg
        c.drawRoundRect(RectF(m, m, wf - m, hf - m), hf * 0.12f, hf * 0.12f, p)
        p.color = (0xFF shl 24) or fg
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        p.setShadowLayer(hf * 0.02f, 0f, hf * 0.025f, 0x88000000.toInt())
        fun fit(t: String, base: Float): Float {
            p.textSize = base
            val tw = p.measureText(t)
            return if (tw > wf * 0.84f) base * wf * 0.84f / tw else base
        }
        if (text2 == null) {
            val s = fit(text1, hf * 0.62f)
            p.textSize = s
            c.drawText(text1, wf / 2f, hf / 2f + s * 0.35f, p)
        } else {
            val s1 = fit(text1, hf * 0.40f)
            p.textSize = s1
            c.drawText(text1, wf / 2f, hf * 0.45f, p)
            val s2 = fit(text2, hf * 0.30f)
            p.textSize = s2
            c.drawText(text2, wf / 2f, hf * 0.78f, p)
        }

        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        tex = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        bmp.recycle()

        val verts = floatArrayOf(
            -0.5f, -0.5f, 0f, 0f, 1f,
            0.5f, -0.5f, 0f, 1f, 1f,
            0.5f, 0.5f, 0f, 1f, 0f,
            -0.5f, 0.5f, 0f, 0f, 0f
        )
        vb = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply { put(verts); position(0) }
        val idx = shortArrayOf(0, 1, 2, 0, 2, 3)
        ib = ByteBuffer.allocateDirect(idx.size * 2).order(ByteOrder.nativeOrder())
            .asShortBuffer().apply { put(idx); position(0) }
    }

    fun draw(posLoc: Int, uvLoc: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        vb.position(0)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 20, vb)
        GLES20.glEnableVertexAttribArray(posLoc)
        vb.position(3)
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, 20, vb)
        GLES20.glEnableVertexAttribArray(uvLoc)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, ib)
    }
}
