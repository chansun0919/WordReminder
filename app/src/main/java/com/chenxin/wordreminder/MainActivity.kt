package com.chenxin.wordreminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chenxin.wordreminder.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { Prefs(this) }

    // 今天到期要复习的单词下标队列
    private var dueList: List<Int> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureChannel()
        requestNotifyPermission()
        ReminderScheduler.schedule(this, prefs.hour, prefs.minute)

        showDue()
        binding.btnDone.setOnClickListener { markDone() }
        binding.btnSettings.setOnClickListener { openTimePicker() }
    }

    /** 重新计算今天到期的单词并展示第一个。 */
    private fun showDue() {
        val words = WordRepository.all(this)
        dueList = Srs.dueIndices(this, words.size)

        if (dueList.isEmpty()) {
            binding.tvWord.text = "今日复习完成 🎉"
            binding.tvPhonetic.text = ""
            binding.tvMeaning.text = "今天的单词都复习过啦，明天见。"
            binding.tvExample.text = ""
            binding.btnDone.isEnabled = false
            binding.btnDone.text = "已背 ✓"
            binding.tvQueue.text = "今天没有待复习的单词"
        } else {
            renderCurrent(words)
        }

        binding.tvStreak.text = "连续 ${prefs.streak} 天 · 累计复习 ${prefs.total} 次"
        binding.tvDate.text = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日"))
    }

    private fun renderCurrent(words: List<Word>) {
        val idx = dueList[0]
        val w = words[idx]
        binding.tvWord.text = w.word
        binding.tvPhonetic.text = w.phonetic ?: ""
        binding.tvMeaning.text = w.meaning
        binding.tvExample.text = w.example ?: ""
        binding.btnDone.isEnabled = true
        binding.btnDone.text = "已背 ✓"
        binding.tvQueue.text = "今日待复习 ${dueList.size} 个 · 熟练等级 ${Srs.levelOf(this, idx)}"
    }

    private fun markDone() {
        if (dueList.isEmpty()) return
        val idx = dueList[0]
        Srs.review(this, idx)   // 升级并推后下次复习
        prefs.bumpTotal()       // 累计复习次数 +1
        prefs.checkIn()         // 当天首次复习时更新连续天数
        showDue()
    }

    private fun openTimePicker() {
        val dp = android.app.TimePickerDialog(this, { _, h, m ->
            prefs.hour = h
            prefs.minute = m
            ReminderScheduler.schedule(this, h, m)
            showDue()
            android.widget.Toast.makeText(
                this, "已设为 ${String.format("%02d:%02d", h, m)} 提醒",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }, prefs.hour, prefs.minute, true)
        dp.show()
    }

    private fun requestNotifyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID, "背单词提醒", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "每日背单词" }
            nm.createNotificationChannel(ch)
        }
    }

    companion object {
        const val CHANNEL_ID = "word_reminder"
    }
}
