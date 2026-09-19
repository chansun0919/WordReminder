package com.chenxin.wordreminder

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.chenxin.wordreminder.databinding.ActivityLearnedBinding

class LearnedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearnedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val words = WordRepository.all(this)
        val prefs = Prefs(this)
        val learned = prefs.learnedIndices()

        binding.tvCount.text = "已背 ${learned.size} 个"

        if (learned.isEmpty()) {
            binding.tvEmpty.text = "还没有背过的单词，去首页背几个吧～"
            binding.listView.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            val items = learned.map { i ->
                val w = words[i]
                "${w.word}  —  ${w.meaning}"
            }
            binding.listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
            binding.tvEmpty.visibility = View.GONE
        }
    }
}
