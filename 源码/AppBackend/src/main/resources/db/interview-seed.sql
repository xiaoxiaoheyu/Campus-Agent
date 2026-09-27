-- AI 面试示例数据（岗位技术栈 + 题库）
-- 库：smart-campus
-- 可重复执行：按岗位名 / 题目内容去重后再插入

SET NAMES utf8mb4;

-- ========== 岗位技术栈 ==========
INSERT INTO interview_job_tech_stack (job_position, tech_stack, status, remark, created_at, updated_at)
SELECT * FROM (
  SELECT 'Java 后端' AS job_position, 'Java, Spring Boot, MySQL, Redis, MyBatis, JVM' AS tech_stack, 1 AS status, '后端开发方向' AS remark, NOW() AS created_at, NOW() AS updated_at
  UNION ALL SELECT '前端开发', 'HTML, CSS, JavaScript, Vue3, TypeScript, Vite', 1, 'Web 前端方向', NOW(), NOW()
  UNION ALL SELECT '测试开发', 'Java/Python, Selenium, JMeter, SQL, 接口测试', 1, '测试与质量保障', NOW(), NOW()
  UNION ALL SELECT '嵌入式开发', 'C/C++, Linux, RTOS, 单片机, 驱动开发', 1, '嵌入式方向', NOW(), NOW()
  UNION ALL SELECT '产品经理', '需求分析, 原型设计, 数据分析, 项目管理', 1, '产品岗位', NOW(), NOW()
) AS t
WHERE NOT EXISTS (
  SELECT 1 FROM interview_job_tech_stack j WHERE j.job_position = t.job_position
);

-- ========== 题库 ==========
INSERT INTO knowledge_base (
  job_position, question_type, question, excellent_answer, difficulty, keywords,
  question_intent, answer_points, score_standard, suitable_level, remark, status, in_chroma, created_at, updated_at
)
SELECT * FROM (
  SELECT
    'Java 后端' AS job_position,
    '技术面' AS question_type,
    '请解释 HashMap 的底层实现，以及 JDK8 之后做了哪些优化？' AS question,
    'HashMap 基于数组 + 链表/红黑树。put 时通过 hash 定位桶；链表过长（默认 8）且容量 ≥64 时转红黑树。JDK8 用尾插、树化优化冲突，扩容时高低位拆分减少 rehash 成本。' AS excellent_answer,
    3 AS difficulty,
    'HashMap,哈希,红黑树,JDK8' AS keywords,
    '考察集合原理与 JDK 演进理解' AS question_intent,
    '1) 数组+链表结构 2) hash 扰动 3) 树化阈值与条件 4) JDK8 尾插与扩容优化' AS answer_points,
    '能讲清结构与冲突处理 60 分；能说清树化与扩容细节 80 分；能对比并发风险与 ConcurrentHashMap 90 分+' AS score_standard,
    '中级' AS suitable_level,
    '示例题' AS remark,
    1 AS status,
    0 AS in_chroma,
    NOW() AS created_at,
    NOW() AS updated_at
  UNION ALL SELECT
    'Java 后端', '技术面',
    'Spring Boot 自动配置的原理是什么？',
    '通过 @EnableAutoConfiguration 导入 AutoConfigurationImportSelector，读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports（或旧版 spring.factories），结合 @ConditionalOn* 按条件装配 Bean。',
    3, 'SpringBoot,自动配置,Conditional',
    '考察框架原理',
    '1) 启用自动配置入口 2) 配置类加载来源 3) Conditional 条件装配 4) 自定义 starter 思路',
    '说出入口与加载机制 70 分；能讲 Conditional 与自定义 starter 90 分',
    '中级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    'Java 后端', '技术面',
    'MySQL 索引失效有哪些常见场景？如何优化慢查询？',
    '失效场景：函数/运算包裹列、隐式类型转换、左右模糊、or 导致索引无法合并、违背最左前缀等。优化：explain 看 type/key/rows，建合适联合索引，避免 select *，控制回表，必要时改写 SQL。',
    4, 'MySQL,索引,慢查询,explain',
    '考察数据库调优能力',
    '1) 常见失效场景 2) explain 关键字段 3) 索引设计 4) SQL 改写',
    '列举 ≥3 个失效场景 60 分；结合 explain 给出优化方案 85 分',
    '高级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    'Java 后端', '行为面',
    '描述一次你排查线上故障的经历，用 STAR 原则说明。',
    'Situation：高峰期接口超时；Task：定位并恢复；Action：看监控与日志、定位慢 SQL/锁、限流回滚；Result：恢复服务并沉淀告警与预案。',
    2, 'STAR,故障排查,线上问题',
    '考察问题解决与表达',
    '情境清晰、行动具体、结果可量化、复盘改进',
    '结构完整 70 分；有数据与复盘 90 分',
    '初级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    '前端开发', '技术面',
    'Vue3 的响应式原理与 Vue2 有何不同？',
    'Vue2 用 Object.defineProperty 拦截属性；Vue3 用 Proxy 代理整对象，支持新增/删除属性、Map/Set，配合 Reflect，性能与能力更好。',
    3, 'Vue3,Proxy,响应式',
    '考察框架核心原理',
    '1) defineProperty vs Proxy 2) 新增属性问题 3) ref/reactive 4) effect 依赖收集',
    '对比清楚 70 分；能讲依赖收集与调度 90 分',
    '中级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    '前端开发', '技术面',
    '浏览器从输入 URL 到页面展示经历了哪些主要步骤？',
    'DNS → TCP/TLS → 发请求 → 收响应 → 解析 HTML 建 DOM → CSSOM → Render Tree → Layout → Paint → Composite；期间可能有缓存、重定向、预连接等。',
    2, '浏览器,渲染,网络',
    '考察基础功底',
    '网络阶段 + 渲染流水线 + 可优化点',
    '主流程完整 75 分；提到关键路径优化 90 分',
    '初级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    '前端开发', '技术面',
    '如何理解和解决前端内存泄漏？',
    '常见原因：未清理定时器/监听器、闭包引用 DOM、全局缓存无限增长。用 Performance/Memory 面板看 heap，及时 removeEventListener、abort 请求、弱引用或显式释放。',
    4, '内存泄漏,Performance,闭包',
    '考察工程排障能力',
    '原因分类、排查工具、修复手段',
    '能举真实案例并说明排查步骤 85 分+',
    '高级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    '测试开发', '技术面',
    '接口自动化测试框架你会怎么设计？',
    '分层：用例层、请求封装、断言、数据驱动、环境配置、报告与 CI。关注鉴权复用、幂等数据、失败重试与清晰断言信息。',
    3, '接口测试,自动化,CI',
    '考察测试架构思维',
    '分层设计、数据隔离、断言策略、CI 集成',
    '结构合理 70 分；有落地细节 90 分',
    '中级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    '测试开发', '技术面',
    '如何做接口性能测试？关注哪些指标？',
    '明确目标与场景（峰值/稳定性），用 JMeter/k6 压测；关注 QPS、RT、错误率、资源水位；分析瓶颈后给优化建议并回归验证。',
    3, '性能测试,JMeter,QPS',
    '考察性能质量保障',
    '场景设计、指标、瓶颈分析、结论闭环',
    '指标齐全且有分析方法 80 分+',
    '中级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    '嵌入式开发', '技术面',
    '请说明中断与轮询的区别，以及中断中要注意什么？',
    '轮询主动查询，中断由事件触发。中断里应短小快速，避免阻塞与复杂逻辑，注意临界区保护、可重入与优先级。',
    3, '中断,轮询,临界区',
    '考察嵌入式基础',
    '区别、优缺点、中断处理约束',
    '概念正确 70 分；提到临界区/优先级 85 分',
    '中级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    '嵌入式开发', '技术面',
    '如何排查嵌入式设备偶发死机问题？',
    '抓现场：看门狗日志、堆栈、寄存器；排查栈溢出、野指针、中断竞态、电源与散热；用断言/日志/复现脚本缩小范围。',
    4, '死机,看门狗,调试',
    '考察嵌入式排障',
    '现场保全、常见根因、工具链、复现策略',
    '有系统排查思路 80 分+',
    '高级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    '产品经理', '行为面',
    '如何做需求优先级排序？请举例。',
    '结合用户价值、业务目标、实现成本与风险，可用 RICE/Kano/MoSCoW。举例：核心转化链路优先，体验优化按影响面分批。',
    2, '需求优先级,RICE,Kano',
    '考察产品决策方法',
    '框架 + 指标 + 案例',
    '有方法有例子 80 分',
    '初级', '示例题', 1, 0, NOW(), NOW()
  UNION ALL SELECT
    '产品经理', '综合面',
    '如果你负责校园二手交易功能，你会如何设计 MVP？',
    'MVP 聚焦：发帖、浏览筛选、站内沟通、线下成交确认；暂不做复杂担保支付。用发布量、成交率、投诉率验证。',
    3, 'MVP,校园,二手交易',
    '考察产品设计与取舍',
    '用户路径、功能边界、验证指标、风险控制',
    '取舍清晰且可验证 85 分+',
    '中级', '示例题', 1, 0, NOW(), NOW()
) AS q
WHERE NOT EXISTS (
  SELECT 1 FROM knowledge_base k
  WHERE k.job_position = q.job_position AND k.question = q.question
);
