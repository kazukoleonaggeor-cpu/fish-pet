package com.fish.pet

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DeepSeekApi {

    private const val BASE = "https://api.deepseek.com"

    fun balance(key: String): String {
        return try {
            val conn = URL("$BASE/user/balance").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $key")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""

            if (code != 200) return "查询失败 (HTTP $code)：$body"

            val json = JSONObject(body)
            val arr = json.optJSONArray("balance_infos")
            if (arr == null || arr.length() == 0) return "没有余额信息"

            val sb = StringBuilder()
            for (i in 0 until arr.length()) {
                val b = arr.getJSONObject(i)
                if (i > 0) sb.append("\n")
                sb.append(b.optString("currency"))
                sb.append("  ")
                sb.append(b.optString("total_balance"))
            }
            "余额：\n$sb"
        } catch (e: Exception) {
            "查询出错：${e.message}"
        }
    }

    fun chat(key: String, history: JSONArray, userText: String): String {
        return try {
            val messages = JSONArray()
            for (i in 0 until history.length()) messages.put(history.get(i))
            messages.put(JSONObject().put("role", "user").put("content", userText))

            val payload = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", messages)
                put("stream", false)
            }

            val conn = URL("$BASE/chat/completions").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $key")
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }

            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""

            if (code != 200) return "出错了 (HTTP $code)：$body"

            JSONObject(body)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            "连接失败：${e.message}"
        }
    }
}
