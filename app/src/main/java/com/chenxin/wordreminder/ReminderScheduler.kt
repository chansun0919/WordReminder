package com.chenxin.wordreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderScheduler {

    /**
     * 用 setAlarmClock 而非 setExactAndAllowWhileIdle：
     * - 不受 Doze / 国产系统省电策略延迟，息屏、锁屏、被杀后台都能准时响；
     * - 状态栏会显示一个闹钟图标，用户一眼可见，国产 ROM（OPPO/vivo）也拦不住它；
     * - 不需要 SCHEDULE_EXACT_ALARM 运行时权限，绕开授权弹窗与降级坑。
     * 每天 hour:minute 响一次，自动跳过今天已过的时刻。
     */
    fun schedule(context: Context, hour: Int, minute: Int) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val show = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
            am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, show), pi)
        } catch (t: Throwable) {
            // 任何异常都不应让 App 启动崩溃
            t.printStackTrace()
        }
    }

    /** 测试用：delayMillis 毫秒后触发一次（不受时段限制），用于验证提醒是否弹出。 */
    fun scheduleTest(context: Context, delayMillis: Long = 60_000) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context, 1,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val show = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(System.currentTimeMillis() + delayMillis, show), pi)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
