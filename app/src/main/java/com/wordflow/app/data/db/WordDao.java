package com.wordflow.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.wordflow.app.data.model.Word;

import java.util.List;

/**
 * 单词数据访问对象（DAO）— 定义对 words 表的所有数据库操作
 *
 * ===== 通俗理解 =====
 * 这个接口定义了"单词表"有哪些操作可以用：增删改查、按词书查询、分页查询等。
 * Room 会在编译时自动生成这些方法的实现代码，不用我们手写 SQL 执行逻辑。
 *
 * ===== LiveData vs 普通返回值的区别 =====
 * 返回 LiveData 的方法：数据库内容变化时，LiveData 会自动通知 UI 刷新（响应式）
 * 返回普通值的方法：只查询一次，之后数据变了也不会自动通知
 *
 * ===== 为什么有些方法有两个版本？ =====
 * 如 getByBookId 返回 LiveData（给 UI 用的，需要自动刷新），
 * getByBookIdList 返回普通 List（给 Repository 后台线程用的，只需要查一次）。
 */
@Dao
public interface WordDao {

    // ==================== 基本 CRUD 操作 ====================

    /** 插入一个单词，返回自增主键 ID */
    @Insert
    long insert(Word word);

    /** 批量插入（用于首次启动时预填充 400+ 单词） */
    @Insert
    void insertAll(List<Word> words);

    /** 更新单词信息 */
    @Update
    void update(Word word);

    /** 删除单词 */
    @Delete
    void delete(Word word);

    // ==================== 查询操作 ====================

    /** 根据主键 ID 查询单个单词 */
    @Query("SELECT * FROM words WHERE id = :id")
    Word getById(long id);

    /** 根据词书 ID 查询单词列表 — 响应式 LiveData，数据变化 UI 自动刷新 */
    @Query("SELECT * FROM words WHERE wordBookId = :bookId")
    LiveData<List<Word>> getByBookId(long bookId);

    /** 同 getByBookId，但返回普通 List（一次性查询，不需要响应式更新的场景） */
    @Query("SELECT * FROM words WHERE wordBookId = :bookId")
    List<Word> getByBookIdList(long bookId);

    /** 查询用户自定义的单词（isCustom = 1 表示自定义，0 表示内置） */
    @Query("SELECT * FROM words WHERE isCustom = 1")
    LiveData<List<Word>> getCustomWords();

    /**
     * 分页查询单词 — 用于学习模式
     * @param bookId 词书 ID
     * @param limit  每次取多少个（对应当日剩余目标数）
     * @param offset 跳过多少个（对应当日已学数量）
     */
    @Query("SELECT * FROM words WHERE wordBookId = :bookId ORDER BY id LIMIT :limit OFFSET :offset")
    List<Word> getWordsByBook(long bookId, int limit, int offset);

    /** 统计词书中单词总数 */
    @Query("SELECT COUNT(*) FROM words WHERE wordBookId = :bookId")
    int getCountByBook(long bookId);

    /** 按词书删除所有非自定义单词 — 用于重置词书进度 */
    @Query("DELETE FROM words WHERE isCustom = 0 AND wordBookId = :bookId")
    void deleteByBookId(long bookId);

    /** 删除所有用户自定义单词 — 用于「重置应用数据」 */
    @Query("DELETE FROM words WHERE isCustom = 1")
    void deleteCustomWords();
}
