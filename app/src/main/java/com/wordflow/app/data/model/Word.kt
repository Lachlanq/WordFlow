package com.wordflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单词数据模型 — 定义「单词」在数据库中的存储结构
 *
 * ===== 通俗理解 =====
 * 这个类就像 Excel 表格的表头，定义了一个单词有哪些信息需要存储。
 * 每新建一个 Word 对象，就相当于在数据库的 words 表中插入一行数据。
 *
 * ===== 关键注解 =====
 * @Entity(tableName = "words") → 告诉 Room："这个类对应数据库中的 words 表"
 * @PrimaryKey(autoGenerate = true) → id 是主键，自动递增（第1个单词 id=1，第2个 id=2...）
 *
 * ===== data class 是什么 =====
 * Kotlin 的 data class 会自动生成 getter/setter/equals/hashCode/toString/copy 方法，
 * 省去 Java 中手写几十行模板代码的麻烦。这就是为什么数据模型保留 Kotlin 的原因。
 *
 * ===== 字段说明 =====
 * @property id          主键，唯一标识一个单词，由数据库自动生成
 * @property word        英文单词本身，例如 "abandon"
 * @property phoneticUk  英式音标，例如 "/əˈbændən/"
 * @property phoneticUs  美式音标
 * @property mainMeaning 主要中文释义，例如 "v. 放弃；抛弃"（必填，不能为空）
 * @property subMeaning  辅助释义，例如 "n. 放任；放纵"（可选）
 * @property collocation 常见搭配用法，例如 "abandon oneself to 沉溺于"
 * @property synonym     近义词列表，例如 "desert, forsake, give up"
 * @property exampleEn   英文例句，例如 "They had to abandon the plan."
 * @property exampleCn   例句的中文翻译
 * @property wordBookId  所属词书的ID（外键），关联到 word_books 表。0 表示未分配
 * @property isCustom    是否为用户手动录入的单词。true=自定义单词，false=内置单词
 */
@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,  // 主键，自动生成
    val word: String,                                     // 英文单词（必填）
    val phoneticUk: String = "",                          // 英式音标
    val phoneticUs: String = "",                          // 美式音标
    val mainMeaning: String,                              // 主要释义（必填）
    val subMeaning: String = "",                          // 辅助释义
    val collocation: String = "",                         // 常见搭配
    val synonym: String = "",                             // 近义词
    val exampleEn: String = "",                           // 英文例句
    val exampleCn: String = "",                           // 例句中文翻译
    val wordBookId: Long = 0,                             // 所属词书ID（外键）
    val isCustom: Boolean = false                         // 是否用户自定义
)
