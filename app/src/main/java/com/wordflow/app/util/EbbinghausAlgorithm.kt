package com.wordflow.app.util

import com.wordflow.app.data.model.LearningRecord

/**
 * 艾宾浩斯遗忘曲线 / SM-2 间隔重复算法 — App 的核心智能引擎
 *
 * 【背景知识】
 * - 艾宾浩斯(Hermann Ebbinghaus)是德国心理学家，1885 年发现记忆的遗忘规律：遗忘「先快后慢」
 * - SM-2(SuperMemo 2)是波兰科学家 Piotr Wozniak 在 1987 年开发的算法，
 *   它将艾宾浩斯理论转化为可编程的数学公式，是目前世界上最广泛使用的间隔记忆算法之一
 * - Anki（流行的开源闪卡软件）就是基于 SM-2 的改进版本
 *
 * 【SM-2 算法核心】
 * 算法通过三个参数调度复习：
 *   1. interval（间隔天数）— 距离下一次复习需要等待多少天
 *   2. easeFactor（难度系数）— 默认 2.5，范围 ≥ 1.3
 *      - 值越大 → 间隔增长越快 → 说明这个单词对你来说容易
 *      - 值越小 → 间隔增长越慢 → 说明这个单词对你来说困难
 *   3. familiarity（熟悉度）— 0=不认识, 1=模糊, 2=认识
 *
 * 【interval 的演变过程（以「认识」为例）】
 *   首次学习 → interval=0（当天立即可复习）
 *   第1次复习 → interval=1 天
 *   第2次复习 → interval=2 天
 *   第3次复习开始 → interval = interval × easeFactor
 *   例如 easeFactor=2.5 → 间隔呈指数增长：1→2→5→12.5→31.25...天
 *
 * 【easeFactor 的动态调整】
 *   回答「认识」→ easeFactor += 0.1（单词变容易了，下次等更久）
 *   回答「模糊」→ easeFactor -= 0.15（稍微更难了）
 *   回答「不认识」→ easeFactor -= 0.2（难很多，重置间隔）
 *
 * 【object 关键字】
 * 使用 object（单例）而非 class，因为算法是纯函数（无状态），
 * 整个应用共享一个实例即可，无需每次创建新对象。
 */
object EbbinghausAlgorithm {
    /** 难度系数下限 — 防止间隔增长过慢（低于 1.3 意味着几乎每次都要复习） */
    private const val MIN_EASE_FACTOR = 1.3f

    /**
     * 计算下一次复习时间和各项参数 — SM-2 算法的完整实现
     *
     * @param record      当前的学习记录（包含之前的 interval、easeFactor 等）
     * @param familiarity 用户本次的反馈：0=不认识, 1=模糊, 2=认识
     * @return 更新后的学习记录（interval、easeFactor、nextReviewDate 等已重新计算）
     */
    fun calculateNextReview(record: LearningRecord, familiarity: Int, now: Long = System.currentTimeMillis()): LearningRecord {
        var easeFactor = record.easeFactor
        var interval = record.interval
        val reviewCount = record.reviewCount + 1  // 复习次数 +1

        when (familiarity) {
            0 -> { // 不认识 - 重置所有进度，从头开始
                interval = 0  // interval=0 表示立即可复习（同一天内）
                // 难度系数降低 0.2，但不低于 1.3
                easeFactor = maxOf(MIN_EASE_FACTOR, easeFactor - 0.2f)
            }
            1 -> { // 模糊 - 重置间隔，但惩罚较轻
                interval = 0
                easeFactor = maxOf(MIN_EASE_FACTOR, easeFactor - 0.15f)
            }
            2 -> { // 认识 - 间隔延长，难度降低
                interval = when {
                    reviewCount == 1 -> 0       // 首次学习后，立即可复习
                    reviewCount == 2 -> 1       // 第一次复习后，1 天后再来
                    reviewCount == 3 -> 2       // 第二次复习后，2 天后再来
                    // 第三次及以后 → 间隔按难度系数倍增
                    else -> (interval * easeFactor).toInt().coerceAtLeast(1)
                }
                easeFactor = easeFactor + 0.1f  // 难度系数降低（变得更简单）
            }
        }

        // 计算 nextReviewDate：
        //   interval=0 → 现在就可以复习（复习到期）
        //   interval>=1 → 从现在起 interval 天后需要复习
        val nextReviewTime = if (interval == 0) {
            now  // 立即可复习
        } else {
            // 将天数转换为毫秒：1天 = 24 * 60 * 60 * 1000 = 86400000ms
            now + interval * 24L * 60 * 60 * 1000
        }

        // 判断是否已掌握：熟悉度达到「认识」即视为已掌握
        val isMastered = familiarity >= 2

        return record.copy(
            familiarity = familiarity,
            reviewCount = reviewCount,
            lastReviewDate = now,
            nextReviewDate = nextReviewTime,
            interval = interval,
            easeFactor = easeFactor,
            isMastered = isMastered,
            isLearning = true
        )
    }

    /**
     * 获取遗忘曲线数据 — 基于艾宾浩斯经典实验的理论数据
     *
     * 返回 Pair<天数, 记忆保留率> 的列表
     * 用于 Analytics 页面的理论遗忘曲线折线图
     *
     * 数据来源：艾宾浩斯 1885 年实验数据的简化版本
     *   0天→100%, 1天→58%, 2天→44%, 4天→36%, 7天→28%,
     *   15天→21%, 30天→15%, 60天→10%, 120天→8%
     */
    fun getForgettingCurveData(): List<Pair<Float, Float>> {
        // 记忆保留率（来自艾宾浩斯实验）
        val retentionRates = listOf(1.0f, 0.58f, 0.44f, 0.36f, 0.28f, 0.21f, 0.15f, 0.10f, 0.08f)
        // 对应的天数间隔
        val daysIntervals = listOf(0, 1, 2, 4, 7, 15, 30, 60, 120)
        // zip 将两个列表配对: (0,1.0) (1,0.58) (2,0.44) ...
        return daysIntervals.zip(retentionRates).map { (day, rate) -> day.toFloat() to rate }
    }
}
