package com.jobchain.controller;

import com.jobchain.dto.*;
import com.jobchain.entity.ApplicationEntity;
import com.jobchain.entity.ExamScoreEntity;
import com.jobchain.entity.MeritListEntity;
import com.jobchain.service.ApplicationService;
import com.jobchain.service.ExamService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exam")
@Slf4j
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private ApplicationService applicationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/record-score")
    public ResponseEntity<ExamScoreResponse> recordExamScore(
            @Valid @RequestBody RecordExamScoreRequest request) {

        try {
            log.info("POST /api/exam/record-score - Recording score for application: {}",
                    request.getApplicationId());

            Optional<ApplicationEntity> applicationOpt =
                    applicationService.getApplicationById(request.getApplicationId());

            if (applicationOpt.isEmpty()) {
                throw new IllegalArgumentException(
                        "Application not found: " + request.getApplicationId());
            }

            ApplicationEntity application = applicationOpt.get();

            if (!application.getVacancyId().equals(request.getVacancyId())) {
                throw new IllegalArgumentException(
                        "Application does not belong to given vacancy");
            }

            ExamScoreEntity examScore = examService.recordExamScore(
                    request.getVacancyId(),
                    request.getApplicationId(),
                    request.getMarks(),
                    request.getMarkingJson()
            );

            ExamScoreResponse response = new ExamScoreResponse();
            response.setId(examScore.getId());
            response.setApplicationId(examScore.getApplicationId());
            response.setMarks(examScore.getMarks());
            response.setMarkingHash(examScore.getMarkingHash());
            response.setBlockchainTxHash(examScore.getBlockchainTxHash());

            log.info("Exam score recorded successfully: id={}", examScore.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to record exam score", e);
            throw new RuntimeException("Failed to record exam score");
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/publish-merit")
    public ResponseEntity<PublishMeritResponse> publishMerit(@RequestParam UUID vacancyId) {
        try {
            log.info("POST /api/exam/publish-merit - Publishing merit for vacancy: {}", vacancyId);

            // Publish merit list via service
            MeritListEntity meritList = examService.publishMerit(vacancyId);

            // Parse merit JSON to get ranked list
            List<Map<String, Object>> meritData = objectMapper.readValue(
                    meritList.getMeritJson(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            // Fetch applications to get candidate details
            List<ApplicationEntity> applications = applicationService.getApplicationsByVacancy(vacancyId);
            Map<String, ApplicationEntity> appMap = applications.stream()
                    .collect(Collectors.toMap(
                            app -> app.getId().toString(),
                            app -> app
                    ));

            // Map to response DTOs
            List<MeritItemResponse> meritItems = meritData.stream()
                    .map(item -> {
                        MeritItemResponse merit = new MeritItemResponse();
                        merit.setRank((Integer) item.get("rank"));

                        String appId = (String) item.get("applicationId");
                        ApplicationEntity app = appMap.get(appId);

                        if (app != null) {
                            merit.setCandidateName(app.getCandidateName());
                            merit.setCategory(app.getCategory());
                        } else {
                            merit.setCandidateName("Unknown");
                            merit.setCategory("N/A");
                        }

                        merit.setMarks((Double) item.get("marks"));
                        return merit;
                    })
                    .collect(Collectors.toList());

            PublishMeritResponse response = new PublishMeritResponse();
            response.setVacancyId(vacancyId);
            response.setMeritList(meritItems);
            response.setMeritHash(meritList.getMeritHash());
            response.setBlockchainTxHash(meritList.getBlockchainTxHash());

            log.info("Merit list published successfully with {} candidates", meritItems.size());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw new RuntimeException("Validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish merit list: {}", e.getMessage());
            throw new RuntimeException("Failed to publish merit list: " + e.getMessage());
        }
    }

    @GetMapping("/merit")
    public ResponseEntity<GetMeritResponse> getMeritList(@RequestParam UUID vacancyId) {
        try {
            log.info("GET /api/exam/merit - Fetching merit list for vacancy: {}", vacancyId);

            MeritListEntity meritList = examService.getMeritList(vacancyId);

            // Parse merit JSON
            List<Map<String, Object>> meritData = objectMapper.readValue(
                    meritList.getMeritJson(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            // Fetch applications to get candidate details
            List<ApplicationEntity> applications = applicationService.getApplicationsByVacancy(vacancyId);
            Map<String, ApplicationEntity> appMap = applications.stream()
                    .collect(Collectors.toMap(
                            app -> app.getId().toString(),
                            app -> app
                    ));

            // Map to response DTOs
            List<MeritItemResponse> meritItems = meritData.stream()
                    .map(item -> {
                        MeritItemResponse merit = new MeritItemResponse();
                        merit.setRank((Integer) item.get("rank"));

                        String appId = (String) item.get("applicationId");
                        ApplicationEntity app = appMap.get(appId);

                        if (app != null) {
                            merit.setCandidateName(app.getCandidateName());
                            merit.setCategory(app.getCategory());
                        } else {
                            merit.setCandidateName("Unknown");
                            merit.setCategory("N/A");
                        }

                        merit.setMarks((Double) item.get("marks"));
                        return merit;
                    })
                    .collect(Collectors.toList());

            GetMeritResponse response = new GetMeritResponse();
            response.setMeritList(meritItems);
            response.setMeritHash(meritList.getMeritHash());
            response.setBlockchainTxHash(meritList.getBlockchainTxHash());
            response.setVerified(meritList.isVerified());

            log.info("Merit list retrieved successfully with {} candidates", meritItems.size());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Merit list not found: {}", e.getMessage());
            throw new RuntimeException("Merit list not found: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch merit list: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch merit list: " + e.getMessage());
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verifyMeritIntegrity(@RequestParam UUID vacancyId) {
        try {
            log.info("GET /api/exam/verify - Verifying merit integrity for vacancy: {}", vacancyId);

            boolean verified = examService.verifyMeritIntegrity(vacancyId);

            Map<String, Boolean> result = new HashMap<>();
            result.put("verified", verified);

            if (verified) {
                log.info("✅ Merit verification result: VALID");
            } else {
                log.error("🚨 Merit verification result: TAMPERED");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to verify merit integrity: {}", e.getMessage());
            throw new RuntimeException("Failed to verify merit integrity: " + e.getMessage());
        }
    }
}