package com.wordflow.app.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.wordflow.app.data.model.DailyStats;

import java.util.List;

/**
 * 每日统计数据访问对象（DAO）
 *
 * ===== 通俗理解 =====
 * 这个接口定义了 daily_stats 表的所有数据库操作。
 * "每日统计"就是按天汇总学习数据：今天学了多少个新词、复习了多少个、对了多少个。
 * 用于在数据分析页生成柱状图和折线图。
 *
 * ===== OnConflictStrategy.REPLACE =====
 * 插入时如果日期（date）已经存在，就用新数据替换旧数据。
 * 这实现了"每天只有一条统计记录"，后续操作通过 update 累加数据。
 */
@Dao
public interface DailyStatsDao {

    /** 插入今日统计——如果当天已有记录则替换（REPLACE策略） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyStats stats);

    /** 更新统计数据——例如累加今日学习量 */
    @Update
    void update(DailyStats stats);

    /** 根据日期查询——用于判断今天是否已有记录 */
    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    DailyStats getByDate(String date);

    /** 获取最近7天数据——倒序排列（最近在前），用于绘制柱状图 */
    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 7")
    List<DailyStats> getLast7Days();

    /** 统计累计学习天数——COUNT(DISTINCT date) 确保每天只算一次 */
    @Query("SELECT COUNT(DISTINCT date) FROM daily_stats")
    int getTotalDays();

    /** 统计累计学习单词总数——SUM 对所有行的 newWordsLearned 求和 */
    @Query("SELECT SUM(newWordsLearned) FROM daily_stats")
    int getTotalLearned();

    /** 获取所有有学习记录的日期（降序），用于计算连续打卡天数 */
    @Query("SELECT DISTINCT date FROM daily_stats ORDER BY date DESC")
    List<String> getAllDatesDesc();

    /** 清空所有每日统计数据 */
    @Query("DELETE FROM daily_stats")
    void deleteAll();
}
