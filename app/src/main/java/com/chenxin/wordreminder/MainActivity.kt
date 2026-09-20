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

        ensureExactAlarmPermission()
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
