package com.jobchain.repository;

import com.jobchain.entity.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ApplicationEntity.
 * Provides CRUD operations and custom queries for job applications.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {

    /**
     * Find all applications for a specific vacancy.
     * @param vacancyId UUID of the vacancy
     * @return List of applications for the vacancy
     */
    List<ApplicationEntity> findByVacancyId(UUID vacancyId);
}