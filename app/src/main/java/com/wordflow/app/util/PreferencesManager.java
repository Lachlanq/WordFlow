package com.wordflow.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 偏好设置管理器 — 封装 SharedPreferences 的读写操作
 *
 * ===== 通俗理解 =====
 * SharedPreferences 是 Android 内置的"小本本"，用来存一些零散的设置项，
 * 比如用户名、是否登录、每日目标等。数据保存在 App 私有的 XML 文件中。
 *
 * ===== 为什么不用数据库（Room）？ =====
 * Room 适合大量结构化数据（如 400+ 单词），SharedPreferences 适合少量键值对（如设置项）。
 * 两者在 WordFlow 中互补使用：
 *   Room → 单词、词书、学习记录、每日统计
 *   SharedPreferences → 用户昵称、登录状态、每日目标、收藏
 *
 * ===== apply() vs commit() =====
 * apply() 异步写入（不阻塞主线程，推荐）
 * commit() 同步写入（阻塞当前线程，返回是否成功）
 */
public class PreferencesManager {

    private final SharedPreferences prefs;  // 偏好文件名为 "wordflow_prefs"，仅本 App 可访问

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences("wordflow_prefs", Context.MODE_PRIVATE);
    }

    // ==================== 用户状态 ====================

    /** 是否已登录 */
    public boolean isLoggedIn() {
        return prefs.getBoolean("is_logged_in", false);  // 默认 false
    }

    public void setLoggedIn(boolean value) {
        prefs.edit().putBoolean("is_logged_in", value).apply();
    }

    /** 用户昵称，默认 "WordFlow用户" */
    public String getNickname() {
        return prefs.getString("nickname", "WordFlow用户");
    }

    public void setNickname(String value) {
        prefs.edit().putString("nickname", value).apply();
    }

    // ==================== 学习设置 ====================

    /** 当前使用的词书 ID，默认 1（CET-4） */
    public long getCurrentBookId() {
        return prefs.getLong("current_book_id", 1);
    }

    public void setCurrentBookId(long value) {
        prefs.edit().putLong("current_book_id", value).apply();
    }

    /** 每日学习目标（单词数），默认 20 */
    public int getDailyGoal() {
        return prefs.getInt("daily_goal", 20);
    }

    public void setDailyGoal(int value) {
        prefs.edit().putInt("daily_goal", value).apply();
    }

    /** 今日是否为休息日 */
    public boolean isRestDay() {
        return prefs.getBoolean("is_rest_day", false);
    }

    public void setRestDay(boolean value) {
        prefs.edit().putBoolean("is_rest_day", value).apply();
    }

    /** 休息日激活日期（当天零点时间戳），用于跨天自动清除 */
    public long getRestDayDate() {
        return prefs.getLong("rest_day_date", 0);
    }

    public void setRestDayDate(long value) {
        prefs.edit().putLong("rest_day_date", value).apply();
    }

    /**
     * 获取存储的休息日期集合（"yyyy-MM-dd" 格式）
     * 用于滚动 30 天窗口计数
     */
    public Set<String> getRestDayDates() {
        Set<String> set = prefs.getStringSet("rest_day_dates", new HashSet<>());
        return set != null ? new HashSet<>(set) : new HashSet<>();
    }

    public void setRestDayDates(Set<String> dates) {
        prefs.edit().putStringSet("rest_day_dates", dates).apply();
    }

    /**
     * 检查今天是否可以使用休息功能
     * 条件：近 30 天内使用次数 < 3，且今天尚未休息
     */
    public boolean canUseRestDay() {
        if (isRestDay()) return false;

        Set<String> dates = getRestDayDates();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 计算近 30 天内的使用次数
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -30);
        String thirtyDaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        int recentCount = 0;
        for (String date : dates) {
            if (date.compareTo(thirtyDaysAgo) >= 0 && date.compareTo(today) <= 0) {
                recentCount++;
            }
        }
        return recentCount < 3;
    }

    /**
     * 获取近 30 天内休息次数
     */
    public int getRestDayCountLast30Days() {
        Set<String> dates = getRestDayDates();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -30);
        String thirtyDaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        int recentCount = 0;
        for (String date : dates) {
            if (date.compareTo(thirtyDaysAgo) >= 0 && date.compareTo(today) <= 0) {
                recentCount++;
            }
        }
        return recentCount;
    }

    /**
     * 激活休息日：添加今天日期到集合，清理 30 天前旧记录，标记今天为休息日
     */
    public void useRestDay() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Set<String> dates = getRestDayDates();
        dates.add(today);

        // 清理 30 天前的旧记录
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -30);
        String thirtyDaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        dates.removeIf(date -> date.compareTo(thirtyDaysAgo) < 0);

        setRestDayDates(dates);
        setRestDay(true);
        setRestDayDate(getTodayStart());
    }

    /**
     * 检查是否需要清除休息状态（跨天后自动清除）
     * 应在 App 启动或首页加载时调用
     */
    public void clearRestDayIfNewDay() {
        if (isRestDay()) {
            long todayStart = getTodayStart();
            if (getRestDayDate() < todayStart) {
                setRestDay(false);
                setRestDayDate(0);
            }
        }
    }

    private long getTodayStart() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // ==================== 收藏（Bookmark）管理 ====================
    // 注：当前版本收藏功能 UI 已移除，但底层存储逻辑保留，方便后续恢复

    /** 检查某个单词是否已收藏 */
    public boolean isWordCollected(long wordId) {
        Set<String> set = prefs.getStringSet("collected_words", Collections.emptySet());
        return set != null && set.contains(String.valueOf(wordId));
    }

    /** 收藏一个单词 */
    public void addCollectedWord(long wordId) {
        Set<String> current = new HashSet<>(prefs.getStringSet("collected_words", new HashSet<>()));
        current.add(String.valueOf(wordId));
        prefs.edit().putStringSet("collected_words", current).apply();
    }

    /** 取消收藏一个单词 */
    public void removeCollectedWord(long wordId) {
        Set<String> current = new HashSet<>(prefs.getStringSet("collected_words", new HashSet<>()));
        current.remove(String.valueOf(wordId));
        prefs.edit().putStringSet("collected_words", current).apply();
    }

    /** 获取所有已收藏的单词 ID 列表 */
    public Set<Long> getCollectedWordIds() {
        Set<String> set = prefs.getStringSet("collected_words", new HashSet<>());
        Set<Long> result = new HashSet<>();
        if (set != null) {
            for (String s : set) {
                try { result.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    /** 清空所有收藏 */
    public void clearCollectedWords() {
        prefs.edit().putStringSet("collected_words", new HashSet<>()).apply();
    }

    /** 清空所有偏好数据 — 用于「重置应用数据」功能 */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
