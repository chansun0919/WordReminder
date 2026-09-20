package com.chenxin.wordreminder

import android.content.Context
import java.time.LocalDate

/**
 * 间隔复习（艾宾浩斯遗忘曲线）：每个单词记一个熟练等级 1..MAX_LEVEL，
 * 复习后升级，下次复习间隔按 INTERVALS 推后；越熟隔得越久。
 */
object Srs {

    // 复习间隔（天）：第1次复习后隔1天，依次 2 / 4 / 7 / 15 / 30 天
    private val INTERVALS = intArrayOf(1, 2, 4, 7, 15, 30)
    private const val MAX_LEVEL = 6

    /** 当天所有「到期」单词的下标（按到期先后排序）。 */
    fun dueIndices(ctx: Context, size: Int): List<Int> {
        val p = Prefs(ctx)
        val today = LocalDate.now().toEpochDay()
        val list = mutableListOf<Int>()
        for (i in 0 until size) {
            if (p.dueOf(i) <= today) list.add(i)
        }
        return list.sortedBy { p.dueOf(it) }
    }

    /** 当前熟练等级（1..6），仅用于界面展示。 */
    fun levelOf(ctx: Context, i: Int): Int = Prefs(ctx).boxOf(i)

    /** 用户点「已背」：升级并算出下次到期日。 */
    fun review(ctx: Context, i: Int) {
        val p = Prefs(ctx)
        val level = p.boxOf(i)
        val newLevel = (level + 1).coerceAtMost(MAX_LEVEL)
        // 用「当前等级」对应的间隔（level=1 即首次复习，隔 1 天）
        val interval = INTERVALS[(level - 1).coerceIn(0, INTERVALS.lastIndex)]
        val due = LocalDate.now().toEpochDay() + interval
        p.saveReview(i, newLevel, due)
    }
}
