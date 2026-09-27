SELECT COUNT(*) AS python_count FROM knowledge_base WHERE job_position='Python 开发';
SELECT question_type, COUNT(*) AS c FROM knowledge_base WHERE job_position='Python 开发' GROUP BY question_type;
SELECT LEFT(question, 30) AS q, LEFT(remark, 50) AS src FROM knowledge_base WHERE job_position='Python 开发' LIMIT 3;
SELECT COUNT(*) AS job_ok FROM interview_job_tech_stack WHERE job_position='Python 开发';
