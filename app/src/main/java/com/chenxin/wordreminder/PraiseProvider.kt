package com.chenxin.wordreminder

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * 表扬句子来源：每天联网拉一批（默认 Hitokoto 一言），缓存到本地；
 * 联网失败则用内置鼓励语兜底，保证一定能表扬。
 */
object PraiseProvider {

    private val BUILTIN = listOf(
        "太棒了！又背了 10 个，你真的在进步！💪",
        "厉害！每个单词都是你变强的证据！🌟",
        "坚持就是胜利，今天的你比昨天更强！🔥",
        "了不起！你的大脑又装进了新武器！🧠",
        "完美！离目标稳稳推进，继续！🎯",
        "真棒！自律的人最迷人！😊",
        "优秀！每天进步一点点，复利惊人！🌈",
        "牛！连续打卡，习惯正在养成！🏆"
    )

    /** 打开 App 时调用：今天还没拉过就联网拉一批（失败回退内置）。 */
    fun ensureToday(context: Context) {
        val prefs = Prefs(context)
        val today = LocalDate.now().toString()
        if (prefs.praiseDate == today && prefs.praiseList.isNotEmpty()) return
        Thread {
            try {
                val list = mutableListOf<String>()
                repeat(8) { fetchOne()?.let { list.add(it) } }
                prefs.setPraise(if (list.size >= 3) list else BUILTIN, today)
            } catch (_: Throwable) {
                prefs.setPraise(BUILTIN, today)
            }
        }.start()
    }

    private fun fetchOne(): String? = try {
        val conn = URL("https://v1.hitokoto.cn/?encode=text").openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "GET"
        if (conn.responseCode == 200) {
            conn.inputStream.bufferedReader().use { it.readText().trim() }
                .takeIf { it.isNotBlank() }
        } else null
    } catch (_: Throwable) { null }

    /** 第 milestone/10 句表扬（循环取）。 */
    fun praiseFor(prefs: Prefs, milestone: Int): String {
        val pool = if (prefs.praiseList.isNotEmpty()) prefs.praiseList else BUILTIN
        return pool[milestone % pool.size]
    }
}
