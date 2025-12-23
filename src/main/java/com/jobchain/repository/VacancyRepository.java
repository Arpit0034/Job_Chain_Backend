package com.jobchain.repository;

import com.jobchain.entity.VacancyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VacancyRepository extends JpaRepository<VacancyEntity, UUID> {

}