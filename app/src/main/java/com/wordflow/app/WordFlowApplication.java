package com.wordflow.app;

import android.app.Application;

import com.wordflow.app.data.db.AppDatabase;
import com.wordflow.app.data.repository.WordRepository;
import com.wordflow.app.util.PreferencesManager;

/**
 * 自定义 Application 类 — 整个 App 的入口和全局实例提供者
 *
 * ===== 通俗理解 =====
 * Application 是 Android App 的"起点"和"大管家"。
 * 它在任何 Activity 创建之前就初始化好了，并且一直活到 App 进程被杀死。
 * 这里把数据库、Repository、偏好设置等全局对象一次性创建好，供所有页面使用。
 *
 * ===== 为什么需要自定义 Application？ =====
 * 1. 一次性初始化：在 App 启动时创建数据库、Repository、PreferencesManager
 * 2. 全局访问点：任何页面通过 WordFlowApplication.getInstance() 就能获取这些实例
 * 3. 避免重复创建：如果每个页面都新建数据库连接，浪费资源且容易出错
 *
 * ===== 单例模式 =====
 * instance 是静态变量，整个 App 只有一份 Application 实例。
 * 任何地方都可以通过 WordFlowApplication.getInstance() 访问。
 */
public class WordFlowApplication extends Application {

    private AppDatabase database;                       // 数据库（单例）
    private WordRepository repository;                  // 业务仓库
    private PreferencesManager preferencesManager;      // 偏好设置管理器

    private static WordFlowApplication instance;        // 全局 Application 实例

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;  // 保存全局引用

        // 步骤 1：创建数据库（首次启动自动预填充词书和单词数据）
        database = AppDatabase.getInstance(this);

        // 步骤 2：创建 Repository（传入 4 个 DAO），作为 UI 层和数据层的桥梁
        repository = new WordRepository(
            database.wordDao(),
            database.wordBookDao(),
            database.learningRecordDao(),
            database.dailyStatsDao()
        );

        // 步骤 3：创建偏好设置管理器（SharedPreferences 封装）
        preferencesManager = new PreferencesManager(this);
    }

    // ===== Getter 方法 =====
    public AppDatabase getDatabase() { return database; }
    public WordRepository getRepository() { return repository; }
    public PreferencesManager getPreferencesManager() { return preferencesManager; }

    /** 获取全局 Application 单例 */
    public static WordFlowApplication getInstance() { return instance; }
}
