package com.jobchain.repository;

import com.jobchain.entity.ExamScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamScoreRepository extends JpaRepository<ExamScoreEntity, UUID> {

    /**
     * Find exam score by application ID.
     * @param applicationId UUID of the application
     * @return Optional containing ExamScoreEntity if found
     */
    Optional<ExamScoreEntity> findByApplicationId(UUID applicationId);

    @Query("""
    SELECT e.markingHash
    FROM ExamScoreEntity e
    WHERE e.vacancyId = :vacancyId
    """)
    List<String> findMarkingHashesByVacancyId(UUID vacancyId);

    @Query("""
    SELECT e.marks
    FROM ExamScoreEntity e
    WHERE e.vacancyId = :vacancyId
    """)
    List<Double> findMarksByVacancyId(UUID vacancyId);

    @Query("""
        SELECT e.applicationId, e.marks
        FROM ExamScoreEntity e
        WHERE e.vacancyId = :vacancyId
    """)
    List<Object[]> findApplicationIdAndMarksByVacancyId(
            @Param("vacancyId") UUID vacancyId
    );


}