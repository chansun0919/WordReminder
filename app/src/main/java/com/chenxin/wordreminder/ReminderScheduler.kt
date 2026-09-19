package com.chenxin.wordreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    /**
     * 设置每天 hour:minute 准时响一次（息屏/省电也生效），自动跳过今天已过的时刻。
     *
     * 关键：Android 12+ 用 setExactAndAllowWhileIdle 必须拿到 SCHEDULE_EXACT_ALARM 运行时授权，
     * 否则会抛 SecurityException 直接崩。这里先判断：没授权就降级为普通 set()（仍能每天响，
     * 仅不保证秒级精准），并把整个调用包起来，任何异常都不让 App 启动崩溃。
     */
    fun schedule(context: Context, hour: Int, minute: Int) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }

            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.canScheduleExactAlarms()
            } else {
                true
            }

            if (canExact) {
                try {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                } catch (se: SecurityException) {
                    // 极端情况下仍被拒，降级普通闹钟
                    am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                }
            } else {
                // 未授予精确闹钟权限：降级为普通闹钟，保证每天仍能响
                am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (t: Throwable) {
            // 任何异常都不应让 App 启动崩溃
            t.printStackTrace()
        }
    }

    fun cancel(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
