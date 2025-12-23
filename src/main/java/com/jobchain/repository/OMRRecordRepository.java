package com.jobchain.repository;

import com.jobchain.entity.OMRRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OMRRecordRepository extends JpaRepository<OMRRecordEntity, UUID> {

    /**
     * Find OMR record by candidate ID.
     * @param candidateId String identifier of the candidate
     * @return Optional containing OMRRecordEntity if found
     */
    Optional<OMRRecordEntity> findByCandidateId(String candidateId);

    /**
     * Find OMR record by vacancy ID.
     * @param vacancyId UUID of the vacancy
     * @return Optional containing OMRRecordEntity if found
     */
    Optional<OMRRecordEntity> findByVacancyId(UUID vacancyId);
}