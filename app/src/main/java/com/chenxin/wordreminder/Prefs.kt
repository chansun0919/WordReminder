package com.chenxin.wordreminder

import android.content.Context
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

    /** 累计复习次数（每次点「已背」+1）。 */
    fun bumpTotal() {
        total += 1
    }

    /** 点击「已背」时更新连续天数：当天首次复习才更新，断了从 1 重算。 */
    fun checkIn() {
        val today = LocalDate.now().toString()
        if (lastCheckIn == today) return
        val yesterday = LocalDate.now().minusDays(1).toString()
        streak = if (lastCheckIn == yesterday) streak + 1 else 1
        lastCheckIn = today
    }

    // ---- 间隔复习：每个单词的熟练等级(level 1..6)与到期日(epoch day) ----
    private fun todayEpoch() = LocalDate.now().toEpochDay()

    private val reviews: JSONObject
        get() {
            val s = sp.getString("reviews", "") ?: ""
            return if (s.isBlank()) JSONObject() else JSONObject(s)
        }

    /** 熟练等级，默认 1。 */
    fun boxOf(i: Int): Int {
        val o = reviews.optJSONObject(i.toString()) ?: return 1
        return o.optInt("b", 1)
    }

    /** 到期日（epoch day）。新词按下标错开：第 i 个词 i 天后首次到期。 */
    fun dueOf(i: Int): Long {
        val o = reviews.optJSONObject(i.toString())
        if (o != null) return o.optLong("d", todayEpoch())
        return todayEpoch() + i
    }

    /** 保存一次复习结果。 */
    fun saveReview(i: Int, level: Int, dueDay: Long) {
        val r = reviews
        r.put(i.toString(), JSONObject().put("b", level).put("d", dueDay))
        sp.edit().putString("reviews", r.toString()).apply()
    }
}
