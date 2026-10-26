package com.fish.pet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.min
import kotlin.math.sin

class WhaleImageView(c: Context) : View(c) {

    var facing = 1f
    private var t = 0f
    private val bmp: Bitmap? = BitmapFactory.decodeResource(c.resources, R.drawable.whale)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val tick = object : Runnable {
        override fun run() {
            t += 0.1f
            invalidate()
            postDelayed(this, 40)
        }
    }

    init {
        post(tick)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val b = bmp ?: return

        canvas.save()
        if (facing < 0f) canvas.scale(-1f, 1f, w / 2f, h / 2f)

        val bob = sin(t) * h * 0.025f
        val tilt = sin(t * 0.8f) * 3f
        canvas.rotate(tilt, w / 2f, h / 2f + bob)

        val scale = min(w / b.width.toFloat(), h / b.height.toFloat())
        val dw = b.width * scale
        val dh = b.height * scale
        val left = (w - dw) / 2f
        val top = (h - dh) / 2f + bob
        canvas.drawBitmap(b, null, RectF(left, top, left + dw, top + dh), paint)

        canvas.restore()
    }
}
