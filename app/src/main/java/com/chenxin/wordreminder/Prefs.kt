package com.chenxin.wordreminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("wr_prefs", Context.MODE_PRIVATE)

    var hour: Int
        get() = sp.getInt("hour", 9)
        set(v) = sp.edit().putInt("hour", v).apply()

    var minute: Int
        get() = sp.getInt("minute", 0)
        set(v) = sp.edit().putInt("minute", v).apply()

    var streak: Int
        get() = sp.getInt("streak", 0)
        set(v) = sp.edit().putInt("streak", v).apply()

    var total: Int
        get() = sp.getInt("total", 0)
        set(v) = sp.edit().putInt("total", v).apply()

    var lastCheckIn: String
        get() = sp.getString("last_checkin", "") ?: ""
        set(v) = sp.edit().putString("last_checkin", v).apply()

    /** 累计「已背」次数（每次点已背 +1）。 */
    fun bumpTotal() {
        total += 1
    }

    /** 点「已背」时更新连续天数：当天首次才更新，断了从 1 重算。 */
    fun checkIn() {
        val today = LocalDate.now().toString()
        if (lastCheckIn == today) return
        val yesterday = LocalDate.now().minusDays(1).toString()
        streak = if (lastCheckIn == yesterday) streak + 1 else 1
        lastCheckIn = today
    }

    // ---- 已背诵单词集合（下标） ----
    private val learnedSet: MutableSet<Int>
        get() {
            val s = sp.getString("learned", "") ?: ""
            val set = mutableSetOf<Int>()
            if (s.isBlank()) return set
            val arr = JSONArray(s)
            for (i in 0 until arr.length()) set.add(arr.getInt(i))
            return set
        }

    fun isLearned(i: Int): Boolean = learnedSet.contains(i)

    fun markLearned(i: Int) {
        val set = learnedSet
        if (set.add(i)) {
            sp.edit().putString("learned", JSONArray(set.sorted()).toString()).apply()
        }
    }

    fun learnedCount(): Int = learnedSet.size

    fun learnedIndices(): List<Int> = learnedSet.sorted()

    // ---- 每日背诵历史：date -> 数量，用于近14天统计 ----
    private val historyObj: JSONObject
        get() {
            val s = sp.getString("history", "") ?: ""
            return if (s.isBlank()) JSONObject() else JSONObject(s)
        }

    /** 记录今天新背了 1 个词（首次背才计）。 */
    fun recordToday() {
        val today = LocalDate.now().toString()
        val h = historyObj
        h.put(today, h.optInt(today, 0) + 1)
        sp.edit().putString("history", h.toString()).apply()
    }

    /** 今天已背数量。 */
    fun todayCount(): Int = historyObj.optInt(LocalDate.now().toString(), 0)

    /** 最近 14 天 [date, count]，从旧到新。 */
    fun historyLast14(): List<Pair<String, Int>> {
        val h = historyObj
        val today = LocalDate.now()
        val out = mutableListOf<Pair<String, Int>>()
        for (d in 13 downTo 0) {
            val date = today.minusDays(d.toLong())
            out.add(date.toString() to h.optInt(date.toString(), 0))
        }
        return out
    }
}
