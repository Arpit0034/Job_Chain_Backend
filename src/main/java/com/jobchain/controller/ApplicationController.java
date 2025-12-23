package com.jobchain.controller;

import com.jobchain.dto.ApplicationResponse;
import com.jobchain.dto.CreateApplicationRequest;
import com.jobchain.entity.ApplicationEntity;
import com.jobchain.service.ApplicationService;
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
@RequestMapping("/api/applications")
@Slf4j
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    /**
     * Submits a new job application and records on blockchain.
     *
     * Endpoint: POST /api/applications
     *
     * @param request CreateApplicationRequest with candidate details
     * @return ResponseEntity with ApplicationResponse and 201 CREATED status
     */
    @PreAuthorize("hasAuthority('STUDENT')")
    @PostMapping
    public ResponseEntity<ApplicationResponse> submitApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        try {
            log.info("POST /api/applications - Submitting application for vacancy: {}",
                    request.getVacancyId());

            ApplicationEntity application = applicationService.submitApplication(request);
            ApplicationResponse response = mapToResponse(application);

            log.info("Application submitted successfully: id={}", application.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw new RuntimeException("Validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to submit application: {}", e.getMessage());
            throw new RuntimeException("Failed to submit application: " + e.getMessage());
        }
    }

    /**
     * Retrieves all applications for a specific vacancy.
     *
     * Endpoint: GET /api/applications/vacancy/{vacancyId}
     *
     * @param vacancyId UUID of the vacancy
     * @return ResponseEntity with list of ApplicationResponse
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/vacancy/{vacancyId}")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByVacancy(
            @PathVariable UUID vacancyId) {
        try {
            log.info("GET /api/applications/vacancy/{} - Fetching applications", vacancyId);

            List<ApplicationEntity> applications = applicationService.getApplicationsByVacancy(vacancyId);
            List<ApplicationResponse> responses = applications.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            log.info("Retrieved {} applications for vacancy", responses.size());
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("Failed to fetch applications: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch applications: " + e.getMessage());
        }
    }

    /**
     * Retrieves a specific application by ID.
     *
     * Endpoint: GET /api/applications/{id}
     *
     * @param id UUID of the application
     * @return ResponseEntity with ApplicationResponse or 404 if not found
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable UUID id) {
        try {
            log.info("GET /api/applications/{} - Fetching application", id);

            Optional<ApplicationEntity> applicationOpt = applicationService.getApplicationById(id);

            if (!applicationOpt.isPresent()) {
                log.warn("Application not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            ApplicationResponse response = mapToResponse(applicationOpt.get());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch application: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch application: " + e.getMessage());
        }
    }

    /**
     * Maps ApplicationEntity to ApplicationResponse DTO.
     */
    private ApplicationResponse mapToResponse(ApplicationEntity entity) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(entity.getId());
        response.setVacancyId(entity.getVacancyId());
        response.setCandidateName(entity.getCandidateName());
        response.setEmail(entity.getEmail());
        response.setCategory(entity.getCategory());
        response.setAppHash(entity.getAppHash());
        response.setStatus(entity.getStatus());
        response.setBlockchainTxHash(entity.getBlockchainTxHash());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}