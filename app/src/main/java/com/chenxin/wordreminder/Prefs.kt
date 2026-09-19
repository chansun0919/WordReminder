package com.chenxin.wordreminder

import android.content.Context
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

    /** 点击「已背」时更新连续天数：断了就从 1 重算。 */
    fun checkIn() {
        val today = LocalDate.now().toString()
        if (lastCheckIn == today) return
        val yesterday = LocalDate.now().minusDays(1).toString()
        streak = if (lastCheckIn == yesterday) streak + 1 else 1
        total += 1
        lastCheckIn = today
    }
}
