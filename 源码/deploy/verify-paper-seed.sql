SET NAMES utf8mb4;
SELECT
  (SELECT COUNT(*) FROM question_bank WHERE name LIKE 'A3测试·%') AS ok_banks,
  (SELECT COUNT(*) FROM question_bank WHERE name LIKE 'A3???%') AS bad_banks,
  (SELECT COUNT(*) FROM paper WHERE name LIKE 'A3测试·%') AS ok_papers,
  (SELECT COUNT(*) FROM paper WHERE name LIKE 'A3???%') AS bad_papers,
  (SELECT COUNT(*) FROM question q JOIN question_bank b ON b.id = q.bank_id
     WHERE b.name LIKE 'A3测试·%' AND q.content LIKE '%关键字%') AS ok_questions,
  (SELECT COUNT(*) FROM question q JOIN question_bank b ON b.id = q.bank_id
     WHERE b.name LIKE 'A3%' AND q.content LIKE '%???%') AS bad_questions;
