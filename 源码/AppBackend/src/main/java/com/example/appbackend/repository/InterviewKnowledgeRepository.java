package com.example.appbackend.repository;

import com.example.appbackend.entity.InterviewKnowledge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewKnowledgeRepository extends JpaRepository<InterviewKnowledge, Long> {

    @Query("SELECT k FROM InterviewKnowledge k WHERE "
            + "(:jobPosition IS NULL OR :jobPosition = '' OR k.jobPosition = :jobPosition) AND "
            + "(:questionType IS NULL OR :questionType = '' OR k.questionType = :questionType) AND "
            + "(:status IS NULL OR k.status = :status) AND "
            + "(:q IS NULL OR :q = '' OR k.question LIKE CONCAT('%', :q, '%') "
            + "OR COALESCE(k.keywords, '') LIKE CONCAT('%', :q, '%') "
            + "OR k.jobPosition LIKE CONCAT('%', :q, '%')) "
            + "ORDER BY k.id DESC")
    Page<InterviewKnowledge> search(@Param("jobPosition") String jobPosition,
                                    @Param("questionType") String questionType,
                                    @Param("status") Integer status,
                                    @Param("q") String q,
                                    Pageable pageable);

    @Query("SELECT DISTINCT k.questionType FROM InterviewKnowledge k WHERE "
            + "(:jobPosition IS NULL OR :jobPosition = '' OR k.jobPosition = :jobPosition) AND "
            + "(:status IS NULL OR k.status = :status) AND "
            + "k.questionType IS NOT NULL AND k.questionType <> '' ORDER BY k.questionType")
    List<String> findDistinctQuestionTypes(@Param("jobPosition") String jobPosition,
                                           @Param("status") Integer status);

    @Query("SELECT DISTINCT k.jobPosition FROM InterviewKnowledge k WHERE "
            + "(:questionType IS NULL OR :questionType = '' OR k.questionType = :questionType) AND "
            + "(:status IS NULL OR k.status = :status) AND "
            + "k.jobPosition IS NOT NULL AND k.jobPosition <> '' ORDER BY k.jobPosition")
    List<String> findDistinctJobPositions(@Param("questionType") String questionType,
                                          @Param("status") Integer status);

    @Query("SELECT k FROM InterviewKnowledge k WHERE "
            + "(:jobPosition IS NULL OR :jobPosition = '' OR k.jobPosition = :jobPosition) AND "
            + "(:questionType IS NULL OR :questionType = '' OR k.questionType = :questionType) AND "
            + "(:status IS NULL OR k.status = :status) AND "
            + "(k.question LIKE CONCAT('%', :q, '%') "
            + "OR COALESCE(k.keywords, '') LIKE CONCAT('%', :q, '%') "
            + "OR k.jobPosition LIKE CONCAT('%', :q, '%') "
            + "OR COALESCE(k.answerPoints, '') LIKE CONCAT('%', :q, '%')) "
            + "ORDER BY k.id DESC")
    List<InterviewKnowledge> keywordMatch(@Param("q") String q,
                                          @Param("jobPosition") String jobPosition,
                                          @Param("questionType") String questionType,
                                          @Param("status") Integer status,
                                          Pageable pageable);
}
