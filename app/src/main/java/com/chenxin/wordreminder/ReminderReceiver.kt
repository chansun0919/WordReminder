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
        val prefs = Prefs(context)
        val queue = buildQueue(words, prefs)
        val nm = context.getSystemService(NotificationManager::class.java)

        // 全部背完就不打扰
        if (queue.isEmpty()) {
            ReminderScheduler.schedule(context, prefs.hour, prefs.minute)
            return
        }

        val first = words[queue.first()]
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("📖 今日背单词（${queue.size} 个）：${first.word}")
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
        ReminderScheduler.schedule(context, prefs.hour, prefs.minute)
    }

    /** 未背过的前 20 个（从词库开头 = 初一单词）。 */
    private fun buildQueue(words: List<Word>, prefs: Prefs): List<Int> {
        val out = mutableListOf<Int>()
        for (i in words.indices) {
            if (!prefs.isLearned(i)) {
                out.add(i)
                if (out.size >= 20) break
            }
        }
        return out
    }
}
