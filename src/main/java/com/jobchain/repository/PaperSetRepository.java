package com.jobchain.repository;

import com.jobchain.entity.PaperSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaperSetRepository extends JpaRepository<PaperSetEntity, UUID> {

    /**
     * Find all paper sets for a specific vacancy.
     * @param vacancyId UUID of the vacancy
     * @return List of paper sets for the vacancy
     */
    List<PaperSetEntity> findByVacancyId(UUID vacancyId);
}