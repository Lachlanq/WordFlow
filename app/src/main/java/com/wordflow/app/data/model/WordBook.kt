package com.wordflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 词书数据模型 — 定义「词书」在数据库中的存储结构
 *
 * ===== 通俗理解 =====
 * 词书就是一组单词的集合。比如「CET-4 四级词汇」这本词书包含 100 个四级单词。
 * 就像书架上的一本书，用户可以从多本词书中选一本当前正在使用的。
 *
 * ===== 核心设计 =====
 * 同一时间只有一本词书的 isCurrent = true。
 * 切换词书时，先清除所有词书的"当前"标记，再标记新词书，保证数据一致性。
 *
 * ===== 字段说明 =====
 * @property id          主键，唯一标识一本词书
 * @property name        词书名称，例如 "CET-4 四级词汇"
 * @property description 词书描述，例如 "大学英语四级核心词汇"
 * @property wordCount   词书中包含的单词总数
 * @property isBuiltIn   是否为内置词书（内置词书不可删除，防止用户误删）
 * @property isCurrent   是否为当前正在使用的词书
 */
@Entity(tableName = "word_books")
data class WordBook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,  // 主键，自动生成
    val name: String,                                     // 词书名称（必填）
    val description: String = "",                         // 词书描述
    val wordCount: Int = 0,                               // 包含的单词数
    val isBuiltIn: Boolean = true,                        // 是否内置（内置不可删）
    val isCurrent: Boolean = false                        // 是否当前使用
)
