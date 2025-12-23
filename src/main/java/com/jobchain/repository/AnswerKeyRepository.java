package com.jobchain.repository;

import com.jobchain.entity.AnswerKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerKeyRepository extends JpaRepository<AnswerKeyEntity, UUID> {

    /**
     * Find answer key by vacancy ID.
     * @param vacancyId UUID of the vacancy
     * @return Optional containing AnswerKeyEntity if found
     */
    Optional<AnswerKeyEntity> findByVacancyId(UUID vacancyId);
}