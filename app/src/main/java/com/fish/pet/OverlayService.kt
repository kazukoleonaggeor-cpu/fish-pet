package com.fish.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var whale: WhaleView
    private lateinit var lp: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())

    private var screenW = 0
    private var screenH = 0
    private var targetX = 100
    private var targetY = 800
    private var fx = 100f
    private var fy = 800f
    private var dragging = false
    private var downAt = 0L
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0

    private val roam = object : Runnable {
        override fun run() {
            if (!dragging) {
                val maxX = (screenW - whale.width).coerceAtLeast(1)
                val minY = screenH / 3
                val maxY = (screenH - whale.height - 80).coerceAtLeast(minY + 1)
                targetX = Random.nextInt(0, maxX)
                targetY = Random.nextInt(minY, maxY)
            }
            handler.postDelayed(this, Random.nextLong(3500, 7000))
        }
    }

    private val move = object : Runnable {
        override fun run() {
            if (!dragging) {
                val dx = (targetX - lp.x).toDouble()
                val dy = (targetY - lp.y).toDouble()
                val dist = hypot(dx, dy)
                if (dist > 6.0) {
                    fx += (dx / dist * 5.0).toFloat()
                    fy += (dy / dist * 5.0).toFloat()
                    lp.x = fx.toInt()
                    lp.y = fy.toInt()
                    if (abs(dx) > 1.0) whale.facing = if (dx < 0) -1f else 1f
                    try {
                        wm.updateViewLayout(whale, lp)
                    } catch (e: Exception) {
                    }
                }
            }
            handler.postDelayed(this, 40)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val m = resources.displayMetrics
        screenW = m.widthPixels
        screenH = m.heightPixels

        startForegroundCompat()

        whale = WhaleView(this)
        lp = WindowManager.LayoutParams(
            240, 240,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 150
            y = screenH / 2
        }
        fx = lp.x.toFloat()
        fy = lp.y.toFloat()
        targetX = lp.x
        targetY = lp.y

        whale.setOnTouchListener { _, e -> onTouch(e) }
        wm.addView(whale, lp)

        handler.post(roam)
        handler.post(move)
    }

    private fun onTouch(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                downAt = System.currentTimeMillis()
                downX = e.rawX
                downY = e.rawY
                startX = lp.x
                startY = lp.y
            }
            MotionEvent.ACTION_MOVE -> {
                lp.x = startX + (e.rawX - downX).toInt()
                lp.y = startY + (e.rawY - downY).toInt()
                fx = lp.x.toFloat()
                fy = lp.y.toFloat()
                try {
                    wm.updateViewLayout(whale, lp)
                } catch (ex: Exception) {
                }
            }
            MotionEvent.ACTION_UP -> {
                dragging = false
                val moved = abs(e.rawX - downX) > 20f || abs(e.rawY - downY) > 20f
                val held = System.currentTimeMillis() - downAt
                if (!moved) {
                    if (held > 700) showBalance() else openChat()
                } else {
                    targetX = lp.x
                    targetY = lp.y
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
            }
        }
        return true
    }

    private fun openChat() {
        val i = Intent(this, ChatActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    private fun showBalance() {
        val key = getSharedPreferences("pet", MODE_PRIVATE)
            .getString("api_key", "").orEmpty()
        if (key.isBlank()) {
            toast("先在 App 里填 Key")
            return
        }
        toast("查询中…")
        Thread {
            val r = DeepSeekApi.balance(key)
            handler.post { toast(r) }
        }.start()
    }

    private fun toast(s: String) {
        val safe = if (s.length > 300) s.substring(0, 300) else s
        Toast.makeText(this, safe, Toast.LENGTH_LONG).show()
    }

    private fun startForegroundCompat() {
        val ch = NotificationChannel(
            "pet", "大肥鱼", NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)

        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, "pet")
            .setContentTitle("大肥鱼在游")
            .setContentText("点它聊天，长按看余额")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            wm.removeView(whale)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class WhaleView(c: Context) : View(c) {

    var facing = 1f
    private var t = 0f

    private val tick = object : Runnable {
        override fun run() {
            t += 0.12f
            invalidate()
            postDelayed(this, 40)
        }
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D6BFE")
    }
    private val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2F4BD8")
    }
    private val bellyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DCE4FF")
    }
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B1B")
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B1B")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    init {
        post(tick)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.save()
        if (facing < 0f) canvas.scale(-1f, 1f, w / 2f, h / 2f)

        val bob = sin(t) * h * 0.03f
        val cx = w * 0.53f
        val cy = h * 0.50f + bob
        val rw = w * 0.30f
        val rh = h * 0.24f

        val tailAngle = sin(t * 1.3f) * 18f
        canvas.save()
        canvas.rotate(-tailAngle, cx - rw * 0.9f, cy)
        val tail = Path()
        tail.moveTo(cx - rw * 0.85f, cy)
        tail.quadTo(cx - rw * 1.25f, cy - rh * 0.9f, cx - rw * 1.45f, cy - rh * 0.7f)
        tail.quadTo(cx - rw * 1.15f, cy, cx - rw * 1.45f, cy + rh * 0.7f)
        tail.quadTo(cx - rw * 1.25f, cy + rh * 0.9f, cx - rw * 0.85f, cy)
        tail.close()
        canvas.drawPath(tail, bodyPaint)
        canvas.restore()

        val rect = RectF(cx - rw, cy - rh, cx + rw, cy + rh)
        val bodyPath = Path().apply {addOval(rect, Path.Direction.CW) }
        canvas.drawPath(bodyPath, bodyPaint)

        canvas.save()
        canvas.clipPath(bodyPath)
        canvas.drawOval(
            RectF(cx - rw * 0.5f, cy + rh * 0.2f, cx + rw * 0.85f, cy + rh * 1.1f),
            bellyPaint
        )
        canvas.restore()

        val fin = Path()
        fin.moveTo(cx - rw * 0.15f, cy + rh * 0.55f)
        fin.quadTo(cx - rw * 0.1f, cy + rh * 1.3f, cx + rw * 0.35f, cy + rh * 0.7f)
        fin.close()
        canvas.drawPath(fin, darkPaint)

        val ex = cx + rw * 0.45f
        val ey = cy - rh * 0.28f
        canvas.drawCircle(ex, ey, rh * 0.26f, whitePaint)
        canvas.drawCircle(ex + rh * 0.04f, ey, rh * 0.14f, blackPaint)

        canvas.drawArc(
            RectF(cx + rw * 0.28f, cy - rh * 0.05f, cx + rw * 0.68f, cy + rh * 0.35f),
            20f, 110f, false, linePaint
        )

        canvas.restore()
    }
}
