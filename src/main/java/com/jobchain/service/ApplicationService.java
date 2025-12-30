package com.jobchain.service;

import com.jobchain.dto.CreateApplicationRequest;
import com.jobchain.entity.ApplicationEntity;
import com.jobchain.repository.ApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
@Slf4j
@Transactional
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private BlockchainService blockchainService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApplicationEntity submitApplication(CreateApplicationRequest request) {
        try {
            log.info("Submitting application for vacancy: {}, candidate: {}",
                    request.getVacancyId(), request.getCandidateName());

            // Validation
            if (request.getVacancyId() == null) {
                throw new IllegalArgumentException("Vacancy ID cannot be null");
            }
            if (request.getCandidateName() == null || request.getCandidateName().trim().isEmpty()) {
                throw new IllegalArgumentException("Candidate name cannot be empty");
            }
            if (request.getMarks10() <= 0 || request.getMarks12() <= 0) {
                throw new IllegalArgumentException("Marks must be positive");
            }

            // Create application JSON
            Map<String, Object> appData = new HashMap<>();
            appData.put("candidateName", request.getCandidateName());
            appData.put("email", request.getEmail());
            appData.put("category", request.getCategory());
            appData.put("marks10", request.getMarks10());
            appData.put("marks12", request.getMarks12());
            appData.put("timestamp", new Date().toString());

            String appJson = objectMapper.writeValueAsString(appData);

            // Calculate application hash
            String appHash = sha256(appJson);

            // Record on blockchain
            String txHash = blockchainService.logApplicationOnChain(request.getVacancyId(), appHash);

            // Save to database
            ApplicationEntity application = ApplicationEntity.builder()
                    .vacancyId(request.getVacancyId())
                    .candidateName(request.getCandidateName())
                    .email(request.getEmail())
                    .category(request.getCategory())
                    .marks10(request.getMarks10())
                    .marks12(request.getMarks12())
                    .appJson(appJson)
                    .appHash(appHash)
                    .status("SUBMITTED")
                    .blockchainTxHash(txHash)
                    .build();

            ApplicationEntity savedApplication = applicationRepository.save(application);
            log.info("Application submitted successfully: id={}, txHash={}",
                    savedApplication.getId(), txHash);

            return savedApplication;

        } catch (Exception e) {
            log.error("Failed to submit application: {}", e.getMessage());
            throw new RuntimeException("Application submission failed", e);
        }
    }

    public List<ApplicationEntity> getApplicationsByVacancy(UUID vacancyId) {
        try {
            log.info("Fetching applications for vacancy: {}", vacancyId);
            List<ApplicationEntity> applications = applicationRepository.findByVacancyId(vacancyId);
            log.info("Retrieved {} applications", applications.size());
            return applications;
        } catch (Exception e) {
            log.error("Failed to fetch applications: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve applications", e);
        }
    }

    public Optional<ApplicationEntity> getApplicationById(UUID id) {
        try {
            log.info("Fetching application by id: {}", id);
            Optional<ApplicationEntity> application = applicationRepository.findById(id);
            if (application.isPresent()) {
                log.info("Application found: {}", id);
            } else {
                log.warn("Application not found: {}", id);
            }
            return application;
        } catch (Exception e) {
            log.error("Failed to fetch application: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve application", e);
        }
    }

    public void updateApplicationStatus(UUID applicationId, String status) {
        try {
            log.info("Updating application status: id={}, status={}", applicationId, status);

            Optional<ApplicationEntity> applicationOpt = applicationRepository.findById(applicationId);
            if (!applicationOpt.isPresent()) {
                throw new IllegalArgumentException("Application not found: " + applicationId);
            }

            ApplicationEntity application = applicationOpt.get();
            application.setStatus(status);
            applicationRepository.save(application);

            log.info("Application status updated successfully");

        } catch (Exception e) {
            log.error("Failed to update application status: {}", e.getMessage());
            throw new RuntimeException("Status update failed", e);
        }
    }

    public boolean verifyApplicationIntegrity(UUID applicationId) {
        try {
            log.info("Verifying application integrity: {}", applicationId);

            Optional<ApplicationEntity> applicationOpt = applicationRepository.findById(applicationId);
            if (!applicationOpt.isPresent()) {
                log.warn("Application not found for verification");
                return false;
            }

            ApplicationEntity application = applicationOpt.get();

            // Recalculate hash from current application JSON
            String recalculatedHash = sha256(application.getAppJson());

            // Compare with stored hash
            boolean isValid = recalculatedHash.equals(application.getAppHash());

            if (isValid) {
                log.info("Application integrity verified: VALID");
            } else {
                log.error("Application integrity verification FAILED: TAMPERING DETECTED!");
            }

            return isValid;

        } catch (Exception e) {
            log.error("Failed to verify application integrity: {}", e.getMessage());
            return false;
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash calculation failed", e);
        }
    }
}