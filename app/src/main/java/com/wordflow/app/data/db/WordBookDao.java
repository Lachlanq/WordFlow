package com.wordflow.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.wordflow.app.data.model.WordBook;

import java.util.List;

/**
 * 词书数据访问对象（DAO）— 定义对 word_books 表的所有数据库操作
 *
 * ===== 核心设计：同一时间只有一本"当前"词书 =====
 * 切换词书时分两步操作：
 *   1. clearCurrent()：清除所有词书的"当前"标记（全部设为 0）
 *   2. setCurrent()：标记新词书为"当前"（设为 1）
 * 这确保了一致性——不会出现多本词书同时被标记为"当前"的情况。
 */
@Dao
public interface WordBookDao {

    // ==================== 基本 CRUD 操作 ====================

    /** 插入词书，返回自增 ID */
    @Insert
    long insert(WordBook book);

    /** 批量插入（首次初始化时插入 5 本内置词书） */
    @Insert
    void insertAll(List<WordBook> books);

    /** 更新词书信息 */
    @Update
    void update(WordBook book);

    /** 删除词书 */
    @Delete
    void delete(WordBook book);

    // ==================== 查询操作 ====================

    /** 获取所有词书 — 返回响应式 LiveData，数据变化自动通知 UI */
    @Query("SELECT * FROM word_books")
    LiveData<List<WordBook>> getAll();

    /** 获取当前正在使用的词书（isCurrent = 1 的唯一一本） */
    @Query("SELECT * FROM word_books WHERE isCurrent = 1 LIMIT 1")
    WordBook getCurrent();

    /** 获取当前词书的 LiveData 版本 — 切换词书时自动通知 UI */
    @Query("SELECT * FROM word_books WHERE isCurrent = 1 LIMIT 1")
    LiveData<WordBook> getCurrentFlow();

    /** 根据 ID 查询词书 */
    @Query("SELECT * FROM word_books WHERE id = :id")
    WordBook getById(long id);

    /** 统计词书总数 — 用于判断是否需要预填充数据（=0 表示首次启动） */
    @Query("SELECT COUNT(*) FROM word_books")
    int getCount();

    // ==================== 词书切换操作 ====================

    /** 清除所有词书的「当前」标记 */
    @Query("UPDATE word_books SET isCurrent = 0")
    void clearCurrent();

    /** 将指定词书标记为「当前」 */
    @Query("UPDATE word_books SET isCurrent = 1 WHERE id = :id")
    void setCurrent(long id);

    // ==================== 单词计数操作 ====================

    /** 词书单词数 +1 — 用户手动添加自定义单词时调用 */
    @Query("UPDATE word_books SET wordCount = wordCount + 1 WHERE id = :bookId")
    void incrementWordCount(long bookId);

    /** 词书单词数归零 — 重置应用数据时调用（针对录入单词词书） */
    @Query("UPDATE word_books SET wordCount = 0 WHERE id = :bookId")
    void resetWordCount(long bookId);
}
