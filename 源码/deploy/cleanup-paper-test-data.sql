-- Cleanup corrupted A3 paper seed rows (idempotent for re-seed).
SET NAMES utf8mb4;
START TRANSACTION;

SET @demo_user_id = (
    SELECT id FROM sys_user WHERE username = 'zzs' ORDER BY id LIMIT 1
);
SET @demo_user_id = IFNULL(@demo_user_id, 4);

DELETE pq FROM paper_question pq
JOIN paper p ON p.id = pq.paper_id
WHERE p.name LIKE 'A3测试·%'
   OR p.name LIKE 'A3???%'
   OR (p.creator_id = @demo_user_id AND p.name LIKE 'A3%');

DELETE FROM paper
WHERE name LIKE 'A3测试·%'
   OR name LIKE 'A3???%'
   OR (creator_id = @demo_user_id AND name LIKE 'A3%');

DELETE f FROM question_favorite f
JOIN question q ON q.id = f.question_id
JOIN question_bank b ON b.id = q.bank_id
WHERE b.name LIKE 'A3测试·%'
   OR b.name LIKE 'A3???%'
   OR b.name LIKE 'A3%';

DELETE i FROM question_bank_item i
JOIN question_bank b ON b.id = i.bank_id
WHERE b.name LIKE 'A3测试·%'
   OR b.name LIKE 'A3???%'
   OR b.name LIKE 'A3%';

DELETE q FROM question q
JOIN question_bank b ON b.id = q.bank_id
WHERE b.name LIKE 'A3测试·%'
   OR b.name LIKE 'A3???%'
   OR b.name LIKE 'A3%';

DELETE FROM question_bank
WHERE name LIKE 'A3测试·%'
   OR name LIKE 'A3???%'
   OR name LIKE 'A3%';

COMMIT;
