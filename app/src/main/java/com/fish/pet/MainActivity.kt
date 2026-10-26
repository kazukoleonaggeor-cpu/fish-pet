package com.fish.pet

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : android.app.Activity() {

    private lateinit var keyInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("pet", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 140, 60, 60)
            setBackgroundColor(Color.WHITE)
        }

        root.addView(TextView(this).apply {
            text = "大肥鱼"
            textSize = 28f
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "把你的 DeepSeek API Key 贴在下面"
            textSize = 14f
            setPadding(0, 50, 0, 10)
        })

        keyInput = EditText(this).apply {
            hint = "sk-..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("api_key", ""))
        }
        root.addView(keyInput)

        root.addView(Button(this).apply {
            text = "保存 Key"
            setOnClickListener {
                prefs.edit().putString("api_key", keyInput.text.toString().trim()).apply()
                Toast.makeText(this@MainActivity, "已保存", Toast.LENGTH_SHORT).show()
            }
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 30, 0, 30)
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "查余额"
            setOnClickListener { checkBalance(prefs.getString("api_key", "").orEmpty()) }
        })

        root.addView(Button(this).apply {
            text = "放出大肥鱼"
            setOnClickListener { startPet() }
        })

        root.addView(Button(this).apply {
            text = "收回大肥鱼"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(this@MainActivity, "收回了", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(root)
    }

    private fun startPet() {
        val key = getSharedPreferences("pet", MODE_PRIVATE)
            .getString("api_key", "").orEmpty()
        if (key.isBlank()) {
            Toast.makeText(this, "先填 API Key 并保存", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请允许悬浮窗，然后回来再点一次", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        Toast.makeText(this, "大肥鱼出发了", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }

    private fun checkBalance(key: String) {
        if (key.isBlank()) {
            status.text = "先填 Key 并保存"
            return
        }
        status.text = "查询中…"
        Thread {
            val r = DeepSeekApi.balance(key)
            runOnUiThread { status.text = r }
        }.start()
    }
}
