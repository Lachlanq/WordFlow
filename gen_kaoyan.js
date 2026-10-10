const fs = require('fs');

const data = `["abdicate","/ˈæbdɪkeɪt/","/ˈæbdɪkeɪt/","v. 退位；放弃(职责)","","abdicate responsibility 放弃责任","resign, renounce","The king abdicated the throne.","国王退位了。"]
["aberration","/ˌæbəˈreɪʃn/","/ˌæbəˈreɪʃn/","n. 异常；偏差；过失","","a statistical aberration 统计偏差","anomaly, deviation","The warm weather in winter is an aberration.","冬天温暖的天气是一种异常现象。"]
["abide","/əˈbaɪd/","/əˈbaɪd/","v. 遵守；忍受；持续","","abide by the rules 遵守规则","obey, tolerate","You must abide by the contract terms.","你必须遵守合同条款。"]
["abolish","/əˈbɒlɪʃ/","/əˈbɑːlɪʃ/","v. 废除；取消","","abolish slavery 废除奴隶制","eliminate, annul","The government decided to abolish the outdated law.","政府决定废除这项过时的法律。"]
["abstain","/əbˈsteɪn/","/əbˈsteɪn/","v. 弃权；戒除","","abstain from voting 投弃权票","refrain, desist","They chose to abstain from the vote.","他们选择在投票中弃权。"]
["acclaim","/əˈkleɪm/","/əˈkleɪm/","v./n. 欢呼；喝彩；称赞","","critical acclaim 好评如潮","applaud, praise","The novel received widespread acclaim.","这部小说获得了广泛的赞誉。"]
["accountability","/əˌkaʊntəˈbɪləti/","/əˌkaʊntəˈbɪləti/","n. 问责制；有责任","","government accountability 政府问责制","responsibility, liability","Transparency and accountability are essential for democracy.","透明度和问责制对民主至关重要。"]
["acumen","/ˈækjəmən/","/əˈkjuːmən/","n. 敏锐；聪明","","business acumen 商业头脑","insight, shrewdness","Her business acumen helped the company thrive.","她的商业头脑帮助公司蓬勃发展。"]
["adept","/əˈdept/","/əˈdept/","adj. 熟练的；擅长的","","adept at negotiation 擅长谈判","skilled, proficient","She is adept at solving complex problems.","她善于解决复杂的问题。"]
["adhere","/ədˈhɪə(r)/","/ədˈhɪr/","v. 坚持；遵守；粘附","","adhere to principles 坚持原则","stick, comply","We must adhere to the safety regulations.","我们必须遵守安全规定。"]
["adjudicate","/əˈdʒuːdɪkeɪt/","/əˈdʒuːdɪkeɪt/","v. 裁决；判决","","adjudicate a dispute 裁决争端","judge, arbitrate","The court will adjudicate the case next month.","法院将于下个月裁决此案。"]
["admonish","/ədˈmɒnɪʃ/","/ədˈmɑːnɪʃ/","v. 告诫；警告","","admonish sb for sth 因某事告诫某人","reprimand, warn","The teacher admonished the student for cheating.","老师因作弊告诫了那名学生。"]
["adorn","/əˈdɔːn/","/əˈdɔːrn/","v. 装饰；佩戴","","adorn with jewels 用珠宝装饰","decorate, embellish","Flowers adorned the wedding hall.","鲜花装饰了婚礼大厅。"]
["adversary","/ˈædvəsəri/","/ˈædvərseri/","n. 对手；敌手","","a formidable adversary 强大的对手","opponent, foe","He regarded his rival as a worthy adversary.","他将对手视为值得尊敬的敌人。"]
["adversity","/ədˈvɜːsəti/","/ədˈvɜːrsəti/","n. 逆境；不幸","","overcome adversity 克服逆境","hardship, misfortune","She showed great courage in the face of adversity.","她在逆境中表现出极大的勇气。"]
["aesthetic","/iːsˈθetɪk/","/esˈθetɪk/","adj. 审美的；美学的","n. 审美观","aesthetic value 审美价值","artistic, tasteful","The design combines functionality with aesthetic appeal.","这个设计将功能性与审美吸引力结合在一起。"]
["affiliate","/əˈfɪlieɪt/","/əˈfɪlieɪt/","v. 使附属；加入 n. 附属机构","","affiliate with an organization 加入组织","associate, attach","The hospital is affiliated with the university.","这家医院附属于该大学。"]
["afflict","/əˈflɪkt/","/əˈflɪkt/","v. 折磨；使痛苦","","afflicted with disease 遭受疾病折磨","torment, plague","The region is afflicted by severe drought.","该地区遭受严重干旱的折磨。"]
["agenda","/əˈdʒendə/","/əˈdʒendə/","n. 议程；议事日程","","political agenda 政治议程","schedule, program","Climate change is high on the agenda.","气候变化是议程上的重要议题。"]
["albeit","/ˌɔːlˈbiːɪt/","/ˌɔːlˈbiːɪt/","conj. 虽然；尽管","","albeit reluctantly 尽管不情愿","although, though","He accepted the decision, albeit with reservations.","他接受了这个决定，尽管有所保留。"]
["alienate","/ˈeɪliəneɪt/","/ˈeɪliəneɪt/","v. 疏远；离间","","alienate supporters 疏远支持者","estrange, isolate","His arrogant attitude alienated his colleagues.","他傲慢的态度疏远了同事。"]
["allegiance","/əˈliːdʒəns/","/əˈliːdʒəns/","n. 忠诚；效忠","","swear allegiance to 宣誓效忠于","loyalty, fidelity","They pledged allegiance to the new government.","他们宣誓效忠新政府。"]
["alleviate","/əˈliːvieɪt/","/əˈliːvieɪt/","v. 减轻；缓和","","alleviate poverty 减轻贫困","relieve, ease","The medicine helped alleviate her pain.","药物帮助减轻了她的疼痛。"]
["altruism","/ˈæltruɪzəm/","/ˈæltruɪzəm/","n. 利他主义；无私","","act of altruism 利他行为","selflessness, benevolence","True altruism expects nothing in return.","真正的利他主义不求任何回报。"]
["amalgamate","/əˈmælɡəmeɪt/","/əˈmælɡəmeɪt/","v. 合并；融合","","amalgamate two companies 合并两家公司","merge, combine","The two departments were amalgamated into one.","两个部门合并成了一个。"]
["ambivalent","/æmˈbɪvələnt/","/æmˈbɪvələnt/","adj. 矛盾的；摇摆不定的","","ambivalent feelings 矛盾的情感","conflicted, uncertain","She felt ambivalent about the job offer.","她对这份工作邀请感到矛盾。"]
["ameliorate","/əˈmiːliəreɪt/","/əˈmiːliəreɪt/","v. 改善；改良","","ameliorate living conditions 改善生活条件","improve, enhance","Steps have been taken to ameliorate the situation.","已采取措施改善局势。"]
["amend","/əˈmend/","/əˈmend/","v. 修改；修订；改正","","amend the constitution 修改宪法","revise, modify","Congress voted to amend the bill.","国会投票修改该法案。"]
["amenity","/əˈmiːnəti/","/əˈmenəti/","n. 便利设施；舒适","","public amenities 公共设施","facility, convenience","The hotel offers excellent amenities.","这家酒店提供优质的设施。"]
["amiable","/ˈeɪmiəbl/","/ˈeɪmiəbl/","adj. 和蔼可亲的；友善的","","an amiable personality 和蔼可亲的性格","friendly, genial","He was an amiable host who made everyone welcome.","他是一位和蔼可亲的主人，让每个人都感到受欢迎。"]
["amicable","/ˈæmɪkəbl/","/ˈæmɪkəbl/","adj. 友好的；和睦的","","amicable settlement 友好解决","cordial, harmonious","They reached an amicable agreement.","他们达成了友好的协议。"]
["amnesty","/ˈæmnəsti/","/ˈæmnəsti/","n. 大赦；特赦","","grant amnesty to prisoners 赦免囚犯","pardon, clemency","The president declared a general amnesty.","总统宣布了大赦。"]
["analogy","/əˈnælədʒi/","/əˈnælədʒi/","n. 类比；比喻","","draw an analogy between 在…之间做类比","comparison, metaphor","The teacher used an analogy to explain the concept.","老师用类比来解释这个概念。"]
["anarchist","/ˈænəkɪst/","/ˈænərkɪst/","n. 无政府主义者","","anarchist movement 无政府主义运动","rebel, nihilist","The anarchist opposed all forms of government.","无政府主义者反对一切形式的政府。"]
["anecdote","/ˈænɪkdəʊt/","/ˈænɪkdoʊt/","n. 轶事；趣闻","","a humorous anecdote 幽默轶事","story, tale","He told an amusing anecdote about his travels.","他讲了一个关于他旅行的有趣轶事。"]
["animosity","/ˌænɪˈmɒsəti/","/ˌænɪˈmɑːsəti/","n. 敌意；仇恨","","harbor animosity toward 对…怀有敌意","hostility, enmity","There is deep animosity between the rival factions.","敌对派系之间存在着深深的敌意。"]
["anomaly","/əˈnɒməli/","/əˈnɑːməli/","n. 异常；不规则","","a statistical anomaly 统计异常","irregularity, aberration","The scientist investigated the anomaly in the data.","科学家调查了数据中的异常现象。"]
["antagonism","/ænˈtæɡənɪzəm/","/ænˈtæɡənɪzəm/","n. 对抗；敌对","","class antagonism 阶级对抗","opposition, conflict","The antagonism between the two groups intensified.","两个群体之间的对抗加剧了。"]
["anthology","/ænˈθɒlədʒi/","/ænˈθɑːlədʒi/","n. 选集；文集","","a poetry anthology 诗歌选集","collection, compilation","She published an anthology of modern poetry.","她出版了一本现代诗歌选集。"]
["apparatus","/ˌæpəˈreɪtəs/","/ˌæpəˈrætəs/","n. 装置；设备；机构","","state apparatus 国家机器","equipment, mechanism","The laboratory has sophisticated apparatus for experiments.","实验室有用于实验的精密仪器。"]
["appease","/əˈpiːz/","/əˈpiːz/","v. 安抚；平息；姑息","","appease public anger 平息公众愤怒","pacify, placate","The government tried to appease the protesters.","政府试图安抚抗议者。"]
["apprehension","/ˌæprɪˈhenʃn/","/ˌæprɪˈhenʃn/","n. 忧虑；理解；逮捕","","fill with apprehension 充满忧虑","anxiety, dread","She looked at the future with apprehension.","她怀着忧虑的心情看待未来。"]
["arduous","/ˈɑːdjuəs/","/ˈɑːrdʒuəs/","adj. 艰巨的；费力的","","an arduous journey 艰苦的旅程","strenuous, grueling","The climb was long and arduous.","攀登漫长而艰辛。"]
["aristocracy","/ˌærɪˈstɒkrəsi/","/ˌærɪˈstɑːkrəsi/","n. 贵族；贵族阶级","","landed aristocracy 地主贵族","nobility, elite","The aristocracy lost much of its power after the revolution.","革命后贵族阶级失去了大部分权力。"]
["articulate","/ɑːˈtɪkjuleɪt/","/ɑːrˈtɪkjuleɪt/","adj. 善于表达的 v. 清楚表达","","articulate one's thoughts 表达想法","eloquent, expressive","She is an articulate speaker who captivates the audience.","她是一位善于表达的演讲者，吸引了听众。"]
["ascertain","/ˌæsəˈteɪn/","/ˌæsərˈteɪn/","v. 查明；确定","","ascertain the facts 查明事实","determine, verify","The police are trying to ascertain the cause of death.","警方正在努力查明死因。"]
["aspire","/əˈspaɪə(r)/","/əˈspaɪər/","v. 渴望；立志","","aspire to greatness 渴望伟大","strive, yearn","Many young people aspire to become entrepreneurs.","许多年轻人渴望成为企业家。"]
["assail","/əˈseɪl/","/əˈseɪl/","v. 攻击；质问；困扰","","assail with questions 用问题质问","attack, assault","Reporters assailed the politician with tough questions.","记者用尖锐的问题质问这位政治家。"]
["assent","/əˈsent/","/əˈsent/","v./n. 同意；赞成","","give one's assent 表示同意","agree, consent","The board gave its assent to the proposal.","董事会同意了该提案。"]
["assiduous","/əˈsɪdjuəs/","/əˈsɪdʒuəs/","adj. 勤奋的；刻苦的","","assiduous research 勤奋的研究","diligent, industrious","Her assiduous efforts led to a breakthrough discovery.","她勤奋的努力带来了突破性的发现。"]`;

const keys = ["word","phoneticUk","phoneticUs","mainMeaning","subMeaning","collocation","synonym","exampleEn","exampleCn"];
const lines = data.trim().split("\n");
const result = lines.map(line => {
  const arr = JSON.parse(line);
  const obj = {};
  keys.forEach((k, i) => obj[k] = arr[i]);
  return obj;
});

const outPath = 'C:/Users/LENOVO/AppData/Roaming/TRAE SOLO CN/ModularData/ai-agent/work-mode-projects/6a1ee03cb57a25ab8550513e/WordFlow/app/src/main/assets/seed_kaoyan.json';
fs.writeFileSync(outPath, JSON.stringify(result, null, 2), 'utf-8');
console.log(`Written ${result.length} words to seed_kaoyan.json`);
