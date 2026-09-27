-- AgentA3 试卷生成联调数据。
-- 幂等设计：按稳定名称和题目内容判断，重复执行不会产生重复数据。
-- 默认绑定演示学生账号：zzs（通常 user_id = 4，脚本会按 username 动态解析）。
--
-- 使用方式（在 MySQL 所在环境）：
--   mysql -uroot -p smart-campus < deploy/seed-paper-test-data.sql
-- 或 Docker：
--   docker exec -i <mysql容器名> mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart-campus < deploy/seed-paper-test-data.sql
--
-- 小程序登录账号：zzs / admin123，然后打开「试卷生成」即可看到：
--   - 最近编辑（草稿试卷）
--   - 共有题库 / 私有题库 / 收藏夹

SET NAMES utf8mb4;
START TRANSACTION;

SET @demo_user_id = (
    SELECT id FROM sys_user WHERE username = 'zzs' ORDER BY id LIMIT 1
);
SET @demo_user_id = IFNULL(@demo_user_id, 4);

INSERT INTO question_bank
    (name, subject_id, visibility, owner_id, description, bank_type, create_time, update_time)
SELECT 'A3测试·Python基础公共题库', 1, 'public', NULL,
       '覆盖Python基础语法与常用数据结构，用于公共题库组卷测试。', 'final_review', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM question_bank WHERE name = 'A3测试·Python基础公共题库' AND visibility = 'public'
);

INSERT INTO question_bank
    (name, subject_id, visibility, owner_id, description, bank_type, create_time, update_time)
SELECT 'A3测试·数据结构公共题库', 5, 'public', NULL,
       '覆盖栈、队列、查找、树和链表，用于多题型试卷预览。', 'chapter_practice', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM question_bank WHERE name = 'A3测试·数据结构公共题库' AND visibility = 'public'
);

INSERT INTO question_bank
    (name, subject_id, visibility, owner_id, description, bank_type, create_time, update_time)
SELECT 'A3测试·我的Python错题集', 1, 'private', @demo_user_id,
       'zzs的私有Python练习题，用于验证私有题库权限与组卷。', 'wrong_questions', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM question_bank
    WHERE name = 'A3测试·我的Python错题集' AND visibility = 'private' AND owner_id = @demo_user_id
);

INSERT INTO question_bank
    (name, subject_id, visibility, owner_id, description, bank_type, create_time, update_time)
SELECT 'A3测试·我的数据库练习', 3, 'private', @demo_user_id,
       'zzs的私有数据库基础题，用于验证收藏和混合组卷。', 'custom', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM question_bank
    WHERE name = 'A3测试·我的数据库练习' AND visibility = 'private' AND owner_id = @demo_user_id
);

SET @public_python_bank = (
    SELECT id FROM question_bank
    WHERE name = 'A3测试·Python基础公共题库' AND visibility = 'public'
    ORDER BY id LIMIT 1
);
SET @public_ds_bank = (
    SELECT id FROM question_bank
    WHERE name = 'A3测试·数据结构公共题库' AND visibility = 'public'
    ORDER BY id LIMIT 1
);
SET @private_python_bank = (
    SELECT id FROM question_bank
    WHERE name = 'A3测试·我的Python错题集' AND visibility = 'private' AND owner_id = @demo_user_id
    ORDER BY id LIMIT 1
);
SET @private_db_bank = (
    SELECT id FROM question_bank
    WHERE name = 'A3测试·我的数据库练习' AND visibility = 'private' AND owner_id = @demo_user_id
    ORDER BY id LIMIT 1
);

-- 公共题库：Python基础（6题）
INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_python_bank, 1, 'Python程序设计', '基础语法', '函数定义', '单选题', '简单',
       'Python中用于定义普通函数的关键字是？', '["function","def","func","lambda"]', 'B',
       'def用于定义普通函数；lambda用于创建匿名函数。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_python_bank AND content = 'Python中用于定义普通函数的关键字是？');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_python_bank, 1, 'Python程序设计', '数据类型', '不可变对象', '多选题', '中等',
       '下列哪些属于Python不可变数据类型？', '["列表","元组","字符串","字典"]', 'B、C',
       '元组和字符串创建后不能原地修改；列表和字典属于可变对象。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_python_bank AND content = '下列哪些属于Python不可变数据类型？');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_python_bank, 1, 'Python程序设计', '容器类型', '列表', '判断题', '简单',
       'Python列表是可变对象，可以在原列表上追加或删除元素。', NULL, '正确',
       'append、extend、remove等操作都会修改原列表。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_python_bank AND content = 'Python列表是可变对象，可以在原列表上追加或删除元素。');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_python_bank, 1, 'Python程序设计', '内置函数', 'len函数', '填空题', '简单',
       '表达式len([10, 20, 30])的计算结果为____。', NULL, '3',
       '列表中包含三个元素，因此长度为3。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_python_bank AND content = '表达式len([10, 20, 30])的计算结果为____。');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_python_bank, 1, 'Python程序设计', '数据类型', '列表与元组', '简答题', '中等',
       '简述Python列表与元组的主要区别，并各举一个适用场景。', NULL,
       '列表可变，适合保存需要增删改的数据；元组不可变，适合表达固定记录或作为字典键。',
       '答案应说明可变性差异，并给出合理使用场景。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_python_bank AND content = '简述Python列表与元组的主要区别，并各举一个适用场景。');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_python_bank, 1, 'Python程序设计', '函数与字符串', '回文判断', '编程题', '困难',
       '编写函数is_palindrome(text)，忽略大小写判断字符串是否为回文，返回布尔值。', NULL,
       'def is_palindrome(text):\n    value = text.lower()\n    return value == value[::-1]',
       '将字符串统一为小写后与其反转结果比较。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_python_bank AND content = '编写函数is_palindrome(text)，忽略大小写判断字符串是否为回文，返回布尔值。');

-- 公共题库：数据结构（6题）
INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_ds_bank, 5, '数据结构', '线性结构', '队列', '单选题', '简单',
       '队列通常遵循哪一种元素访问原则？', '["先进先出","后进先出","随机访问","按关键字访问"]', 'A',
       '队列从队尾入队、队头出队，遵循FIFO原则。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_ds_bank AND content = '队列通常遵循哪一种元素访问原则？');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_ds_bank, 5, '数据结构', '树', '二叉搜索树', '多选题', '中等',
       '关于二叉搜索树，下列说法正确的有？', '["左子树键值通常小于根节点","右子树键值通常大于根节点","中序遍历结果有序","所有节点都必须有两个孩子"]', 'A、B、C',
       '二叉搜索树不要求每个节点都有两个孩子。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_ds_bank AND content = '关于二叉搜索树，下列说法正确的有？');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_ds_bank, 5, '数据结构', '递归', '运行时栈', '判断题', '中等',
       '递归调用通常会借助运行时栈保存每一层调用的局部状态。', NULL, '正确',
       '每次函数调用都会形成新的栈帧，返回时按相反顺序出栈。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_ds_bank AND content = '递归调用通常会借助运行时栈保存每一层调用的局部状态。');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_ds_bank, 5, '数据结构', '查找', '二分查找', '填空题', '中等',
       '在有序数组中，二分查找的平均时间复杂度为____。', NULL, 'O(log n)',
       '每次比较都将搜索区间缩小约一半。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_ds_bank AND content = '在有序数组中，二分查找的平均时间复杂度为____。');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_ds_bank, 5, '数据结构', '散列表', '哈希冲突', '简答题', '中等',
       '什么是哈希冲突？请写出两种常见的冲突解决方法。', NULL,
       '不同关键字映射到同一哈希地址称为哈希冲突；常见方法有开放定址法和链地址法。',
       '答案包含冲突定义以及任意两种合理解决方法即可。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_ds_bank AND content = '什么是哈希冲突？请写出两种常见的冲突解决方法。');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @public_ds_bank, 5, '数据结构', '链表', '单链表反转', '编程题', '困难',
       '编写伪代码或程序，将一个单链表原地反转并返回新的头节点。', NULL,
       'prev = null\ncur = head\nwhile cur != null:\n    next = cur.next\n    cur.next = prev\n    prev = cur\n    cur = next\nreturn prev',
       '遍历过程中保存后继节点，并逐个反转next指针。', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @public_ds_bank AND content = '编写伪代码或程序，将一个单链表原地反转并返回新的头节点。');

-- 私有题库：zzs 的 Python 错题集（4题）
INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @private_python_bank, 1, 'Python程序设计', '序列', 'range函数', '单选题', '简单',
       'list(range(1, 5))包含多少个整数？', '["3","4","5","6"]', 'B',
       'range左闭右开，结果为1、2、3、4，共4个整数。', @demo_user_id, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @private_python_bank AND content = 'list(range(1, 5))包含多少个整数？');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @private_python_bank, 1, 'Python程序设计', '字典', '字典方法', '多选题', '中等',
       '下列哪些是Python字典对象的常用方法？', '["keys","values","items","append"]', 'A、B、C',
       'append是列表方法，字典常用keys、values和items访问视图。', @demo_user_id, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @private_python_bank AND content = '下列哪些是Python字典对象的常用方法？');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @private_python_bank, 1, 'Python程序设计', '推导式', '列表推导式', '填空题', '中等',
       '表达式[x * x for x in range(3)]的结果为____。', NULL, '[0, 1, 4]',
       'range(3)产生0、1、2，分别平方得到0、1、4。', @demo_user_id, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @private_python_bank AND content = '表达式[x * x for x in range(3)]的结果为____。');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @private_python_bank, 1, 'Python程序设计', '综合应用', '词频统计', '编程题', '困难',
       '编写函数word_count(words)，返回每个字符串在列表中出现次数的字典。', NULL,
       'def word_count(words):\n    result = {}\n    for word in words:\n        result[word] = result.get(word, 0) + 1\n    return result',
       '使用字典get方法读取已有计数，不存在时从0开始。', @demo_user_id, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @private_python_bank AND content = '编写函数word_count(words)，返回每个字符串在列表中出现次数的字典。');

-- 私有题库：zzs 的数据库练习（4题）
INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @private_db_bank, 3, '数据库', 'SQL基础', '聚合查询', '单选题', '简单',
       'SQL中用于统计结果行数的聚合函数是？', '["SUM","COUNT","AVG","MAX"]', 'B',
       'COUNT用于统计行数或非NULL值数量。', @demo_user_id, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @private_db_bank AND content = 'SQL中用于统计结果行数的聚合函数是？');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @private_db_bank, 3, '数据库', '事务', 'ACID特性', '多选题', '中等',
       '关系数据库事务的ACID特性包括哪些？', '["原子性","一致性","隔离性","持久性"]', 'A、B、C、D',
       'ACID分别代表Atomicity、Consistency、Isolation、Durability。', @demo_user_id, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @private_db_bank AND content = '关系数据库事务的ACID特性包括哪些？');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @private_db_bank, 3, '数据库', '关系模型', '主键约束', '判断题', '简单',
       '关系表的主键列可以包含NULL值。', NULL, '错误',
       '主键必须唯一且非空。', @demo_user_id, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @private_db_bank AND content = '关系表的主键列可以包含NULL值。');

INSERT INTO question
    (bank_id, subject_id, subject, chapter, knowledge_point, question_type, difficulty,
     content, options, answer, analysis, creator_id, create_time, update_time)
SELECT @private_db_bank, 3, '数据库', '索引', '索引设计', '简答题', '困难',
       '简述数据库索引对查询和写入性能的主要影响。', NULL,
       '索引通常减少查询扫描量并加快检索，但会占用额外空间，且插入、更新、删除时需要维护索引，因此可能降低写入性能。',
       '需要同时说明查询收益以及空间和写入维护成本。', @demo_user_id, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question WHERE bank_id = @private_db_bank AND content = '简述数据库索引对查询和写入性能的主要影响。');

-- 私有题库关联
INSERT INTO question_bank_item (bank_id, question_id, added_by, create_time)
SELECT q.bank_id, q.id, @demo_user_id, NOW()
FROM question q
WHERE q.bank_id IN (@private_python_bank, @private_db_bank)
  AND NOT EXISTS (
      SELECT 1 FROM question_bank_item item
      WHERE item.bank_id = q.bank_id AND item.question_id = q.id
  );

-- 收藏夹
INSERT INTO question_favorite (user_id, question_id, create_time)
SELECT @demo_user_id, q.id, NOW()
FROM question q
WHERE q.content IN (
    'Python中用于定义普通函数的关键字是？',
    '简述Python列表与元组的主要区别，并各举一个适用场景。',
    '队列通常遵循哪一种元素访问原则？',
    '什么是哈希冲突？请写出两种常见的冲突解决方法。',
    '表达式[x * x for x in range(3)]的结果为____。',
    '关系数据库事务的ACID特性包括哪些？'
)
AND NOT EXISTS (
    SELECT 1 FROM question_favorite favorite
    WHERE favorite.user_id = @demo_user_id AND favorite.question_id = q.id
);

-- 最近编辑：草稿试卷（首页展示 status=draft）
INSERT INTO paper
    (name, subject_id, subject, category, remark, duration, total_score, status, creator_id, create_time, update_time)
SELECT 'A3测试·Python期中复习卷（草稿）', 1, 'Python程序设计', '期中考试',
       '自动种子数据：可继续选题或删除。', 90, 0, 'draft', @demo_user_id,
       DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM paper
    WHERE name = 'A3测试·Python期中复习卷（草稿）' AND creator_id = @demo_user_id
);

INSERT INTO paper
    (name, subject_id, subject, category, remark, duration, total_score, status, creator_id, create_time, update_time)
SELECT 'A3测试·数据结构随堂练习（草稿）', 5, '数据结构', '章节练习',
       '自动种子数据：混合公共题与私有题。', 45, 0, 'draft', @demo_user_id,
       DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 30 MINUTE)
WHERE NOT EXISTS (
    SELECT 1 FROM paper
    WHERE name = 'A3测试·数据结构随堂练习（草稿）' AND creator_id = @demo_user_id
);

-- 我的试卷页也能看到已完成卷
INSERT INTO paper
    (name, subject_id, subject, category, remark, duration, total_score, status, creator_id, create_time, update_time)
SELECT 'A3测试·数据库基础测验（已完成）', 3, '数据库', '单元测验',
       '自动种子数据：用于「我的试卷」已完成列表。', 60, 0, 'completed', @demo_user_id,
       DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM paper
    WHERE name = 'A3测试·数据库基础测验（已完成）' AND creator_id = @demo_user_id
);

SET @draft_python_paper = (
    SELECT id FROM paper
    WHERE name = 'A3测试·Python期中复习卷（草稿）' AND creator_id = @demo_user_id
    ORDER BY id LIMIT 1
);
SET @draft_ds_paper = (
    SELECT id FROM paper
    WHERE name = 'A3测试·数据结构随堂练习（草稿）' AND creator_id = @demo_user_id
    ORDER BY id LIMIT 1
);
SET @completed_db_paper = (
    SELECT id FROM paper
    WHERE name = 'A3测试·数据库基础测验（已完成）' AND creator_id = @demo_user_id
    ORDER BY id LIMIT 1
);

-- 草稿卷1：Python 公共题 4 道
INSERT INTO paper_question (paper_id, question_id, question_order, score, source_type, source_id, create_time)
SELECT @draft_python_paper, q.id, ranked.ord, ranked.score, 'public', q.bank_id, NOW()
FROM (
    SELECT content, ord, score FROM (
        SELECT 'Python中用于定义普通函数的关键字是？' AS content, 1 AS ord, 5 AS score
        UNION ALL SELECT '下列哪些属于Python不可变数据类型？', 2, 5
        UNION ALL SELECT 'Python列表是可变对象，可以在原列表上追加或删除元素。', 3, 5
        UNION ALL SELECT '表达式len([10, 20, 30])的计算结果为____。', 4, 5
    ) t
) ranked
JOIN question q ON q.content = ranked.content AND q.bank_id = @public_python_bank
WHERE @draft_python_paper IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM paper_question pq
      WHERE pq.paper_id = @draft_python_paper AND pq.question_id = q.id
  );

-- 草稿卷2：数据结构公共题 + 私有 Python 题
INSERT INTO paper_question (paper_id, question_id, question_order, score, source_type, source_id, create_time)
SELECT @draft_ds_paper, q.id, ranked.ord, ranked.score, ranked.source_type, q.bank_id, NOW()
FROM (
    SELECT content, bank_id, ord, score, source_type FROM (
        SELECT '队列通常遵循哪一种元素访问原则？' AS content, @public_ds_bank AS bank_id, 1 AS ord, 5 AS score, 'public' AS source_type
        UNION ALL SELECT '关于二叉搜索树，下列说法正确的有？', @public_ds_bank, 2, 5, 'public'
        UNION ALL SELECT '在有序数组中，二分查找的平均时间复杂度为____。', @public_ds_bank, 3, 5, 'public'
        UNION ALL SELECT 'list(range(1, 5))包含多少个整数？', @private_python_bank, 4, 5, 'private'
    ) t
) ranked
JOIN question q ON q.content = ranked.content AND q.bank_id = ranked.bank_id
WHERE @draft_ds_paper IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM paper_question pq
      WHERE pq.paper_id = @draft_ds_paper AND pq.question_id = q.id
  );

-- 已完成卷：数据库私有题 3 道
INSERT INTO paper_question (paper_id, question_id, question_order, score, source_type, source_id, create_time)
SELECT @completed_db_paper, q.id, ranked.ord, ranked.score, 'private', q.bank_id, NOW()
FROM (
    SELECT content, ord, score FROM (
        SELECT 'SQL中用于统计结果行数的聚合函数是？' AS content, 1 AS ord, 10 AS score
        UNION ALL SELECT '关系数据库事务的ACID特性包括哪些？', 2, 10
        UNION ALL SELECT '关系表的主键列可以包含NULL值。', 3, 10
    ) t
) ranked
JOIN question q ON q.content = ranked.content AND q.bank_id = @private_db_bank
WHERE @completed_db_paper IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM paper_question pq
      WHERE pq.paper_id = @completed_db_paper AND pq.question_id = q.id
  );

-- 回填试卷总分（首页展示用）
UPDATE paper p
JOIN (
    SELECT paper_id, COALESCE(SUM(score), 0) AS total_score
    FROM paper_question
    WHERE paper_id IN (@draft_python_paper, @draft_ds_paper, @completed_db_paper)
    GROUP BY paper_id
) s ON s.paper_id = p.id
SET p.total_score = s.total_score,
    p.update_time = GREATEST(p.update_time, NOW());

COMMIT;

SELECT
    @demo_user_id AS demo_user_id,
    (SELECT COUNT(*) FROM question_bank WHERE name LIKE 'A3测试·%') AS banks,
    (SELECT COUNT(*) FROM question q JOIN question_bank b ON b.id = q.bank_id WHERE b.name LIKE 'A3测试·%') AS questions,
    (SELECT COUNT(*) FROM question_favorite WHERE user_id = @demo_user_id) AS favorites,
    (SELECT COUNT(*) FROM paper WHERE creator_id = @demo_user_id AND name LIKE 'A3测试·%') AS papers;
