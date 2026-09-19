package com.chenxin.wordreminder

import android.content.Context
import org.json.JSONArray
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Word(
    val word: String,
    val phonetic: String?,
    val meaning: String,
    val example: String?
)

object WordRepository {

    private var cache: List<Word>? = null

    /** 从 assets/words.json 读取词库（只读一次，缓存）。 */
    fun all(context: Context): List<Word> {
        if (cache != null) return cache!!
        val json = context.assets.open("words.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val list = mutableListOf<Word>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Word(
                    o.getString("word"),
                    o.optString("phonetic").takeIf { it.isNotBlank() },
                    o.getString("meaning"),
                    o.optString("example").takeIf { it.isNotBlank() }
                )
            )
        }
        cache = list
        return list
    }

    /** 每天一个固定单词：按「距起始日的天数」取模，保证同一天全设备一致。 */
    fun todayWord(context: Context): Word {
        val list = all(context)
        val start = LocalDate.of(2026, 1, 1)
        val days = ChronoUnit.DAYS.between(start, LocalDate.now()).toInt()
        val idx = ((days % list.size) + list.size) % list.size
        return list[idx]
    }
}
