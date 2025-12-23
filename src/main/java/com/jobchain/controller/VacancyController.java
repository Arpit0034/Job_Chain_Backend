package com.jobchain.controller;

import com.jobchain.dto.CreateVacancyRequest;
import com.jobchain.dto.VacancyResponse;
import com.jobchain.entity.VacancyEntity;
import com.jobchain.service.VacancyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/vacancies")
@Slf4j
public class VacancyController {

    @Autowired
    private VacancyService vacancyService;

    /**
     * Creates a new job vacancy and records on blockchain.
     *
     * Endpoint: POST /api/vacancies
     *
     * @param request CreateVacancyRequest containing title and totalPosts
     * @return ResponseEntity with VacancyResponse and 201 CREATED status
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<VacancyResponse> createVacancy(@Valid @RequestBody CreateVacancyRequest request) {
        try {
            log.info("POST /api/vacancies - Creating vacancy: {}", request.getTitle());

            // Generate paper hash (in production, this would be hash of actual notification document)
            String paperHash = "0".repeat(64); // Placeholder - replace with actual document hash

            // Create vacancy via service
            VacancyEntity vacancy = vacancyService.createVacancy(
                    request.getTitle(),
                    request.getTotalPosts(),
                    paperHash
            );

            // Map entity to response DTO
            VacancyResponse response = mapToResponse(vacancy);

            log.info("Vacancy created successfully: id={}", vacancy.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw new RuntimeException("Validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create vacancy: {}", e.getMessage());
            throw new RuntimeException("Failed to create vacancy: " + e.getMessage());
        }
    }

    /**
     * Retrieves all vacancies from database.
     *
     * Endpoint: GET /api/vacancies
     *
     * @return ResponseEntity with list of VacancyResponse
     */
    @GetMapping
    public ResponseEntity<List<VacancyResponse>> getAllVacancies() {
        try {
            log.info("GET /api/vacancies - Fetching all vacancies");

            List<VacancyEntity> vacancies = vacancyService.getAllVacancies();
            List<VacancyResponse> responses = vacancies.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            log.info("Retrieved {} vacancies", responses.size());
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("Failed to fetch vacancies: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch vacancies: " + e.getMessage());
        }
    }

    /**
     * Retrieves a specific vacancy by ID.
     *
     * Endpoint: GET /api/vacancies/{id}
     *
     * @param id UUID of the vacancy
     * @return ResponseEntity with VacancyResponse or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<VacancyResponse> getVacancyById(@PathVariable UUID id) {
        try {
            log.info("GET /api/vacancies/{} - Fetching vacancy", id);

            Optional<VacancyEntity> vacancyOpt = vacancyService.getVacancyById(id);

            if (!vacancyOpt.isPresent()) {
                log.warn("Vacancy not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            VacancyResponse response = mapToResponse(vacancyOpt.get());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch vacancy: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch vacancy: " + e.getMessage());
        }
    }

    /**
     * Maps VacancyEntity to VacancyResponse DTO.
     */
    private VacancyResponse mapToResponse(VacancyEntity entity) {
        VacancyResponse response = new VacancyResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setTotalPosts(entity.getTotalPosts());
        response.setCreatedAt(entity.getCreatedAt());
        response.setBlockchainTxHash(entity.getBlockchainTxHash());
        return response;
    }
}