package com.chenxin.wordreminder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chenxin.wordreminder.databinding.ActivityStatsBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = Prefs(this)
        val data = prefs.historyLast14()
        val max = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        val sb = StringBuilder()
        var total14 = 0
        for ((date, count) in data) {
            total14 += count
            val d = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("M/d"))
            val blocks = (count * 20 / max).coerceAtLeast(if (count > 0) 1 else 0)
            sb.appendLine(String.format("%-6s %2d 个  %s", d, count, "█".repeat(blocks)))
        }
        binding.tvStats.text = "近14天共背 $total14 个\n\n" + sb.toString()
    }
}
