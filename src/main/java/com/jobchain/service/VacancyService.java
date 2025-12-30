package com.jobchain.service;

import com.jobchain.entity.VacancyEntity;
import com.jobchain.repository.VacancyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class VacancyService {

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private BlockchainService blockchainService;

    public VacancyEntity createVacancy(String title, int totalPosts, String paperHash) {
        try {
            log.info("Creating vacancy: title={}, totalPosts={}", title, totalPosts);

            // Validation
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Vacancy title cannot be empty");
            }
            if (totalPosts <= 0) {
                throw new IllegalArgumentException("Total posts must be greater than 0");
            }
            if (paperHash == null || paperHash.length() != 64) {
                throw new IllegalArgumentException("Invalid paper hash format");
            }

            // Record vacancy on blockchain first
            String txHash = blockchainService.createVacancyOnChain(title, totalPosts, paperHash);

            // Create entity and save to database
            VacancyEntity vacancy = VacancyEntity.builder()
                    .title(title)
                    .totalPosts(totalPosts)
                    .paperHash(paperHash)
                    .blockchainTxHash(txHash)
                    .build();

            VacancyEntity savedVacancy = vacancyRepository.save(vacancy);
            log.info("Vacancy created successfully: id={}, txHash={}", savedVacancy.getId(), txHash);

            return savedVacancy;

        } catch (Exception e) {
            log.error("Failed to create vacancy: {}", e.getMessage());
            throw new RuntimeException("Vacancy creation failed", e);
        }
    }

    public List<VacancyEntity> getAllVacancies() {
        try {
            log.info("Fetching all vacancies");
            List<VacancyEntity> vacancies = vacancyRepository.findAll();
            log.info("Retrieved {} vacancies", vacancies.size());
            return vacancies;
        } catch (Exception e) {
            log.error("Failed to fetch vacancies: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve vacancies", e);
        }
    }

    public Optional<VacancyEntity> getVacancyById(UUID id) {
        try {
            log.info("Fetching vacancy by id: {}", id);
            Optional<VacancyEntity> vacancy = vacancyRepository.findById(id);
            if (vacancy.isPresent()) {
                log.info("Vacancy found: {}", id);
            } else {
                log.warn("Vacancy not found: {}", id);
            }
            return vacancy;
        } catch (Exception e) {
            log.error("Failed to fetch vacancy: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve vacancy", e);
        }
    }

    public void deleteVacancy(UUID id) {
        try {
            log.info("Deleting vacancy: {}", id);

            if (!vacancyRepository.existsById(id)) {
                throw new IllegalArgumentException("Vacancy not found: " + id);
            }

            vacancyRepository.deleteById(id);
            log.info("Vacancy deleted successfully: {}", id);

        } catch (Exception e) {
            log.error("Failed to delete vacancy: {}", e.getMessage());
            throw new RuntimeException("Vacancy deletion failed", e);
        }
    }
}