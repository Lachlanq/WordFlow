package com.wordflow.app.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.wordflow.app.data.model.LearningRecord;

import java.util.List;

/**
 * 学习记录数据访问对象（DAO）— 所有 DAO 中最核心、最复杂的一个
 *
 * ===== 通俗理解 =====
 * "学习记录"存储了每个单词的记忆状态（复习了几次、下次什么时候复习、难度系数等）。
 * 这个 DAO 负责查询"哪些单词到复习时间了"、"已掌握多少个"、"今天学了几个"等关键数据。
 *
 * ===== INNER JOIN 联表查询 =====
 * 很多查询需要同时用到 learning_records 表和 words 表的数据。
 * 例如"当前词书中到期的学习记录"：需要从 learning_records 找到期记录，
 * 但要通过 JOIN words 表来按词书（wordBookId）筛选。
 *
 * @see LearningRecord 对应的数据模型
 */
@Dao
public interface LearningRecordDao {

    /** 首次学习时插入一条新记录 */
    @Insert
    long insert(LearningRecord record);

    /** 复习时更新记录（SM-2 算法计算后的新值） */
    @Update
    void update(LearningRecord record);

    /** 根据单词 ID 查询学习记录 — 用于判断该单词是否已有学习历史 */
    @Query("SELECT * FROM learning_records WHERE wordId = :wordId LIMIT 1")
    LearningRecord getByWordId(long wordId);

    /**
     * 【核心查询】获取指定词书中到达复习时间的单词
     *
     * 条件说明：
     *   isLearning = 1 → 单词正在学习中
     *   nextReviewDate <= :now → 复习时间已到（当前时间已超过下次复习时间）
     *   INNER JOIN words → 按词书（wordBookId）筛选
     *
     * 结果按 nextReviewDate 升序排列（最紧急的先复习）
     */
    @Query("SELECT lr.* FROM learning_records lr INNER JOIN words w ON lr.wordId = w.id WHERE lr.isLearning = 1 AND lr.nextReviewDate <= :now AND w.wordBookId = :bookId ORDER BY lr.nextReviewDate ASC")
    List<LearningRecord> getDueReviewsByBook(long now, long bookId);

    /** 获取指定词书中到达复习时间的单词（限量） — 用于休息日只复习 10% */
    @Query("SELECT lr.* FROM learning_records lr INNER JOIN words w ON lr.wordId = w.id WHERE lr.isLearning = 1 AND lr.nextReviewDate <= :now AND w.wordBookId = :bookId ORDER BY lr.nextReviewDate ASC LIMIT :limit")
    List<LearningRecord> getDueReviewsByBookLimit(long now, long bookId, int limit);

    /** 统计到达复习时间的单词数 — 用于首页「待复习: N」的显示 */
    @Query("SELECT COUNT(DISTINCT lr.wordId) FROM learning_records lr INNER JOIN words w ON lr.wordId = w.id WHERE lr.isLearning = 1 AND lr.nextReviewDate <= :now AND w.wordBookId = :bookId")
    int getDueReviewCountByBook(long now, long bookId);

    /** 统计已掌握的单词数 — isMastered = 1 */
    @Query("SELECT COUNT(DISTINCT lr.wordId) FROM learning_records lr INNER JOIN words w ON lr.wordId = w.id WHERE lr.isMastered = 1 AND w.wordBookId = :bookId")
    int getMasteredCountByBook(long bookId);

    /**
     * 统计今日已学习的单词数
     *
     * 判断标准：lastReviewDate >= todayStart（今天零点）
     * 也就是说，只要今天复习过的就算"今日已学"
     */
    @Query("SELECT COUNT(DISTINCT lr.wordId) FROM learning_records lr INNER JOIN words w ON lr.wordId = w.id WHERE lr.isLearning = 1 AND lr.lastReviewDate >= :todayStart AND w.wordBookId = :bookId")
    int getTodayLearnedCountByBook(long todayStart, long bookId);

    /** 统计正在学习中的单词数（isLearning = 1 但不是已掌握的） */
    @Query("SELECT COUNT(DISTINCT lr.wordId) FROM learning_records lr INNER JOIN words w ON lr.wordId = w.id WHERE lr.isLearning = 1 AND w.wordBookId = :bookId")
    int getLearningCountByBook(long bookId);

    /** 按词书重置学习进度 — 删除该词书所有单词的学习记录 */
    @Query("DELETE FROM learning_records WHERE wordId IN (SELECT id FROM words WHERE wordBookId = :bookId)")
    void deleteByBookId(long bookId);

    /** 重置所有学习记录 — 清空整张表 */
    @Query("DELETE FROM learning_records")
    void deleteAll();
}
