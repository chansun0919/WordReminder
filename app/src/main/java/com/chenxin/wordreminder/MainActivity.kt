package com.chenxin.wordreminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chenxin.wordreminder.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { Prefs(this) }

    // 今天要背的单词队列：未背过的最多 10 个，按词库顺序从前往后（初一单词在最前）
    private var queue: List<Int> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureChannel()
        requestNotifyPermission()
        ReminderScheduler.schedule(this, prefs.hour, prefs.minute)

        showQueue()
        binding.btnDone.setOnClickListener { markDone() }
        binding.btnSettings.setOnClickListener { openTimePicker() }
        binding.btnStats.setOnClickListener { startActivity(Intent(this, StatsActivity::class.java)) }
        binding.btnLearned.setOnClickListener { startActivity(Intent(this, LearnedActivity::class.java)) }

        ensureExactAlarmPermission()
    }

    /** 重新计算今天要背的单词队列（未背过的最多 10 个）并展示第一个。 */
    private fun showQueue() {
        try {
            val words = WordRepository.all(this)
            queue = buildQueue(words)

            if (queue.isEmpty()) {
                binding.tvWord.text = "全部背完啦 🎉"
                binding.tvPhonetic.text = ""
                binding.tvMeaning.text = "词库里的单词都背过一遍了，可以去「已背单词」复习。"
                binding.tvExample.text = ""
                binding.btnDone.isEnabled = false
                binding.btnDone.text = "已背 ✓"
                binding.tvQueue.text = "已背 ${prefs.learnedCount()} / ${words.size} 个"
            } else {
                renderCurrent(words)
            }

            binding.tvStreak.text = "连续 ${prefs.streak} 天 · 累计 ${prefs.total} 个 · 今天 ${prefs.todayCount()} 个"
            binding.tvDate.text = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日"))
        } catch (t: Throwable) {
            t.printStackTrace()
            binding.tvWord.text = "加载出错"
            binding.tvPhonetic.text = ""
            binding.tvMeaning.text = "词库加载失败：${t.message}"
            binding.tvExample.text = ""
            binding.btnDone.isEnabled = false
            binding.tvQueue.text = "请重新安装或联系开发者"
        }
    }

    /** 取未背过的前 10 个（保持词库顺序，从第一个开始 = 初一单词）。 */
    private fun buildQueue(words: List<Word>): List<Int> {
        val out = mutableListOf<Int>()
        for (i in words.indices) {
            if (!prefs.isLearned(i)) {
                out.add(i)
                if (out.size >= 10) break
            }
        }
        return out
    }

    private fun renderCurrent(words: List<Word>) {
        val idx = queue[0]
        val w = words[idx]
        binding.tvWord.text = w.word
        binding.tvPhonetic.text = w.phonetic ?: ""
        binding.tvMeaning.text = w.meaning
        binding.tvExample.text = w.example ?: ""
        binding.btnDone.isEnabled = true
        binding.btnDone.text = "已背 ✓"
        val done = prefs.learnedCount()
        binding.tvQueue.text = "本次 ${queue.size} 个待背 · 进度 $done / ${words.size}"
    }

    private fun markDone() {
        if (queue.isEmpty()) return
        val idx = queue[0]
        if (!prefs.isLearned(idx)) {
            prefs.markLearned(idx)
            prefs.recordToday()
            prefs.bumpTotal()
            prefs.checkIn()
        }
        showQueue()
    }

    private fun openTimePicker() {
        val dp = android.app.TimePickerDialog(this, { _, h, m ->
            prefs.hour = h
            prefs.minute = m
            ReminderScheduler.schedule(this, h, m)
            showQueue()
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

    /**
     * Android 12+ 精确闹钟权限默认可能不授予侧载应用，导致提醒不精准。
     * 未授予时弹窗引导用户去设置里开启。
     */
    private fun ensureExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("开启精准提醒")
                    .setMessage("为保证每天准时提醒，请在设置里允许本应用的「闹钟和提醒（精确闹钟）」权限。")
                    .setPositiveButton("去设置") { _, _ ->
                        try {
                            startActivity(
                                Intent("android.app.action.REQUEST_SCHEDULE_EXACT_ALARM")
                                    .setData(Uri.parse("package:$packageName"))
                            )
                        } catch (_: Exception) { /* 部分 ROM 无此页，忽略 */ }
                    }
                    .setNegativeButton("稍后", null)
                    .show()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "word_reminder"
    }
}
