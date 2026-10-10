package com.wordflow.app.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.wordflow.app.data.model.DailyStats;
import com.wordflow.app.data.model.LearningRecord;
import com.wordflow.app.data.model.Word;
import com.wordflow.app.data.model.WordBook;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room 数据库主类 — 整个应用的数据库核心
 *
 * ===== 什么是 Room？ =====
 * Room 是 Google 官方推出的 Android 数据库框架，底层封装了 SQLite。
 * 用注解定义数据库结构，编译时自动生成实现代码、校验 SQL 语法。
 *
 * ===== @Database 注解 =====
 * entities：声明数据库中包含的表（每个类对应一张表）
 *   本数据库有 4 张表：words(单词)、word_books(词书)、learning_records(学习记录)、daily_stats(每日统计)
 * version：数据库版本号，修改表结构时需递增
 *
 * ===== 单例模式（双重检查锁定） =====
 * 整个应用只创建一个数据库实例，通过 getInstance() 获取。
 * volatile + synchronized 双重检查锁定，确保多线程安全。
 * 单例的好处：避免重复创建数据库连接，节省资源，保证数据一致性。
 *
 * ===== 预填充数据 =====
 * 首次启动时（getCount() == 0），自动向词书表和单词表填充默认数据。
 * 包含 5 本词书（CET-4/CET-6/考研/高中/录入单词）和 400+ 个单词。
 *
 * ===== 数据库迁移策略 =====
 * fallbackToDestructiveMigration()：结构变化时销毁旧数据库→创建新数据库。
 * 注意：这只适用于开发阶段！生产环境应该写 Migration 来保留用户数据。
 */
@Database(
    entities = {Word.class, WordBook.class, LearningRecord.class, DailyStats.class},
    version = 4,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    /** Room 在编译时自动为这些抽象方法生成实现 */
    public abstract WordDao wordDao();
    public abstract WordBookDao wordBookDao();
    public abstract LearningRecordDao learningRecordDao();
    public abstract DailyStatsDao dailyStatsDao();

    /** volatile 确保多线程间的可见性：一个线程修改后，其他线程立即看到新值 */
    private static volatile AppDatabase INSTANCE;

    /**
     * 获取数据库单例 — 双重检查锁定模式
     * 外层 if：大部分情况下 INSTANCE 已存在，直接返回，不加锁，性能好
     * 内层 synchronized + if：确保只有一个线程创建实例
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = buildDatabase(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 构建数据库实例
     * 使用 applicationContext 而非 Activity 的 Context，避免 Activity 销毁后内存泄漏。
     */
    private static AppDatabase buildDatabase(Context context) {
        AppDatabase database = Room.databaseBuilder(
            context.getApplicationContext(),  // 用 ApplicationContext 防止内存泄漏
            AppDatabase.class,
            "wordflow_database"               // 数据库文件名
        ).fallbackToDestructiveMigration()     // 开发阶段：版本变化时重建数据库
         .build();

        // 后台线程预填充数据，阻塞等待完成（等效原 Kotlin 的 runBlocking）
        // 因为 Application.onCreate() 在 Splash 页面显示前执行，阻塞不影响用户体验
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> {
                if (database.wordBookDao().getCount() == 0) {
                    prepopulateData(database.wordBookDao(), database.wordDao());
                }
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepopulate database", e);
        }
        executor.shutdown();
        return database;
    }

    /** Simplified word constructor for seed data — uses full Kotlin data class constructor (all val fields, Java positional args) */
    private static Word w(long bookId, String word, String meaning, String exEn, String exCn) {
        return new Word(0, word, "", "", meaning, "", "", "", exEn, exCn, bookId, false);
    }

    private static void prepopulateData(WordBookDao bookDao, WordDao wordDao) {
        List<WordBook> books = Arrays.asList(
            new WordBook(0, "CET-4 四级词汇", "大学英语四级核心词汇", 100, true, true),
            new WordBook(0, "CET-6 六级词汇", "大学英语六级核心词汇", 100, true, false),
            new WordBook(0, "考研英语", "考研英语核心词汇", 100, true, false),
            new WordBook(0, "高中英语", "高中英语必修词汇", 100, true, false),
            new WordBook(0, "录入单词", "手动录入的生词本", 0, true, false)
        );
        for (WordBook book : books) bookDao.insert(book);

        // CET-4 (bookId=1)
        List<Word> cet4 = Arrays.asList(
            w(1, "abandon", "v. 放弃；抛弃", "They had to abandon the plan.", "他们不得不放弃这个计划。"),
            w(1, "absorb", "v. 吸收；吸引", "Plants absorb sunlight for energy.", "植物吸收阳光获取能量。"),
            w(1, "abstract", "adj. 抽象的；理论的", "Beauty is an abstract concept.", "美是一个抽象的概念。"),
            w(1, "abuse", "v./n. 滥用；虐待", "He abused his power as a manager.", "他滥用了作为经理的权力。"),
            w(1, "academic", "adj. 学术的；学院的", "She has an excellent academic record.", "她有优秀的学业成绩。"),
            w(1, "accelerate", "v. 加速；促进", "We need to accelerate the reform.", "我们需要加快改革。"),
            w(1, "access", "n. 通道；使用权", "Students have access to the library.", "学生可以使用图书馆。"),
            w(1, "accommodate", "v. 容纳；提供住宿", "The hotel can accommodate 500 guests.", "这家酒店可容纳500位客人。"),
            w(1, "accompany", "v. 陪伴；伴随", "She accompanied me to the doctor.", "她陪我去看医生。"),
            w(1, "accomplish", "v. 完成；实现", "We have accomplished our mission.", "我们已经完成了使命。"),
            w(1, "account", "n. 账户；说明；v. 解释", "How do you account for the delay?", "你如何解释这个延迟？"),
            w(1, "accumulate", "v. 积累；积聚", "He accumulated a lot of experience.", "他积累了很多经验。"),
            w(1, "accurate", "adj. 准确的；精确的", "His prediction proved accurate.", "他的预测被证明是准确的。"),
            w(1, "achieve", "v. 达到；取得", "She achieved her goal of becoming a doctor.", "她实现了成为医生的目标。"),
            w(1, "acknowledge", "v. 承认；确认", "He acknowledged his mistake.", "他承认了自己的错误。"),
            w(1, "acquire", "v. 获得；习得", "She acquired a good knowledge of English.", "她掌握了很好的英语知识。"),
            w(1, "adapt", "v. 适应；改编", "You need to adapt to the new environment.", "你需要适应新环境。"),
            w(1, "adequate", "adj. 足够的；适当的", "The food supply is adequate.", "食物供应充足。"),
            w(1, "adjust", "v. 调整；调节", "I need to adjust my watch.", "我需要调一下手表。"),
            w(1, "administration", "n. 管理；行政", "She works in business administration.", "她从事工商管理工作。"),
            w(1, "adopt", "v. 采用；收养", "The company adopted a new strategy.", "公司采用了新战略。"),
            w(1, "advance", "v./n. 前进；进步", "Technology advances rapidly.", "技术进步迅速。"),
            w(1, "advantage", "n. 优势；有利条件", "Being tall is an advantage in basketball.", "个子高在篮球中是优势。"),
            w(1, "advertise", "v. 做广告；宣传", "They advertise their products online.", "他们在网上宣传产品。"),
            w(1, "advocate", "v. 提倡；拥护", "He advocates for environmental protection.", "他倡导环境保护。"),
            w(1, "affair", "n. 事务；事件", "This is a matter of public affairs.", "这是公共事务。"),
            w(1, "affect", "v. 影响；感染", "The weather affects my mood.", "天气影响我的心情。"),
            w(1, "afford", "v. 负担得起；提供", "I can't afford a new car.", "我买不起新车。"),
            w(1, "aggressive", "adj. 侵略的；好斗的", "Some dogs are very aggressive.", "有些狗很有攻击性。"),
            w(1, "allocate", "v. 分配；拨出", "Funds were allocated to education.", "资金被分配给教育。"),
            w(1, "alternative", "n. 替代品；选择", "We need to find an alternative.", "我们需要找到一个替代方案。"),
            w(1, "amateur", "n. 业余爱好者；adj. 业余的", "He is an amateur photographer.", "他是一名业余摄影师。"),
            w(1, "ambition", "n. 雄心；抱负", "Her ambition is to become a CEO.", "她的抱负是成为CEO。"),
            w(1, "analyze", "v. 分析；解析", "We need to analyze the results.", "我们需要分析结果。"),
            w(1, "annual", "adj. 年度的；每年的", "The annual meeting will be held next week.", "年会将于下周举行。"),
            w(1, "anticipate", "v. 预期；期望", "We anticipate a rise in demand.", "我们预期需求会上升。"),
            w(1, "anxiety", "n. 焦虑；忧虑", "She suffers from anxiety about exams.", "她因考试而感到焦虑。"),
            w(1, "apparent", "adj. 明显的；表面的", "It soon became apparent that he was wrong.", "很快就明显他是错的。"),
            w(1, "appetite", "n. 食欲；胃口", "Exercise gave me a good appetite.", "运动让我胃口大开。"),
            w(1, "apply", "v. 申请；应用", "You should apply for the scholarship.", "你应该申请奖学金。"),
            w(1, "appoint", "v. 任命；约定", "They appointed him as the new manager.", "他们任命他为新经理。"),
            w(1, "appreciate", "v. 欣赏；感激", "I appreciate your kindness.", "我很感激你的好意。"),
            w(1, "approach", "v. 接近；n. 方法", "We need a new approach to the problem.", "我们需要新方法解决问题。"),
            w(1, "appropriate", "adj. 适当的；合适的", "Is this appropriate for the occasion?", "这适合这个场合吗？"),
            w(1, "approve", "v. 批准；赞同", "The committee approved the plan.", "委员会批准了这个计划。"),
            w(1, "arise", "v. 出现；产生", "Problems may arise at any time.", "问题随时可能出现。"),
            w(1, "arrange", "v. 安排；整理", "I will arrange a meeting for tomorrow.", "我会安排明天的会议。"),
            w(1, "arrest", "v./n. 逮捕；拘留", "The police arrested the suspect.", "警方逮捕了嫌疑人。"),
            w(1, "artificial", "adj. 人造的；虚假的", "Artificial intelligence is changing the world.", "人工智能正在改变世界。"),
            w(1, "assess", "v. 评估；评定", "We need to assess the situation.", "我们需要评估形势。"),
            w(1, "assign", "v. 分配；指派", "The teacher assigned homework to the students.", "老师给学生布置了作业。"),
            w(1, "assist", "v. 帮助；协助", "Can I assist you with anything?", "我能帮你什么吗？"),
            w(1, "associate", "v. 联系；关联；n. 同事", "I always associate summer with vacation.", "我总把夏天和假期联系在一起。"),
            w(1, "assume", "v. 假设；承担", "I assume you are right.", "我假定你是对的。"),
            w(1, "atmosphere", "n. 气氛；大气", "The restaurant has a friendly atmosphere.", "这家餐厅气氛友好。"),
            w(1, "attach", "v. 附上；贴上", "Please attach your photo to the form.", "请把照片附在表格上。"),
            w(1, "attain", "v. 达到；获得", "He attained his goal through hard work.", "他通过努力达到了目标。"),
            w(1, "attempt", "v./n. 尝试；企图", "She made an attempt to break the record.", "她试图打破纪录。"),
            w(1, "attend", "v. 参加；出席", "Many people attended the concert.", "很多人参加了音乐会。"),
            w(1, "attitude", "n. 态度；看法", "A positive attitude is important.", "积极的态度很重要。"),
            w(1, "attract", "v. 吸引；引起", "The exhibition attracted many visitors.", "展览吸引了许多参观者。"),
            w(1, "attribute", "v. 归因于；n. 属性", "He attributed his success to hard work.", "他把成功归功于努力。"),
            w(1, "audience", "n. 观众；听众", "The audience applauded loudly.", "观众热烈鼓掌。"),
            w(1, "authority", "n. 权威；当局", "He is an authority on ancient history.", "他是古代史的权威。"),
            w(1, "available", "adj. 可用的；可获得的", "The book is available in the library.", "这本书在图书馆可以借到。"),
            w(1, "average", "adj. 平均的；普通的", "The average temperature here is 20°C.", "这里的平均气温是20°C。"),
            w(1, "avoid", "v. 避免；回避", "You should avoid eating too much sugar.", "你应该避免吃太多糖。"),
            w(1, "award", "n. 奖品；v. 授予", "She won an award for her novel.", "她的小说获奖了。"),
            w(1, "aware", "adj. 意识到的；知道的", "Are you aware of the risks?", "你意识到风险了吗？"),
            w(1, "barrier", "n. 障碍；屏障", "Language can be a barrier to communication.", "语言可能成为沟通的障碍。"),
            w(1, "benefit", "n. 利益；v. 有益于", "Exercise benefits your health.", "锻炼有益健康。"),
            w(1, "budget", "n. 预算", "We need to stay within our budget.", "我们需要控制在预算内。"),
            w(1, "burden", "n. 负担；重担", "The heavy workload was a burden.", "繁重的工作是一种负担。"),
            w(1, "campaign", "n. 运动；战役", "They launched an advertising campaign.", "他们发起了一场广告活动。"),
            w(1, "capable", "adj. 有能力的", "She is capable of handling the task.", "她有能力处理这个任务。"),
            w(1, "capacity", "n. 容量；能力", "The hall has a capacity of 500.", "这个大厅可容纳500人。"),
            w(1, "career", "n. 职业；生涯", "She chose teaching as her career.", "她选择了教书作为职业。"),
            w(1, "challenge", "n. 挑战；v. 向…挑战", "This job is a real challenge.", "这份工作是一个真正的挑战。"),
            w(1, "character", "n. 性格；角色；文字", "He has a strong character.", "他有坚强的性格。"),
            w(1, "charity", "n. 慈善；慈善机构", "They donated money to charity.", "他们向慈善机构捐款。"),
            w(1, "circumstance", "n. 环境；情况", "Under the circumstances, we must be careful.", "在这种情况下我们必须小心。"),
            w(1, "claim", "v./n. 声称；要求", "He claimed that he was innocent.", "他声称自己是无辜的。"),
            w(1, "colleague", "n. 同事", "I discussed it with my colleagues.", "我和同事讨论过这件事。"),
            w(1, "combine", "v. 结合；联合", "We should combine theory with practice.", "我们应该把理论和实践结合起来。"),
            w(1, "comment", "n./v. 评论；意见", "Do you have any comments on the plan?", "你对这个计划有什么意见吗？"),
            w(1, "commit", "v. 犯(罪)；承诺", "He committed himself to finishing the work.", "他承诺完成这项工作。"),
            w(1, "communicate", "v. 沟通；交流", "We communicate by email.", "我们通过电子邮件沟通。"),
            w(1, "community", "n. 社区；团体", "The community supports the new policy.", "社区支持新政策。"),
            w(1, "compete", "v. 竞争；比赛", "Several companies are competing for the contract.", "几家公司正在竞争这个合同。"),
            w(1, "concentrate", "v. 集中；专心", "I can't concentrate on my work.", "我无法集中精力工作。"),
            w(1, "concern", "n. 关心；v. 涉及", "This issue concerns everyone.", "这个问题关系到每个人。"),
            w(1, "conduct", "v. 进行；指挥；n. 行为", "They conducted a survey of public opinion.", "他们进行了民意调查。"),
            w(1, "confident", "adj. 自信的", "She is confident about the future.", "她对未来充满信心。"),
            w(1, "confirm", "v. 确认；证实", "Can you confirm the meeting time?", "你能确认一下会议时间吗？"),
            w(1, "conflict", "n. 冲突；矛盾", "The two countries are in conflict.", "这两个国家存在冲突。"),
            w(1, "connect", "v. 连接；联系", "The bridge connects the two cities.", "这座桥连接着两座城市。"),
            w(1, "conserve", "v. 保存；节约", "We should conserve water.", "我们应该节约用水。"),
            w(1, "consume", "v. 消费；消耗", "This car consumes a lot of fuel.", "这辆车耗油很多。"),
            w(1, "contrast", "n./v. 对比；差异", "In contrast to his brother, he is very tall.", "与他哥哥相比，他很高。"),
            w(1, "contribute", "v. 贡献；捐助", "Everyone should contribute to society.", "每个人都应该为社会做贡献。")
        );

        // CET-6 (bookId=2)
        List<Word> cet6 = Arrays.asList(
            w(2, "abnormal", "adj. 反常的；异常的", "The weather has been abnormal this year.", "今年的天气很不正常。"),
            w(2, "abolish", "v. 废除；取消", "Slavery was abolished in the 19th century.", "奴隶制在19世纪被废除了。"),
            w(2, "absurd", "adj. 荒谬的；可笑的", "That's an absurd idea!", "那是个荒谬的想法！"),
            w(2, "abundance", "n. 丰富；充裕", "There is an abundance of wildlife here.", "这里有丰富的野生动物。"),
            w(2, "accessory", "n. 附件；配件；从犯", "She bought some accessories for her phone.", "她买了些手机配件。"),
            w(2, "acquaint", "v. 使熟悉；使了解", "You should acquaint yourself with the rules.", "你应该熟悉一下规则。"),
            w(2, "addict", "v. 使上瘾；n. 上瘾的人", "He is addicted to video games.", "他对电子游戏上瘾了。"),
            w(2, "adhere", "v. 粘附；遵守；坚持", "We must adhere to the regulations.", "我们必须遵守规定。"),
            w(2, "adjacent", "adj. 邻近的；毗连的", "The hotel is adjacent to the station.", "酒店就在车站旁边。"),
            w(2, "administer", "v. 管理；执行；给予", "They administer the company's finances.", "他们管理公司的财务。"),
            w(2, "adolescent", "n. 青少年；adj. 青春期的", "Adolescents need guidance from adults.", "青少年需要成年人的指导。"),
            w(2, "advent", "n. 到来；出现", "The advent of the internet changed everything.", "互联网的出现改变了一切。"),
            w(2, "adverse", "adj. 不利的；相反的", "The drug has some adverse effects.", "这种药有一些不良反应。"),
            w(2, "aggravate", "v. 加重；恶化", "The cold weather aggravated his illness.", "寒冷的天气加重了他的病情。"),
            w(2, "aggregate", "n. 合计；集合体", "The aggregate cost is over budget.", "总成本超出预算。"),
            w(2, "agony", "n. 极度痛苦", "He was in agony after the accident.", "事故后他痛苦万分。"),
            w(2, "alien", "adj. 外国的；陌生的；n. 外星人", "The customs seemed alien to me.", "这些习俗对我来说很陌生。"),
            w(2, "allege", "v. 断言；声称", "He alleged that he was not at the scene.", "他声称自己不在现场。"),
            w(2, "alleviate", "v. 减轻；缓解", "Medicine can alleviate the pain.", "药物可以缓解疼痛。"),
            w(2, "alliance", "n. 联盟；同盟", "The two companies formed an alliance.", "两家公司结成了联盟。"),
            w(2, "ambiguous", "adj. 模糊的；模棱两可的", "The statement was deliberately ambiguous.", "这个声明故意含糊其辞。"),
            w(2, "analogy", "n. 类比；比喻", "He explained by using an analogy.", "他用类比来解释。"),
            w(2, "anonymous", "adj. 匿名的", "The donation was made by an anonymous person.", "捐款是由匿名人士提供的。"),
            w(2, "apparatus", "n. 仪器；设备", "The laboratory has modern apparatus.", "实验室有现代化的仪器。"),
            w(2, "appease", "v. 安抚；平息", "He tried to appease the angry crowd.", "他试图安抚愤怒的人群。"),
            w(2, "arbitrary", "adj. 任意的；专断的", "The decision seemed arbitrary.", "这个决定似乎很武断。"),
            w(2, "articulate", "v. 清楚表达；adj. 口才好的", "She articulated her thoughts clearly.", "她清楚地表达了自己的想法。"),
            w(2, "ascend", "v. 上升；攀登", "The elevator ascended to the top floor.", "电梯升到了顶楼。"),
            w(2, "assault", "n./v. 攻击；袭击", "He was charged with assault.", "他被控袭击罪。"),
            w(2, "assert", "v. 断言；坚持；维护", "He asserted his innocence.", "他坚持自己是无辜的。"),
            w(2, "asset", "n. 资产；优点", "Knowledge is your greatest asset.", "知识是你最大的资产。"),
            w(2, "assimilate", "v. 吸收；同化", "Immigrants assimilated into the culture.", "移民融入了当地文化。"),
            w(2, "attendant", "n. 服务员；随从", "The flight attendant served drinks.", "空乘人员端上了饮料。"),
            w(2, "authentic", "adj. 真正的；真实的", "This is an authentic Picasso painting.", "这是一幅毕加索的真迹。"),
            w(2, "authorize", "v. 授权；批准", "Only the manager can authorize the payment.", "只有经理可以批准付款。"),
            w(2, "autonomy", "n. 自治；自主权", "The region has a high degree of autonomy.", "该地区有高度自治权。"),
            w(2, "avail", "v. 有益于；n. 效用", "All our efforts were of no avail.", "我们所有的努力都是徒劳。"),
            w(2, "baffle", "v. 使困惑；难住", "The mystery baffled the detectives.", "这个谜团难住了侦探们。"),
            w(2, "bald", "adj. 秃头的；光秃的", "He went bald at a young age.", "他年纪轻轻就秃头了。"),
            w(2, "barren", "adj. 贫瘠的；不生育的", "The land became barren after the drought.", "干旱之后土地变得贫瘠。"),
            w(2, "beforehand", "adv. 事先；预先", "Please let me know beforehand.", "请事先让我知道。"),
            w(2, "behave", "v. 行为；表现", "Children should learn to behave properly.", "孩子应该学会举止得体。"),
            w(2, "bewilder", "v. 使迷惑；使不知所措", "The complex rules bewildered him.", "复杂的规则让他困惑。"),
            w(2, "bias", "n. 偏见；偏心", "The judge showed no bias in the case.", "法官在这个案件中不偏不倚。"),
            w(2, "bleak", "adj. 荒凉的；暗淡的", "The future looks bleak for the company.", "公司的前景黯淡。"),
            w(2, "bless", "v. 祝福；保佑", "God bless you!", "上帝保佑你！"),
            w(2, "blunder", "n. 大错；v. 犯大错", "He made a blunder in the report.", "他在报告中犯了一个大错。"),
            w(2, "bonus", "n. 奖金；红利", "Employees received a year-end bonus.", "员工收到了年终奖金。"),
            w(2, "boom", "n. 繁荣；v. 激增", "The economy experienced a boom.", "经济经历了繁荣期。"),
            w(2, "boycott", "v./n. 联合抵制", "They called for a boycott of the products.", "他们呼吁抵制这些产品。"),
            w(2, "breach", "n. 违反；缺口；v. 打破", "This is a breach of the agreement.", "这违反了协议。"),
            w(2, "brisk", "adj. 轻快的；活跃的", "We went for a brisk walk in the park.", "我们在公园里轻快地散步。"),
            w(2, "bureaucracy", "n. 官僚主义；官僚机构", "The bureaucracy slows down decision-making.", "官僚机构延缓了决策。"),
            w(2, "catastrophe", "n. 大灾难；大祸", "The flood was a catastrophe for the region.", "洪水对该地区是一场灾难。"),
            w(2, "cater", "v. 提供餐饮；迎合", "The restaurant caters for weddings.", "这家餐厅承办婚宴。"),
            w(2, "caution", "n. 小心；警告", "Use caution when crossing the street.", "过马路要小心。"),
            w(2, "certify", "v. 证明；认证", "The document needs to be certified.", "这份文件需要认证。"),
            w(2, "chronic", "adj. 慢性的；长期的", "He suffers from chronic pain.", "他患有慢性疼痛。"),
            w(2, "circulation", "n. 循环；流通；发行量", "The magazine has a wide circulation.", "这本杂志发行量很大。"),
            w(2, "clash", "n./v. 冲突；碰撞", "There was a clash between the two groups.", "两个团体之间发生了冲突。"),
            w(2, "cling", "v. 紧紧抓住；坚持", "The child clung to his mother's hand.", "孩子紧紧抓住妈妈的手。"),
            w(2, "cognitive", "adj. 认知的", "Cognitive development is important for children.", "认知发展对孩子很重要。"),
            w(2, "coincide", "v. 同时发生；相符", "The meeting coincided with my vacation.", "会议和我的假期撞期了。"),
            w(2, "collaborate", "v. 合作；协作", "We collaborated on the project.", "我们在这个项目上合作了。"),
            w(2, "collapse", "v./n. 倒塌；崩溃", "The building collapsed in the earthquake.", "大楼在地震中倒塌了。"),
            w(2, "commemorate", "v. 纪念", "A ceremony was held to commemorate the heroes.", "举行了仪式来纪念英雄们。"),
            w(2, "commence", "v. 开始；着手", "The ceremony will commence at noon.", "仪式将于中午开始。"),
            w(2, "commodity", "n. 商品；货物", "Oil is a valuable commodity.", "石油是宝贵的商品。"),
            w(2, "commonplace", "adj. 平凡的；普通的", "Smartphones have become commonplace.", "智能手机已经很普遍了。"),
            w(2, "compatible", "adj. 兼容的；能共处的", "The software is compatible with all devices.", "这个软件与所有设备兼容。"),
            w(2, "compensate", "v. 补偿；赔偿", "They compensated him for his loss.", "他们赔偿了他的损失。"),
            w(2, "compile", "v. 编译；汇编", "She compiled a list of useful resources.", "她汇编了一份有用资源清单。"),
            w(2, "complement", "v. 补充；n. 补充物", "The wine complements the food perfectly.", "这酒与食物完美搭配。"),
            w(2, "comply", "v. 遵守；服从", "You must comply with the rules.", "你必须遵守规则。"),
            w(2, "compulsory", "adj. 强制的；必修的", "English is a compulsory subject.", "英语是必修科目。"),
            w(2, "conceive", "v. 构思；怀孕", "She conceived the idea for the book.", "她构思了这本书的想法。"),
            w(2, "conscience", "n. 良心；良知", "He had a guilty conscience.", "他良心不安。"),
            w(2, "conscientious", "adj. 认真负责的", "She is a conscientious worker.", "她是一个认真负责的员工。"),
            w(2, "consecutive", "adj. 连续的", "It rained for three consecutive days.", "连续下了三天雨。"),
            w(2, "consensus", "n. 共识；一致意见", "We reached a consensus on the issue.", "我们在这个问题上达成了共识。"),
            w(2, "consolidate", "v. 巩固；合并", "They consolidated their market position.", "他们巩固了市场地位。"),
            w(2, "conspicuous", "adj. 显眼的；引人注目的", "The red car was very conspicuous.", "那辆红色的车非常显眼。"),
            w(2, "constrain", "v. 限制；约束", "Financial problems constrained their plans.", "财务问题限制了他们的计划。"),
            w(2, "contaminate", "v. 污染；玷污", "The river was contaminated by chemicals.", "河流被化学物质污染了。"),
            w(2, "contemplate", "v. 沉思；考虑", "She contemplated changing her job.", "她考虑换工作。"),
            w(2, "contradict", "v. 与…矛盾；反驳", "His actions contradict his words.", "他的行为与言语矛盾。"),
            w(2, "controversy", "n. 争议；争论", "The decision caused a lot of controversy.", "这个决定引起了很多争议。"),
            w(2, "converge", "v. 汇聚；集中", "The roads converge at the town center.", "这些路在镇中心汇聚。"),
            w(2, "convict", "v. 定罪；n. 罪犯", "He was convicted of theft.", "他被判盗窃罪。"),
            w(2, "coordinate", "v. 协调；配合", "We need to coordinate our efforts.", "我们需要协调我们的努力。"),
            w(2, "cordial", "adj. 热诚的；友好的", "We had a cordial meeting with the guests.", "我们与客人进行了友好的会面。"),
            w(2, "corrode", "v. 腐蚀；侵蚀", "Salt water can corrode metal.", "盐水会腐蚀金属。"),
            w(2, "corrupt", "adj. 腐败的；v. 使腐化", "The corrupt official was arrested.", "那个腐败的官员被逮捕了。"),
            w(2, "counsel", "n. 劝告；法律顾问", "He sought legal counsel.", "他寻求了法律顾问。"),
            w(2, "counterpart", "n. 对应的人或物", "He met his counterpart from another company.", "他会见了另一家公司的对等人员。"),
            w(2, "crucial", "adj. 关键的；决定性的", "This is a crucial moment in history.", "这是历史上的关键时刻。"),
            w(2, "cumulative", "adj. 累积的", "The cumulative effect was significant.", "累积的效果是显著的。"),
            w(2, "currency", "n. 货币；流通", "The euro is the currency of Europe.", "欧元是欧洲的货币。")
        );

        // 考研 (bookId=3)
        List<Word> kaoyan = Arrays.asList(
            w(3, "abdicate", "v. 退位；放弃(职责)", "The king abdicated the throne.", "国王退位了。"),
            w(3, "aberration", "n. 反常；偏差", "The incident was an aberration.", "这个事件是一次反常。"),
            w(3, "abide", "v. 忍受；遵守", "You must abide by the rules.", "你必须遵守规则。"),
            w(3, "abstain", "v. 戒除；弃权", "He abstained from alcohol.", "他戒了酒。"),
            w(3, "acclaim", "v./n. 欢呼；称赞", "The book was acclaimed by critics.", "这本书受到评论家的称赞。"),
            w(3, "accountability", "n. 责任；问责制", "There must be accountability for decisions.", "决策必须有问责制。"),
            w(3, "adept", "adj. 熟练的；擅长的", "She is adept at problem-solving.", "她擅长解决问题。"),
            w(3, "adversity", "n. 逆境；不幸", "He showed courage in the face of adversity.", "他在逆境中表现出勇气。"),
            w(3, "aesthetic", "adj. 美学的；审美的", "The design has great aesthetic appeal.", "这个设计有很好的美学吸引力。"),
            w(3, "affiliate", "v. 使附属；n. 附属机构", "The hospital is affiliated with the university.", "这家医院隶属于大学。"),
            w(3, "agenda", "n. 议程；议事日程", "What's on the agenda for today?", "今天的议程是什么？"),
            w(3, "albeit", "conj. 虽然；即使", "It was an interesting, albeit long, speech.", "虽然很长，但这是个有趣的演讲。"),
            w(3, "altruism", "n. 利他主义", "Altruism is the basis of charity work.", "利他主义是慈善工作的基础。"),
            w(3, "ambivalent", "adj. 矛盾的；模棱两可的", "She felt ambivalent about the decision.", "她对这个决定感到矛盾。"),
            w(3, "ameliorate", "v. 改善；改良", "Measures were taken to ameliorate conditions.", "采取了措施来改善条件。"),
            w(3, "anecdote", "n. 轶事；趣闻", "He told an amusing anecdote.", "他讲了一件有趣的轶事。"),
            w(3, "animosity", "n. 敌意；仇恨", "There was no animosity between them.", "他们之间没有敌意。"),
            w(3, "anomaly", "n. 异常；不规则", "The data showed an anomaly.", "数据显示了一个异常。"),
            w(3, "antagonism", "n. 对抗；敌对", "There is antagonism between the two groups.", "两个团体之间存在对抗。"),
            w(3, "apprehension", "n. 忧虑；理解；逮捕", "She felt apprehension before the exam.", "她在考试前感到忧虑。"),
            w(3, "arduous", "adj. 艰苦的；艰难的", "The journey was long and arduous.", "旅程漫长而艰苦。"),
            w(3, "aristocracy", "n. 贵族；上层社会", "The aristocracy lost power in the revolution.", "贵族在革命中失去了权力。"),
            w(3, "aspire", "v. 渴望；有志于", "She aspires to become a scientist.", "她渴望成为科学家。"),
            w(3, "assiduous", "adj. 刻苦的；勤奋的", "He was assiduous in his studies.", "他学习刻苦。"),
            w(3, "audacious", "adj. 大胆的；无畏的", "The plan was audacious but brilliant.", "这个计划大胆但高明。"),
            w(3, "auspicious", "adj. 吉利的；有利的", "It was an auspicious start to the year.", "这是一年的好兆头。"),
            w(3, "autocratic", "adj. 独裁的；专制的", "The leader ruled in an autocratic manner.", "领导人以专制的方式统治。"),
            w(3, "benevolent", "adj. 仁慈的；慈善的", "She was a benevolent ruler.", "她是一位仁慈的统治者。"),
            w(3, "bolster", "v. 支持；增强", "The evidence bolstered his argument.", "证据支持了他的论点。"),
            w(3, "bourgeois", "adj. 中产阶级的", "They lived a bourgeois lifestyle.", "他们过着中产阶级的生活。"),
            w(3, "candid", "adj. 坦率的；公正的", "He gave a candid opinion on the matter.", "他对此事坦率地发表了意见。"),
            w(3, "capitalism", "n. 资本主义", "Capitalism drives economic growth.", "资本主义推动经济增长。"),
            w(3, "catalyst", "n. 催化剂；促进因素", "The crisis was a catalyst for change.", "危机是变革的催化剂。"),
            w(3, "censure", "v./n. 指责；谴责", "He was censured for his behavior.", "他因行为受到谴责。"),
            w(3, "coalition", "n. 联盟；联合", "A coalition of parties formed the government.", "政党联盟组成了政府。"),
            w(3, "coerce", "v. 强迫；胁迫", "No one should be coerced into working.", "任何人都不应被强迫工作。"),
            w(3, "coherent", "adj. 连贯的；一致的", "The essay was well-structured and coherent.", "文章结构良好且连贯。"),
            w(3, "colonial", "adj. 殖民的；殖民地的", "The country gained independence from colonial rule.", "该国摆脱了殖民统治获得独立。"),
            w(3, "commensurate", "adj. 相称的；相当的", "Salary should be commensurate with experience.", "薪水应该与经验相称。"),
            w(3, "complacent", "adj. 自满的；得意的", "We cannot be complacent about our success.", "我们不能对成功感到自满。"),
            w(3, "concession", "n. 让步；特许权", "Both sides made concessions in the negotiation.", "双方在谈判中都做出了让步。"),
            w(3, "conducive", "adj. 有益的；有助于…的", "Quiet is conducive to study.", "安静有助于学习。"),
            w(3, "confiscate", "v. 没收；充公", "The police confiscated the stolen goods.", "警方没收了被盗物品。"),
            w(3, "connotation", "n. 内涵；隐含意义", "The word has a negative connotation.", "这个词有负面含义。"),
            w(3, "consistency", "n. 一致性；连贯性", "Consistency is key to success.", "一致性是成功的关键。"),
            w(3, "constitution", "n. 宪法；体质；构成", "The constitution guarantees freedom of speech.", "宪法保障言论自由。"),
            w(3, "contemporary", "adj. 当代的；同时代的", "She studies contemporary art.", "她研究当代艺术。"),
            w(3, "contend", "v. 竞争；主张", "He contended that the decision was wrong.", "他主张这个决定是错误的。"),
            w(3, "contingent", "adj. 可能的；偶然的；n. 代表团", "Success is contingent on hard work.", "成功取决于努力工作。"),
            w(3, "convene", "v. 召集；召开", "The committee will convene next Monday.", "委员会将于下周一召开会议。"),
            w(3, "convergence", "n. 汇聚；趋同", "There is a convergence of interests.", "各方利益趋同。"),
            w(3, "conviction", "n. 定罪；坚信", "She spoke with great conviction.", "她带着坚定的信念说话。"),
            w(3, "corroborate", "v. 证实；确证", "The evidence corroborates his story.", "证据证实了他的说法。"),
            w(3, "credibility", "n. 可信度；可靠性", "The scandal damaged his credibility.", "丑闻损害了他的可信度。"),
            w(3, "culminate", "v. 达到顶点；告终", "The event culminated in a grand celebration.", "活动以盛大庆典达到高潮。"),
            w(3, "daunting", "adj. 令人畏惧的", "The task ahead is daunting.", "前面的任务令人畏惧。"),
            w(3, "debilitate", "v. 使虚弱；削弱", "The disease debilitated him.", "疾病使他虚弱。"),
            w(3, "decentralize", "v. 分散；下放(权力)", "The government decentralized decision-making.", "政府下放了决策权。"),
            w(3, "defiance", "n. 违抗；蔑视", "He acted in defiance of the court order.", "他公然违抗法院命令。"),
            w(3, "deficiency", "n. 缺乏；不足；缺陷", "Vitamin deficiency can cause health problems.", "维生素缺乏会导致健康问题。"),
            w(3, "deliberate", "adj. 故意的；深思熟虑的", "It was a deliberate decision, not an accident.", "这是故意的决定，不是意外。"),
            w(3, "democratic", "adj. 民主的；大众的", "We live in a democratic society.", "我们生活在民主社会。"),
            w(3, "denounce", "v. 谴责；告发", "The government denounced the terrorist attack.", "政府谴责了恐怖袭击。"),
            w(3, "depict", "v. 描绘；描述", "The novel depicts life in the 19th century.", "这部小说描绘了19世纪的生活。"),
            w(3, "derive", "v. 源于；获得", "Many English words derive from Latin.", "许多英语单词源于拉丁语。"),
            w(3, "deteriorate", "v. 恶化；退化", "His health deteriorated rapidly.", "他的健康状况迅速恶化。"),
            w(3, "detrimental", "adj. 有害的；不利的", "Smoking is detrimental to your health.", "吸烟对健康有害。"),
            w(3, "devastate", "v. 毁灭；使震惊", "The earthquake devastated the city.", "地震摧毁了这座城市。"),
            w(3, "deviate", "v. 偏离；背离", "We cannot deviate from the plan.", "我们不能偏离计划。"),
            w(3, "differentiate", "v. 区分；使不同", "Can you differentiate between the two?", "你能区分这两者吗？"),
            w(3, "dilemma", "n. 困境；进退两难", "She faced a dilemma about her career.", "她在职业选择上进退两难。"),
            w(3, "diminish", "v. 减少；缩小", "The pain diminished over time.", "疼痛随时间减少了。"),
            w(3, "discrepancy", "n. 差异；不一致", "There is a discrepancy in the accounts.", "账目中存在差异。"),
            w(3, "dismantle", "v. 拆除；解散", "They dismantled the old building.", "他们拆除了旧建筑。"),
            w(3, "disparity", "n. 差距；不平等", "There is a huge disparity in income.", "收入差距巨大。"),
            w(3, "disseminate", "v. 传播；散布", "The organization disseminates health information.", "该组织传播健康信息。"),
            w(3, "dissent", "n./v. 异议；不同意", "There was some dissent among the members.", "成员之间存在一些异议。"),
            w(3, "diverse", "adj. 多样的；不同的", "The city has a diverse population.", "这座城市人口多样化。"),
            w(3, "doctrine", "n. 教义；学说", "He studied the doctrines of Buddhism.", "他研究了佛教教义。"),
            w(3, "domestic", "adj. 国内的；家庭的", "The domestic economy is growing.", "国内经济正在增长。"),
            w(3, "dominate", "v. 支配；主导", "The company dominates the market.", "这家公司主导市场。"),
            w(3, "drastic", "adj. 激烈的；严厉的", "Drastic measures are needed.", "需要采取严厉措施。"),
            w(3, "drought", "n. 干旱", "The drought lasted for several months.", "干旱持续了几个月。"),
            w(3, "dubious", "adj. 可疑的；不确定的", "His explanation seemed dubious.", "他的解释似乎可疑。"),
            w(3, "dynamic", "adj. 动态的；有活力的", "She is a dynamic and inspiring leader.", "她是一位有活力、鼓舞人心的领导。"),
            w(3, "eclipse", "v. 使黯然失色；n. 日食", "His performance eclipsed everyone else's.", "他的表现使其他人黯然失色。"),
            w(3, "egalitarian", "adj. 平等主义的", "They advocated an egalitarian society.", "他们提倡平等主义社会。"),
            w(3, "elaborate", "adj. 精心制作的；v. 详细阐述", "Could you elaborate on your plan?", "你能详细说明你的计划吗？"),
            w(3, "elevate", "v. 提高；提升", "Hard work elevated her to the top position.", "勤奋工作使她升到了最高位置。"),
            w(3, "eloquent", "adj. 雄辩的；有口才的", "He delivered an eloquent speech.", "他发表了一篇雄辩的演讲。"),
            w(3, "embody", "v. 体现；包含", "The statue embodies the spirit of freedom.", "这座雕像体现了自由精神。"),
            w(3, "empirical", "adj. 经验主义的；实证的", "The theory is based on empirical evidence.", "这个理论基于实证证据。"),
            w(3, "empower", "v. 赋予权力；使能够", "Education empowers people to succeed.", "教育赋予人们成功的能力。"),
            w(3, "endeavor", "n./v. 努力；尽力", "We must endeavor to improve our service.", "我们必须努力改善服务。"),
            w(3, "enhance", "v. 提高；增强", "Exercise enhances physical fitness.", "锻炼增强体质。"),
            w(3, "equity", "n. 公平；公正", "Social equity is an important goal.", "社会公平是一个重要目标。"),
            w(3, "erode", "v. 侵蚀；腐蚀", "Trust has been eroded by the scandal.", "信任被丑闻侵蚀了。")
        );

        // 高中 (bookId=4)
        List<Word> highSchool = Arrays.asList(
            w(4, "absorb", "v. 吸收；吸引", "Plants absorb energy from the sun.", "植物吸收太阳的能量。"),
            w(4, "abundant", "adj. 丰富的；充裕的", "The region is abundant in wildlife.", "这个地区野生动物丰富。"),
            w(4, "academy", "n. 学院；学会", "He studied at a military academy.", "他在一所军事学院学习。"),
            w(4, "accomplish", "v. 完成；达到", "She accomplished her goal of graduation.", "她完成了毕业目标。"),
            w(4, "adolescent", "n. 青少年", "Adolescents face many challenges.", "青少年面临许多挑战。"),
            w(4, "adventure", "n. 冒险；奇遇", "The trip was a great adventure.", "这次旅行是一次很棒的冒险。"),
            w(4, "amusement", "n. 娱乐；消遣", "They went to an amusement park.", "他们去了游乐园。"),
            w(4, "anniversary", "n. 周年纪念日", "Today is their wedding anniversary.", "今天是他们的结婚纪念日。"),
            w(4, "appetite", "n. 食欲；胃口", "He has a healthy appetite.", "他有健康的食欲。"),
            w(4, "application", "n. 应用；申请", "I submitted my application for the job.", "我提交了工作申请。"),
            w(4, "appreciate", "v. 欣赏；感激", "I appreciate everything you've done.", "我感激你做的一切。"),
            w(4, "atmosphere", "n. 大气；气氛", "The atmosphere in the classroom was tense.", "教室里的气氛很紧张。"),
            w(4, "attitude", "n. 态度；看法", "Your attitude determines your success.", "你的态度决定你的成功。"),
            w(4, "audience", "n. 观众；听众", "The audience clapped enthusiastically.", "观众热情地鼓掌。"),
            w(4, "bargain", "n. 便宜货；v. 讨价还价", "I got this jacket at a bargain price.", "这件夹克我买得很便宜。"),
            w(4, "beneath", "prep. 在…下方", "The boat sank beneath the waves.", "船沉入海浪下。"),
            w(4, "bother", "v. 打扰；烦恼", "Don't bother me while I'm working.", "我工作时别打扰我。"),
            w(4, "boundary", "n. 边界；界限", "The river forms the boundary between the two countries.", "这条河是两国的边界。"),
            w(4, "brilliant", "adj. 杰出的；明亮的", "That's a brilliant idea!", "那是个绝妙的主意！"),
            w(4, "broadcast", "v./n. 广播；播放", "The game was broadcast live on TV.", "比赛在电视上直播。"),
            w(4, "calculate", "v. 计算；估计", "Can you calculate the total cost?", "你能计算总成本吗？"),
            w(4, "campaign", "n. 运动；战役", "They launched a campaign against smoking.", "他们发起了禁烟运动。"),
            w(4, "capable", "adj. 有能力的", "She is capable of leading the team.", "她有能力领导这个团队。"),
            w(4, "celebrate", "v. 庆祝；赞美", "We celebrated her birthday with a party.", "我们举办派对庆祝她的生日。"),
            w(4, "ceremony", "n. 典礼；仪式", "The graduation ceremony was memorable.", "毕业典礼令人难忘。"),
            w(4, "character", "n. 性格；角色；品德", "Honesty is an important character trait.", "诚实是重要的品格特征。"),
            w(4, "circumstance", "n. 环境；情况", "Under the circumstances, we had no choice.", "在那种情况下，我们别无选择。"),
            w(4, "civilization", "n. 文明；文化", "Ancient Egypt had a great civilization.", "古埃及有伟大的文明。"),
            w(4, "comfortable", "adj. 舒适的", "This chair is very comfortable.", "这把椅子很舒服。"),
            w(4, "communicate", "v. 沟通；交流", "We communicate through social media.", "我们通过社交媒体交流。"),
            w(4, "companion", "n. 同伴；伙伴", "My dog is my faithful companion.", "我的狗是我忠实的伙伴。"),
            w(4, "compete", "v. 竞争；比赛", "Athletes from many countries compete in the Olympics.", "来自许多国家的运动员在奥运会上竞争。"),
            w(4, "concentrate", "v. 集中；专心", "I need to concentrate on my homework.", "我需要专心做作业。"),
            w(4, "conduct", "v. 进行；指挥；n. 行为", "He conducted the orchestra beautifully.", "他精彩地指挥了管弦乐队。"),
            w(4, "confident", "adj. 自信的；确信的", "Be confident in your abilities.", "对自己的能力要有信心。"),
            w(4, "consequence", "n. 结果；后果", "Think about the consequences before you act.", "行动前先考虑后果。"),
            w(4, "conservation", "n. 保护；保存", "Wildlife conservation is important.", "野生动物保护很重要。"),
            w(4, "considerate", "adj. 体贴的；考虑周到的", "She is always considerate of others.", "她总是为他人着想。"),
            w(4, "consist", "v. 由…组成；在于", "The team consists of five members.", "这个团队由五名成员组成。"),
            w(4, "contemporary", "adj. 当代的", "She enjoys contemporary music.", "她喜欢当代音乐。"),
            w(4, "contribute", "v. 贡献；捐助", "Everyone should contribute to the team.", "每个人都应该为团队做贡献。"),
            w(4, "convenient", "adj. 方便的", "Online shopping is very convenient.", "网上购物非常方便。"),
            w(4, "convince", "v. 说服；使确信", "He convinced me to change my mind.", "他说服我改变了主意。"),
            w(4, "cooperate", "v. 合作；协作", "Let's cooperate to finish the project.", "让我们合作完成这个项目。"),
            w(4, "courage", "n. 勇气；胆量", "She showed great courage in the face of danger.", "她在危险面前表现出巨大的勇气。"),
            w(4, "crash", "n./v. 碰撞；坠毁", "The car crashed into a tree.", "汽车撞到了树上。"),
            w(4, "curiosity", "n. 好奇心；求知欲", "Curiosity is the best teacher.", "好奇心是最好的老师。"),
            w(4, "current", "adj. 当前的；流通的", "What is your current address?", "你当前的地址是什么？"),
            w(4, "deadline", "n. 截止日期", "The deadline for the report is Friday.", "报告的截止日期是周五。"),
            w(4, "declare", "v. 宣布；声明", "The president declared a state of emergency.", "总统宣布了紧急状态。"),
            w(4, "define", "v. 定义；明确", "How do you define success?", "你如何定义成功？"),
            w(4, "deliberate", "adj. 故意的；谨慎的", "It was a deliberate choice.", "这是一个有意为之的选择。"),
            w(4, "deliver", "v. 递送；发表；接生", "The package was delivered this morning.", "包裹今天早上送到了。"),
            w(4, "demonstrate", "v. 证明；演示", "Let me demonstrate how the machine works.", "让我演示一下这台机器如何运作。"),
            w(4, "depart", "v. 离开；出发", "The train departs at noon.", "火车中午出发。"),
            w(4, "deserve", "v. 值得；应得", "You deserve a reward for your efforts.", "你的努力值得奖励。"),
            w(4, "desire", "n./v. 渴望；欲望", "She has a strong desire to learn.", "她有强烈的学习欲望。"),
            w(4, "desperate", "adj. 绝望的；不顾一切的", "They were desperate for help.", "他们迫切需要帮助。"),
            w(4, "destination", "n. 目的地；终点", "We reached our destination safely.", "我们安全到达了目的地。"),
            w(4, "determine", "v. 决定；确定", "Age should not determine your ability.", "年龄不应该决定你的能力。"),
            w(4, "devote", "v. 致力于；奉献", "She devoted her life to teaching.", "她毕生致力于教学。"),
            w(4, "dignity", "n. 尊严；高贵", "Everyone deserves to be treated with dignity.", "每个人都有权得到有尊严的对待。"),
            w(4, "discipline", "n. 纪律；学科", "Self-discipline is the key to success.", "自律是成功的关键。"),
            w(4, "discount", "n. 折扣；v. 打折", "They gave me a 20% discount.", "他们给了我八折优惠。"),
            w(4, "distinguish", "v. 区分；辨别", "Can you distinguish right from wrong?", "你能分辨是非吗？"),
            w(4, "distribute", "v. 分配；分发", "The charity distributes food to the poor.", "慈善机构向穷人分发食物。"),
            w(4, "document", "n. 文件；文档；v. 记录", "Please sign this document.", "请签署这份文件。"),
            w(4, "domestic", "adj. 国内的；家庭的", "She enjoys domestic life.", "她享受家庭生活。"),
            w(4, "economy", "n. 经济；节约", "The global economy faces challenges.", "全球经济面临挑战。"),
            w(4, "efficient", "adj. 高效的", "This is an efficient way to work.", "这是一种高效的工作方式。"),
            w(4, "eliminate", "v. 消除；淘汰", "We must eliminate all errors.", "我们必须消除所有错误。"),
            w(4, "embrace", "v. 拥抱；包含；欣然接受", "We should embrace new technologies.", "我们应该欣然接受新技术。"),
            w(4, "emerge", "v. 出现；浮现", "New problems emerged during the test.", "测试期间出现了新问题。"),
            w(4, "enable", "v. 使能够；使可能", "Technology enables us to communicate instantly.", "技术使我们能够即时沟通。"),
            w(4, "encounter", "v./n. 遇到；遭遇", "We encountered many difficulties on the way.", "我们在路上遇到了许多困难。"),
            w(4, "encourage", "v. 鼓励；激励", "Teachers should encourage creativity.", "老师应该鼓励创造力。"),
            w(4, "enormous", "adj. 巨大的；庞大的", "The project required an enormous amount of work.", "这个项目需要大量的工作。"),
            w(4, "enthusiasm", "n. 热情；热忱", "She shows great enthusiasm for art.", "她对艺术表现出极大的热情。"),
            w(4, "essential", "adj. 必要的；本质的", "Water is essential for life.", "水是生命所必需的。"),
            w(4, "establish", "v. 建立；设立", "The company was established in 1990.", "这家公司成立于1990年。"),
            w(4, "evidence", "n. 证据；迹象", "There is no evidence to support his claim.", "没有证据支持他的说法。"),
            w(4, "exaggerate", "v. 夸张；夸大", "Don't exaggerate the problem.", "不要夸大这个问题。"),
            w(4, "exchange", "n./v. 交换；交流", "We exchanged ideas at the meeting.", "我们在会议上交流了想法。"),
            w(4, "exist", "v. 存在；生存", "Does life exist on other planets?", "其他星球上存在生命吗？"),
            w(4, "expand", "v. 扩大；扩展", "The company plans to expand overseas.", "公司计划扩展到海外。"),
            w(4, "explore", "v. 探索；探究", "We explored the ancient ruins.", "我们探索了古代遗迹。"),
            w(4, "extreme", "adj. 极端的；极度的", "Extreme sports are becoming popular.", "极限运动越来越流行。"),
            w(4, "familiar", "adj. 熟悉的", "This song sounds familiar.", "这首歌听起来很熟悉。"),
            w(4, "flexible", "adj. 灵活的；有弹性的", "We offer flexible working hours.", "我们提供弹性工作时间。"),
            w(4, "frequent", "adj. 频繁的；经常的", "She is a frequent visitor to the museum.", "她是博物馆的常客。"),
            w(4, "genuine", "adj. 真正的；真诚的", "Is this a genuine diamond?", "这是真钻石吗？"),
            w(4, "gradual", "adj. 逐渐的", "There has been a gradual improvement.", "已经有了逐步改善。"),
            w(4, "guarantee", "n./v. 保证；担保", "We guarantee the quality of our products.", "我们保证产品质量。"),
            w(4, "harmony", "n. 和谐；融洽", "They live in harmony with nature.", "他们与自然和谐相处。"),
            w(4, "hesitate", "v. 犹豫；迟疑", "Don't hesitate to ask questions.", "不要犹豫提问。"),
            w(4, "highlight", "v. 强调；突出", "The report highlights the key issues.", "报告突出了关键问题。"),
            w(4, "identity", "n. 身份；特性", "He is searching for his cultural identity.", "他在寻找自己的文化身份。"),
            w(4, "ignore", "v. 忽视；不理睬", "You can't ignore the facts.", "你不能忽视事实。"),
            w(4, "illustrate", "v. 说明；阐释", "Let me illustrate my point with an example.", "让我用一个例子说明我的观点。"),
            w(4, "inspire", "v. 激励；启发", "Her story inspired many young people.", "她的故事激励了许多年轻人。")
        );

        cet4.forEach(wordDao::insert);
        cet6.forEach(wordDao::insert);
        kaoyan.forEach(wordDao::insert);
        highSchool.forEach(wordDao::insert);
    }
}
