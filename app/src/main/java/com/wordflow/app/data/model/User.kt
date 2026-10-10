package com.wordflow.app.data.model

/**
 * 用户信息模型
 *
 * ===== 通俗理解 =====
 * 存储当前使用者的基本信息（昵称、学习天数等）。
 * 注意：这个类没有 @Entity 注解，说明它不存数据库，而是存在 SharedPreferences（一个轻量级的 XML 配置文件）中。
 *
 * ===== 为什么不用数据库存用户信息？ =====
 * 用户信息只有几项配置（昵称、学习天数等），数据量小。
 * SharedPreferences 更轻量，读写更快，适合存这种零散的设置项。
 * Room 数据库适合存大量结构化数据（如 400+ 单词），各有分工。
 *
 * @property id            用户 ID
 * @property nickname      用户昵称，默认 "WordFlow用户"
 * @property avatar        头像路径（预留字段，当前版本未使用）
 * @property phone         手机号
 * @property email         邮箱地址
 * @property totalDays     累计学习天数
 * @property masteredWords 已掌握单词总数
 */
data class User(
    val id: Long = 0,
    val nickname: String = "WordFlow用户",
    val avatar: String = "",
    val phone: String = "",
    val email: String = "",
    val totalDays: Int = 0,
    val masteredWords: Int = 0
)
