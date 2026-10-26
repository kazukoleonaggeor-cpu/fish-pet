package com.fish.pet

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class ChatActivity : android.app.Activity() {

    private lateinit var log: TextView
    private lateinit var input: EditText
    private lateinit var send: Button
    private lateinit var scroll: ScrollView
    private val history = JSONArray()
    private val sb = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(0xF0000000.toInt()))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 140, 50, 50)
        }

        log = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            text = "对我说点什么吧"
            setPadding(0, 0, 0, 40)
        }

        scroll = ScrollView(this)
        scroll.addView(log)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        input = EditText(this).apply {
            hint = "输入…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        root.addView(input)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        send = Button(this).apply {
            text = "发送"
            setOnClickListener { doSend() }
        }
        row.addView(send, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        row.addView(Button(this).apply {
            text = "关闭"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        root.addView(row)

        setContentView(root)
    }

    private fun doSend() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        val key = getSharedPreferences("pet", MODE_PRIVATE)
            .getString("api_key", "").orEmpty()
        if (key.isBlank()) {
            append("（还没填 Key，先回设置页填一下）")
            return
        }

        input.setText("")
        append("我：" + text)
        send.isEnabled = false
        append("大肥鱼正在想…")

        Thread {
            val reply = DeepSeekApi.chat(key, history, text)
            runOnUiThread {
                val s = sb.toString()
                val idx = s.lastIndexOf("大肥鱼正在想…")
                if (idx >= 0) {
                    sb.setLength(idx)
                    log.text = sb.toString()
                }
                append("大肥鱼：" + reply)
                history.put(JSONObject().put("role", "user").put("content", text))
                history.put(JSONObject().put("role", "assistant").put("content", reply))
                send.isEnabled = true
            }
        }.start()
    }

    private fun append(line: String) {
        sb.append(line).append("\n\n")
        log.text = sb.toString()
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
