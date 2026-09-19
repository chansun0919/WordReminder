package com.chenxin.wordreminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val word = WordRepository.todayWord(context)
        val nm = context.getSystemService(NotificationManager::class.java)

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("📖 今日单词：${word.word}")
            .setContentText(word.meaning)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${word.meaning}\n\n${word.example ?: ""}")
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
