package com.chenxin.wordreminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
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

    /** 每天最多背的新词数量。 */
    private val DAILY_NEW_GOAL = 20

    private var mode = "new"            // "new" | "review"
    private var currentIndex = -1       // 当前展示词的词库下标（新词模式）
    private var reviewList: List<Int> = emptyList()
    private var reviewPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureChannel()
        requestNotifyPermission()
        ReminderScheduler.schedule(this, prefs.hour, prefs.minute)
        PraiseProvider.ensureToday(this)

        binding.btnDone.setOnClickListener { markDone() }
        binding.btnSettings.setOnClickListener { openTimePicker() }
        binding.btnStats.setOnClickListener { startActivity(Intent(this, StatsActivity::class.java)) }
        binding.btnLearned.setOnClickListener { startActivity(Intent(this, LearnedActivity::class.java)) }
        binding.btnExit.setOnClickListener { finish() }
        binding.btnTest.setOnClickListener { testReminder() }
        binding.btnFix.setOnClickListener { showFixGuide() }

        refresh()
    }

    /** 根据当前模式决定展示什么。 */
    private fun refresh() {
        val words = WordRepository.all(this)
        if (mode == "review") {
            if (reviewList.isEmpty()) startReview(words)
            showReview(words)
            return
        }
        // 新词模式
        if (prefs.todayNewCount() >= DAILY_NEW_GOAL) {
            showDailyDoneDialog("今日新词已背满 $DAILY_NEW_GOAL 个 🎉")
            return
        }
        val idx = nextNewIndex(words)
        if (idx == null) {
            showDailyDoneDialog("词库里的新词都背完啦 🎉")
            return
        }
        currentIndex = idx
        renderWord(words[idx], "新词")
        updateHeader(words)
    }

    private fun nextNewIndex(words: List<Word>): Int? {
        for (i in words.indices) if (!prefs.isLearned(i)) return i
        return null
    }

    private fun renderWord(w: Word, tag: String) {
        binding.tvWord.text = w.word
        binding.tvPhonetic.text = w.phonetic ?: ""
        binding.tvMeaning.text = w.meaning
        binding.tvExample.text = w.example ?: ""
        binding.btnDone.isEnabled = true
        binding.btnDone.text = if (tag == "新词") "已背 ✓" else "已复习 ✓"
    }

    private fun updateHeader(words: List<Word>) {
        val done = prefs.learnedCount()
        val newToday = prefs.todayNewCount()
        binding.tvQueue.text = "新词今日 $newToday / $DAILY_NEW_GOAL · 进度 $done / ${words.size}"
        binding.tvStreak.text = "连续 ${prefs.streak} 天 · 累计 ${prefs.total} 个 · 今天 ${prefs.todayCount()} 个"
        binding.tvDate.text = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日"))
        binding.btnExit.visibility = View.GONE
    }

    private fun markDone() {
        if (mode == "new" && currentIndex < 0) return
        if (mode == "new") {
            if (!prefs.isLearned(currentIndex)) {
                prefs.markLearned(currentIndex)
                prefs.bumpTodayNew()
                prefs.recordToday()
            }
            prefs.bumpTotal()
            prefs.checkIn()
        } else {
            // 复习模式：不计入新词，但计入累计与连续天数
            prefs.bumpTotal()
            prefs.recordToday()
            prefs.checkIn()
            reviewPos++
        }
        onAfterMemorize()
        refresh()
    }

    /** 背满 10 个表扬一句；连续 21 天弹奖励。 */
    private fun onAfterMemorize() {
        // 21 天连续奖励优先
        if (prefs.streak >= 21 && !prefs.reward21Shown) {
            prefs.reward21Shown = true
            showRewardDialog()
            return
        }
        // 每背满 10 个表扬一句
        val total = prefs.total
        val milestone = (total / 10) * 10
        if (total >= 10 && milestone > prefs.lastPraiseMilestone) {
            prefs.lastPraiseMilestone = milestone
            showPraiseDialog(PraiseProvider.praiseFor(prefs, milestone / 10))
        }
    }

    // ---------- 复习模式 ----------
    private fun startReview(words: List<Word>) {
        mode = "review"
        reviewList = prefs.learnedIndices()
        reviewPos = 0
    }

    private fun showReview(words: List<Word>) {
        if (reviewList.isEmpty()) {
            binding.tvWord.text = "还没有可复习的旧词"
            binding.tvPhonetic.text = ""
            binding.tvMeaning.text = "先去背一些新词，再来复习吧。"
            binding.tvExample.text = ""
            binding.btnDone.isEnabled = false
            binding.btnDone.text = "已复习 ✓"
            binding.tvQueue.text = "复习模式"
            binding.btnExit.visibility = View.VISIBLE
            return
        }
        val w = words[reviewList[reviewPos % reviewList.size]]
        renderWord(w, "复习")
        binding.tvQueue.text = "复习模式 · ${reviewPos + 1} / ${reviewList.size}（循环）"
        binding.tvStreak.text = "连续 ${prefs.streak} 天 · 累计 ${prefs.total} 个 · 今天 ${prefs.todayCount()} 个"
        binding.tvDate.text = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日"))
        binding.btnExit.visibility = View.VISIBLE
    }

    // ---------- 弹窗 ----------
    private fun showDailyDoneDialog(msg: String) {
        binding.tvWord.text = msg
        binding.tvPhonetic.text = ""
        binding.tvMeaning.text = "接下来可以循环复习已背过的旧词，或退出程序。"
        binding.tvExample.text = ""
        binding.btnDone.isEnabled = false
        binding.btnDone.text = "今日新词已完成"
        updateHeader(WordRepository.all(this))
        AlertDialog.Builder(this)
            .setTitle("今日新词已完成")
            .setMessage("新词已背满，要复习旧词还是退出？")
            .setPositiveButton("复习旧词") { _, _ ->
                startReview(WordRepository.all(this))
                showReview(WordRepository.all(this))
            }
            .setNegativeButton("退出程序") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showPraiseDialog(sentence: String) {
        AlertDialog.Builder(this)
            .setTitle("🎉 表扬你！")
            .setMessage(sentence)
            .setPositiveButton("继续背") { _, _ -> }
            .show()
    }

    private fun showRewardDialog() {
        AlertDialog.Builder(this)
            .setTitle("🏆 连续 21 天达成！")
            .setMessage(
                "太了不起了！你已经连续背诵 21 天，习惯稳稳养成！\n\n送你一句：" +
                    PraiseProvider.praiseFor(prefs, prefs.streak)
            )
            .setPositiveButton("收下奖励") { _, _ -> }
            .show()
    }

    // ---------- 原有基础设施（提醒/权限） ----------
    private fun openTimePicker() {
        val dp = android.app.TimePickerDialog(this, { _, h, m ->
            prefs.hour = h
            prefs.minute = m
            ReminderScheduler.schedule(this, h, m)
            refresh()
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

    // ---------- 测试提醒 & 系统限制引导 ----------
    private fun testReminder() {
        ReminderScheduler.scheduleTest(this, 60_000)
        android.widget.Toast.makeText(
            this, "已设置：1 分钟后弹提醒，请留意通知栏（状态栏会出现闹钟图标）",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private fun showFixGuide() {
        AlertDialog.Builder(this)
            .setTitle("提醒不弹？两步放开系统限制")
            .setMessage(
                "OPPO/vivo 等国产手机默认会掐掉后台闹钟和通知。请在弹出的设置页里把本应用放开：\n\n" +
                    "1. 电池/耗电管理 → 允许后台活动，或设为「不优化/不受限制」\n" +
                    "2. 通知管理 → 允许通知、允许锁屏显示、允许悬浮通知\n" +
                    "3. 权限/应用管理 → 自启动、关联启动 → 允许\n\n" +
                    "（不同 ROM 名称略有差异，看到类似开关就打开。放开后点上面的「测试提醒」验证。）"
            )
            .setPositiveButton("去应用设置") { _, _ -> openAppSettings() }
            .setNeutralButton("去电池白名单") { _, _ -> requestIgnoreBattery() }
            .setNegativeButton("稍后", null)
            .show()
    }

    private fun openAppSettings() {
        try {
            startActivity(
                android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (_: Exception) { /* 忽略 */ }
    }

    private fun requestIgnoreBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                openAppSettings()
            }
        } else {
            openAppSettings()
        }
    }

    companion object {
        const val CHANNEL_ID = "word_reminder"
    }
}
