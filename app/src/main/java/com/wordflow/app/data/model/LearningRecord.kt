package com.wordflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 学习记录数据模型 — SM-2 间隔重复算法的核心数据载体
 *
 * ===== 通俗理解 =====
 * 每个被学习过的单词都有一条对应的"学习记录"。这条记录就像这个单词的"记忆档案"，
 * 记录了它的记忆状态：什么时候该复习了、复习过几次了、难度如何等。
 * SM-2 算法就是根据这些数据来计算下一次复习时间的。
 *
 * ===== 核心逻辑 =====
 * 用户反馈「认识」   → 复习间隔增长（interval × easeFactor），难度降低
 * 用户反馈「不认识」 → 重置间隔为 0，需要重新学习，难度增加
 * 用户反馈「模糊」   → 重置间隔为 0，但惩罚比"不认识"轻
 * 点击「认识」       → 标记为「已掌握」
 *
 * ===== 字段说明 =====
 * @property id             主键
 * @property wordId         关联的单词 ID（外键，对应 words 表的 id）
 * @property familiarity    熟悉度：0=不认识，1=模糊，2=认识（SM-2 中称为 q 值/质量评分）
 * @property reviewCount    已复习次数（SM-2 中称为 n 值）
 * @property lastReviewDate 上次复习的时间戳（毫秒级 Unix 时间戳）
 * @property nextReviewDate 下次需要复习的时间戳（毫秒）。当前时间 >= 这个值 → 该复习了
 * @property easeFactor     难度系数，默认 2.5（SM-2 中的 EF 值）。范围 ≥ 1.3
 *                          - 值越大，间隔增长越快 → 单词越简单
 *                          - 值越小，间隔增长越慢 → 单词越难
 * @property interval       当前复习间隔天数。0 表示立即可复习（同一天内）
 *                          - 第1次复习后 interval=1 天 → 第2次后 2 天 → 之后 × easeFactor
 * @property isMastered     是否已掌握（familiarity >= 2 即为已掌握）
 * @property isLearning     是否正在学习中（一旦开始学习就设为 true）
 */
@Entity(tableName = "learning_records")
data class LearningRecord(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,    // 主键
    var wordId: Long,                                       // 关联单词ID（外键）
    var familiarity: Int = 0,                               // 熟悉度 0不认识/1模糊/2认识
    var reviewCount: Int = 0,                               // 已复习次数
    var lastReviewDate: Long = 0,                           // 上次复习时间戳(毫秒)
    var nextReviewDate: Long = 0,                           // 下次复习时间戳(毫秒)
    var easeFactor: Float = 2.5f,                           // 难度系数(默认2.5，≥1.3)
    var interval: Int = 0,                                  // 当前间隔天数
    var isMastered: Boolean = false,                        // 是否已掌握
    var isLearning: Boolean = false                         // 是否正在学习
)
