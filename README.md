[WordFlow-README.md](https://github.com/user-attachments/files/33280769/WordFlow-README.md)
# WordFlow — 英语单词学习 App

一款基于 **Android + Java + Room** 的单词记忆应用，实现「看词 → 揭示释义 → 三级反馈 → SM-2 算法调度复习」的完整学习闭环，并用图表呈现学习数据。

> 课程设计项目 · 独立完成 · 2026.06

---

## 功能

### 学习流程
- **两种学习模式**：新词学习与到期复习（按 SM-2 算法计算的复习时间自动筛选）
- **两阶段学习**：先显示单词与音标，点击卡片揭示释义与例句，再给出反馈
- **三级反馈**：认识 / 模糊 / 不认识 —— 反馈结果直接决定下次复习时间

### 词书管理
- 内置 7 本词书：CET-4、CET-6、考研英语、雅思、托福、高中、初中
- 支持切换当前词书，切换状态持久化保存
- 支持手动录入自定义单词

### 数据统计
- 4 项核心指标：累计学习天数、已掌握词汇数、今日学习量、复习率
- **近 7 天学习量柱状图**（MPAndroidChart）
- **艾宾浩斯记忆保留率曲线**（MPAndroidChart）

### 个人中心
- 每日学习目标设置（5–100 词，带输入校验）
- 学习计划暂停（1 天 / 3 天 / 7 天）
- 按词书重置学习进度
- 手动录入单词、学习数据跳转、关于

---

## 技术栈

| 层次 | 技术 |
|---|---|
| 语言 | Java |
| 数据库 | **Room**（SQLite 抽象层），4 张表 + 42 个 DAO 查询 |
| 架构 | **MVVM 分层**：UI（Activity/Fragment）→ Repository → DAO → Room |
| 异步 | **LiveData** + 单线程 `ExecutorService`（数据库读写串行化，避免数据竞争） |
| UI 组件 | ViewBinding、Navigation Component、Material Design、RecyclerView、SwipeRefreshLayout |
| 图表 | **MPAndroidChart**（柱状图 + 折线图） |
| 轻量存储 | SharedPreferences（登录态、每日目标、暂停计划、单词收藏） |
| 构建 | Gradle 8.5 + Kotlin DSL + KSP |

---

## 项目结构

```
app/src/main/java/com/wordflow/app/
├── WordFlowApplication.java      # 全局入口：初始化数据库、Repository、偏好管理器
├── LoginActivity.java            # 登录/注册
├── SplashActivity.java           # 启动页
├── MainActivity.java             # 主容器（底部导航 + Navigation 宿主）
├── home/
│   └── HomeFragment.java         # 首页：学习进度环、新词预览、手动录入入口
├── learn/
│   └── LearnActivity.java        # 学习流程：展示 → 揭示 → 反馈 → 复习调度
├── detail/
│   └── WordDetailActivity.java   # 单词详情：音标 / 释义 / 搭配 / 同义词 / 例句
├── wordbooks/
│   └── WordBooksFragment.java    # 词书列表与切换
├── analytics/
│   └── AnalyticsFragment.java    # 数据统计与图表
├── profile/
│   └── ProfileFragment.java      # 个人中心
├── data/
│   ├── model/                    # 实体：Word / WordBook / LearningRecord / DailyStats
│   ├── db/                       # Room：AppDatabase + 4 个 DAO
│   └── repository/
│       └── WordRepository.java   # 业务逻辑中心：整合 4 个 DAO，统一对外接口
└── util/
    ├── EbbinghausAlgorithm.kt    # SM-2 间隔重复算法
    └── PreferencesManager.java   # SharedPreferences 封装
```

---

## 技术要点

### 1. SM-2 间隔重复算法

复现 SuperMemo SM-2 的核心思想：根据用户对每个单词的熟悉程度，动态调整「难度系数」与「复习间隔」。

```java
// 认识 → 间隔按难度系数放大，难度系数 +0.1
// 模糊 → 保持当前间隔，难度系数 -0.15
// 不认识 → 间隔重置为 1 天，难度系数 -0.2
// 间隔序列上限：1 → 2 → 4 → 7 → 15 → 30 → 60 → 120 天
// 难度系数下限 1.3，初始值 2.5
```

连续答对且复习次数 ≥ 4 次时标记为「已掌握」。

### 2. 统计页数据全部来自真实查询

柱状图的 7 天数据来自 `DailyStats` 表，按日期聚合；复习率由 `每日目标` 与 `今日已学` 实时计算。**没有任何硬编码的演示数据。**

### 3. 异步与线程模型

所有数据库写操作通过单线程 `ExecutorService` 串行执行，结果用 `MutableLiveData.postValue()` 安全地投递回主线程；响应式查询（如词书列表）直接返回 DAO 的 `LiveData`，由 Room 自动在数据变化时通知 UI。

---

## 如何运行

### 环境要求
- Android Studio（Ladybug 或更高版本）
- JDK 17
- Android SDK：compileSdk 36，minSdk 26

### 步骤
1. 克隆仓库
   ```bash
   git clone https://github.com/Lachlanq/WordFlow.git
   ```
2. 用 Android Studio 打开项目根目录，等待 Gradle 同步完成
3. 连接真机或启动模拟器，点击运行

> 首次运行会自动创建数据库，并预置 7 本词书与示例单词数据。

---

## 已知简化项

本项目为课程设计作品，以下部分是有意简化的，**代码注释中均有标注**：

| 项 | 说明 |
|---|---|
| 登录/注册 | 未接后端，仅校验非空并保存登录态。生产环境应接入服务端验证与密码加密 |
| 微信登录 | 预留入口，未实现 |
| 单词发音 | 未集成 TTS，因此**隐藏了发音按钮**而非保留一个无效入口 |
| 单词收藏 | SharedPreferences 底层读写方法已实现，UI 入口待接入 |

---

## 说明

- 本项目的工程骨架在 AI 辅助下搭建，本人负责需求定义、功能设计、调试与整体流程验证，代码经逐模块阅读理解。
- 界面与交互参考常见单词学习类应用；SM-2 算法为公开算法，本项目为其简化实现。
