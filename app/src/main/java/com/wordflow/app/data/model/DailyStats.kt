package com.wordflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日统计模型 — 按天汇总学习数据，用于生成学习报告和图表
 *
 * ===== 通俗理解 =====
 * 这个表就像"学习日记本"，每天只产生一条记录。
 * 比如：2024-06-28 这天学了 15 个新词，复习了 8 个，其中有 12 个点对了。
 * 后续的学习操作都会更新当天的这条记录（累加数据），而不是插入新记录。
 *
 * ===== OnConflictStrategy.REPLACE =====
 * 插入时如果日期冲突，自动用新数据替换旧数据。
 * 配合 update 操作实现"有则更新（累加），无则插入"的效果。
 *
 * ===== 字段说明 =====
 * @property id              主键
 * @property date            日期，格式为 yyyy-MM-dd（例如 "2024-06-28"）
 *                           使用字符串而非时间戳是为了方便 SQL 按日期查询和分组
 * @property newWordsLearned 当日新学习的单词数量
 * @property wordsReviewed   当日复习的单词数量
 * @property correctCount    当日选「认识」的次数（正确次数）
 * @property totalCount      当日总答题次数（认识 + 模糊 + 不认识的总和）
 */
@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,   // 主键
    var date: String,                                      // 日期 yyyy-MM-dd
    var newWordsLearned: Int = 0,                          // 当日新学单词数
    var wordsReviewed: Int = 0,                            // 当日复习单词数
    var correctCount: Int = 0,                             // 当日正确次数(点"认识")
    var totalCount: Int = 0                                // 当日总答题次数
)
