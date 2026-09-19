package com.chenxin.wordreminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val words = WordRepository.all(context)
        val due = Srs.dueIndices(context, words.size)
        val nm = context.getSystemService(NotificationManager::class.java)

        // 今天没有到期单词就不打扰（说明都复习完了）
        if (due.isEmpty()) {
            ReminderScheduler.schedule(context, Prefs(context).hour, Prefs(context).minute)
            return
        }

        val first = words[due.first()]
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("📖 今日复习（${due.size} 词）：${first.word}")
            .setContentText(first.meaning)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${first.meaning}\n\n${first.example ?: ""}")
            )
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(1, n)

        // 排好明天的提醒（闹钟在息屏/省电下依然生效）
        val prefs = Prefs(context)
        ReminderScheduler.schedule(context, prefs.hour, prefs.minute)
    }
}
