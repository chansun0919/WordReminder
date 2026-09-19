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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureChannel()
        requestNotifyPermission()
        ReminderScheduler.schedule(this, prefs.hour, prefs.minute)

        showToday()
        binding.btnDone.setOnClickListener { markDone() }
        binding.btnSettings.setOnClickListener { openTimePicker() }
    }

    private fun showToday() {
        val word = WordRepository.todayWord(this)
        binding.tvWord.text = word.word
        binding.tvPhonetic.text = word.phonetic ?: ""
        binding.tvMeaning.text = word.meaning
        binding.tvExample.text = word.example ?: ""

        val today = LocalDate.now().toString()
        val doneToday = prefs.lastCheckIn == today
        binding.btnDone.isEnabled = !doneToday
        binding.btnDone.text = if (doneToday) "今天已背 ✓" else "已背 ✓"

        binding.tvStreak.text = "连续 ${prefs.streak} 天 · 累计 ${prefs.total} 词"
        binding.tvDate.text = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日"))
    }

    private fun markDone() {
        prefs.checkIn()
        showToday()
    }

    private fun openTimePicker() {
        val dp = android.app.TimePickerDialog(this, { _, h, m ->
            prefs.hour = h
            prefs.minute = m
            ReminderScheduler.schedule(this, h, m)
            showToday()
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
