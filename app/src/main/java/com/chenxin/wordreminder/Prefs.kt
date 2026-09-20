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

    /** 累计「已背/已复习」次数（每次点按钮 +1），用于每 10 个表扬与「累计 X 个」。 */
    var total: Int
        get() = sp.getInt("total", 0)
        set(v) = sp.edit().putInt("total", v).apply()

    var lastCheckIn: String
        get() = sp.getString("last_checkin", "") ?: ""
        set(v) = sp.edit().putString("last_checkin", v).apply()

    /** 连续 21 天奖励是否已弹出（断了连续天数会重置）。 */
    var reward21Shown: Boolean
        get() = sp.getBoolean("reward21", false)
        set(v) = sp.edit().putBoolean("reward21", v).apply()

    /** 上次表扬对应的里程碑（10 的倍数），避免重复弹。 */
    var lastPraiseMilestone: Int
        get() = sp.getInt("last_praise", 0)
        set(v) = sp.edit().putInt("last_praise", v).apply()

    var praiseDate: String
        get() = sp.getString("praise_date", "") ?: ""
        set(v) = sp.edit().putString("praise_date", v).apply()

    fun bumpTotal() { total += 1 }

    /**
     * 连续天数：当天首次才更新；断了从 1 重算，并重置 21 天奖励标记。
     * 调用前应先 recordToday()/markLearned()。
     */
    fun checkIn() {
        val today = LocalDate.now().toString()
        if (lastCheckIn == today) return
        val yesterday = LocalDate.now().minusDays(1).toString()
        if (lastCheckIn == yesterday) {
            streak += 1
        } else {
            streak = 1
            reward21Shown = false
        }
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

    // ---- 今天新词计数（按日期自动归零） ----
    fun todayNewCount(): Int {
        if ((sp.getString("today_new_date", "") ?: "") != LocalDate.now().toString()) return 0
        return sp.getInt("today_new_count", 0)
    }

    fun bumpTodayNew(): Int {
        val today = LocalDate.now().toString()
        val c = if ((sp.getString("today_new_date", "") ?: "") == today)
            sp.getInt("today_new_count", 0) + 1 else 1
        sp.edit().putString("today_new_date", today).putInt("today_new_count", c).apply()
        return c
    }

    // ---- 表扬句子（每天联网拉一次，缓存到本地） ----
    val praiseList: List<String>
        get() {
            val s = sp.getString("praise_list", "") ?: ""
            if (s.isBlank()) return emptyList()
            val arr = JSONArray(s)
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) out.add(arr.getString(i))
            return out
        }

    fun setPraise(list: List<String>, date: String) {
        sp.edit().putString("praise_list", JSONArray(list).toString())
            .putString("praise_date", date).apply()
    }

    // ---- 每日背诵历史：date -> 数量，用于近14天统计 ----
    private val historyObj: JSONObject
        get() {
            val s = sp.getString("history", "") ?: ""
            return if (s.isBlank()) JSONObject() else JSONObject(s)
        }

    /** 记录今天又背/复习了 1 个词（每次点按钮都计，含复习）。 */
    fun recordToday() {
        val today = LocalDate.now().toString()
        val h = historyObj
        h.put(today, h.optInt(today, 0) + 1)
        sp.edit().putString("history", h.toString()).apply()
    }

    /** 今天已背/复习数量（含复习）。 */
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
