package com.wordflow.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.wordflow.app.data.db.DailyStatsDao;
import com.wordflow.app.data.db.LearningRecordDao;
import com.wordflow.app.data.db.WordBookDao;
import com.wordflow.app.data.db.WordDao;
import com.wordflow.app.data.model.DailyStats;
import com.wordflow.app.data.model.LearningRecord;
import com.wordflow.app.data.model.Word;
import com.wordflow.app.data.model.WordBook;
import com.wordflow.app.util.EbbinghausAlgorithm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 单词仓库（Repository）— 整个应用的业务逻辑中心
 *
 * ===== Repository 模式的作用 =====
 * Repository 是 MVVM 架构中的关键层，它整合了 4 个 DAO（数据源），
 * 向 UI 层提供统一、简洁的数据操作接口。
 * 可以把它理解为一个"图书馆管理员"：
 *   UI 层说「我要学 5 个新单词」→ Repository 去各个 DAO 找到合适的单词
 *   UI 层说「这个单词我认识了」→ Repository 调用 SM-2 算法计算下次复习时间
 *   UI 层说「今天学了几个？」→ Repository 去统计表查数据
 *
 * ===== 异步策略 =====
 * 单线程 ExecutorService 执行所有数据库写操作和一次性查询，
 * 通过 MutableLiveData.postValue() 将结果安全地发送到主线程。
 * 响应式查询（如词书列表）直接透传 DAO 的 LiveData。
 *
 * @param executor          单线程池，串行执行数据库操作
 * @param wordDao           单词 DAO
 * @param wordBookDao       词书 DAO
 * @param learningRecordDao 学习记录 DAO
 * @param dailyStatsDao     每日统计 DAO
 */
public class WordRepository {

    /** 单线程执行器：所有数据库写操作串行执行，避免数据竞争 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final WordDao wordDao;
    private final WordBookDao wordBookDao;
    private final LearningRecordDao learningRecordDao;
    private final DailyStatsDao dailyStatsDao;

    public WordRepository(WordDao wordDao, WordBookDao wordBookDao,
                          LearningRecordDao learningRecordDao, DailyStatsDao dailyStatsDao) {
        this.wordDao = wordDao;
        this.wordBookDao = wordBookDao;
        this.learningRecordDao = learningRecordDao;
        this.dailyStatsDao = dailyStatsDao;
    }

    // ==================== WordBook ====================

    public LiveData<List<WordBook>> getAllWordBooks() {
        return wordBookDao.getAll();
    }

    public LiveData<WordBook> getCurrentWordBook() {
        MutableLiveData<WordBook> liveData = new MutableLiveData<>();
        executor.execute(() -> liveData.postValue(wordBookDao.getCurrent()));
        return liveData;
    }

    public LiveData<WordBook> getCurrentWordBookFlow() {
        return wordBookDao.getCurrentFlow();
    }

    public void setCurrentWordBook(long bookId) {
        executor.execute(() -> {
            wordBookDao.clearCurrent();
            wordBookDao.setCurrent(bookId);
        });
    }

    // ==================== Word ====================

    public LiveData<List<Word>> getWordsByBook(long bookId) {
        MutableLiveData<List<Word>> liveData = new MutableLiveData<>();
        executor.execute(() -> liveData.postValue(wordDao.getByBookIdList(bookId)));
        return liveData;
    }

    public LiveData<Word> getWordById(long id) {
        MutableLiveData<Word> liveData = new MutableLiveData<>();
        executor.execute(() -> liveData.postValue(wordDao.getById(id)));
        return liveData;
    }

    public void insertCustomWord(String word, String meaning) {
        executor.execute(() -> {
            wordDao.insert(new Word(0, word, "", "", meaning, "", "", "", "", "", 5, true));
            wordBookDao.incrementWordCount(5);
        });
    }

    public LiveData<List<Word>> getCustomWords() {
        return wordDao.getCustomWords();
    }

    // ==================== Learning (core) ====================

    public LiveData<List<Word>> getWordsForLearning(long bookId, int count) {
        MutableLiveData<List<Word>> liveData = new MutableLiveData<>();
        executor.execute(() -> {
            long todayStart = getTodayStart();
            int learnedToday = learningRecordDao.getTodayLearnedCountByBook(todayStart, bookId);
            int remaining = Math.max(count - learnedToday, 0);
            liveData.postValue(remaining == 0 ? new ArrayList<>() : wordDao.getWordsByBook(bookId, remaining, learnedToday));
        });
        return liveData;
    }

    /**
     * 记录用户的学习反馈 — 这是整个 App 最核心的方法！
     *
     * ===== 执行流程 =====
     * Step 1: 查询该单词是否已有学习记录
     * Step 2a: 有记录 → SM-2 算法更新复习参数（calculateNextReview），执行 UPDATE
     * Step 2b: 无记录 → 创建新记录，SM-2 算法计算初始值，执行 INSERT
     * Step 3: 更新每日统计数据（每日学习量+1，如果选"认识"则正确数+1）
     *
     * @param wordId      单词 ID
     * @param familiarity 用户反馈：0=不认识, 1=模糊, 2=认识
     */
    public void recordLearning(long wordId, int familiarity) {
        executor.execute(() -> {
            LearningRecord existing = learningRecordDao.getByWordId(wordId);
            long now = System.currentTimeMillis();

            if (existing != null) {
                LearningRecord updated = EbbinghausAlgorithm.INSTANCE.calculateNextReview(existing, familiarity, now);
                learningRecordDao.update(updated);
            } else {
                LearningRecord record = new LearningRecord(0, wordId, familiarity, 1, now, 0, 2.5f, 0, false, true);
                LearningRecord calculated = EbbinghausAlgorithm.INSTANCE.calculateNextReview(record, familiarity, now);
                learningRecordDao.insert(calculated);
            }

            updateDailyStats(1, 0, familiarity >= 2 ? 1 : 0);
        });
    }

    public LiveData<Integer> getDueReviewCount(long bookId) {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        executor.execute(() -> liveData.postValue(
            learningRecordDao.getDueReviewCountByBook(System.currentTimeMillis(), bookId)));
        return liveData;
    }

    public LiveData<Integer> getTodayLearnedCount(long bookId) {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        executor.execute(() -> liveData.postValue(
            learningRecordDao.getTodayLearnedCountByBook(getTodayStart(), bookId)));
        return liveData;
    }

    public LiveData<Integer> getMasteredCount(long bookId) {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        executor.execute(() -> liveData.postValue(learningRecordDao.getMasteredCountByBook(bookId)));
        return liveData;
    }

    public LiveData<Integer> getLearningCount(long bookId) {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        executor.execute(() -> liveData.postValue(learningRecordDao.getLearningCountByBook(bookId)));
        return liveData;
    }

    public LiveData<List<Word>> getWordsForReview(long bookId) {
        MutableLiveData<List<Word>> liveData = new MutableLiveData<>();
        executor.execute(() -> {
            List<LearningRecord> records = learningRecordDao.getDueReviewsByBook(System.currentTimeMillis(), bookId);
            List<Word> words = new ArrayList<>();
            for (LearningRecord r : records) {
                Word w = wordDao.getById(r.getWordId());
                if (w != null) words.add(w);
            }
            liveData.postValue(words);
        });
        return liveData;
    }

    // ==================== Stats ====================

    public LiveData<Integer> getTotalDays() {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        executor.execute(() -> liveData.postValue(dailyStatsDao.getTotalDays()));
        return liveData;
    }

    /**
     * 计算连续打卡天数
     * 从今天开始往前数，看 daily_stats 中有多少连续的天数
     */
    public LiveData<Integer> getConsecutiveDays() {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        executor.execute(() -> {
            List<String> dates = dailyStatsDao.getAllDatesDesc();
            int streak = calculateStreak(dates);
            liveData.postValue(streak);
        });
        return liveData;
    }

    /**
     * 根据日期列表计算从今天往前的连续天数
     */
    private int calculateStreak(List<String> dates) {
        if (dates == null || dates.isEmpty()) return 0;

        java.util.Calendar cal = java.util.Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int streak = 0;
        // 检查今天或昨天是否有记录（从今天开始往前数连续天数）
        for (int i = 0; i <= 1; i++) {
            String targetDate = sdf.format(cal.getTime());
            if (dates.contains(targetDate)) {
                streak = 1;
                break;
            }
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        }

        if (streak == 0) return 0;

        // 从昨天继续往前数
        cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1); // 昨天

        while (true) {
            String targetDate = sdf.format(cal.getTime());
            if (dates.contains(targetDate)) {
                streak++;
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * 获取休息日的复习单词列表（10% 的待复习单词，至少 1 个）
     */
    public LiveData<List<Word>> getRestDayReviewWords(long bookId) {
        MutableLiveData<List<Word>> liveData = new MutableLiveData<>();
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            int dueCount = learningRecordDao.getDueReviewCountByBook(now, bookId);
            int limit = Math.max(1, (int) Math.ceil(dueCount * 0.1));
            List<LearningRecord> records = learningRecordDao.getDueReviewsByBookLimit(now, bookId, limit);
            List<Word> words = new ArrayList<>();
            for (LearningRecord r : records) {
                Word w = wordDao.getById(r.getWordId());
                if (w != null) words.add(w);
            }
            liveData.postValue(words);
        });
        return liveData;
    }

    public LiveData<List<DailyStats>> getLast7DaysStats() {
        MutableLiveData<List<DailyStats>> liveData = new MutableLiveData<>();
        executor.execute(() -> liveData.postValue(dailyStatsDao.getLast7Days()));
        return liveData;
    }

    private void updateDailyStats(int newWords, int reviewed, int correct) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DailyStats existing = dailyStatsDao.getByDate(today);
        if (existing != null) {
            existing.setNewWordsLearned(existing.getNewWordsLearned() + newWords);
            existing.setWordsReviewed(existing.getWordsReviewed() + reviewed);
            existing.setCorrectCount(existing.getCorrectCount() + correct);
            existing.setTotalCount(existing.getTotalCount() + 1);
            dailyStatsDao.update(existing);
        } else {
            dailyStatsDao.insert(new DailyStats(0, today, newWords, reviewed, correct, 1));
        }
    }

    private long getTodayStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // ==================== Reset ====================

    public void resetProgressByBook(long bookId) {
        executor.execute(() -> learningRecordDao.deleteByBookId(bookId));
    }

    public void resetAllProgress() {
        executor.execute(() -> {
            learningRecordDao.deleteAll();
            dailyStatsDao.deleteAll();
            wordDao.deleteCustomWords();
            wordBookDao.resetWordCount(5);
        });
    }
}
