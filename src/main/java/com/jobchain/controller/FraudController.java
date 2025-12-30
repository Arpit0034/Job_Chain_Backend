// =====================================================================
// FILE: FraudController.java - COMPLETE VERSION
// =====================================================================
package com.jobchain.controller;

import com.jobchain.dto.FraudAlertResponse;
import com.jobchain.entity.FraudAlertEntity;
import com.jobchain.service.FraudDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fraud")
@Slf4j
public class FraudController {

    @Autowired
    private FraudDetectionService fraudDetectionService;


    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{vacancyId}")
    public ResponseEntity<List<FraudAlertResponse>> getFraudAlerts(@PathVariable UUID vacancyId) {
        try {

            log.info("GET /api/fraud/{} - Fetching fraud alerts", vacancyId);


            List<FraudAlertEntity> alerts = fraudDetectionService.getFraudAlerts(vacancyId);


            List<FraudAlertResponse> responses = alerts.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());


            if (responses.isEmpty()) {
                log.info("✅ No fraud alerts found for vacancy");
            } else {
                log.warn("🚨 Retrieved {} fraud alerts", responses.size());
            }

            // Step 4: Return HTTP 200 OK with the list of fraud alerts
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            // If anything goes wrong, log the error and throw runtime exception
            log.error("Failed to fetch fraud alerts: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch fraud alerts: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/analyze")
    public ResponseEntity<List<FraudAlertEntity>> analyzeFraud(@RequestParam UUID vacancyId) {
        try {
            log.info("POST /api/fraud/analyze - Analyzing fraud for vacancy: {}", vacancyId);

            List<FraudAlertEntity> alerts = new ArrayList<>();

            log.info("Running paper leak detection...");
            List<FraudAlertEntity> paperLeakAlerts = fraudDetectionService.detectPaperLeak(vacancyId);
            alerts.addAll(paperLeakAlerts);

            if (!paperLeakAlerts.isEmpty()) {
                log.error("🚨 PAPER LEAK DETECTED: {} alerts", paperLeakAlerts.size());
            }

            log.info("Running marks anomaly detection...");
            List<FraudAlertEntity> marksAlerts = fraudDetectionService.detectMarksAnomaly(vacancyId);
            alerts.addAll(marksAlerts);

            if (!marksAlerts.isEmpty()) {
                log.error("🚨 MARKS ANOMALY DETECTED: {} alerts", marksAlerts.size());
            }

            if (alerts.isEmpty()) {
                log.info("✅ Fraud analysis complete: No fraud detected");
            } else {
                log.error("🚨 Fraud analysis complete: {} total alerts generated", alerts.size());
            }

            return ResponseEntity.ok(alerts);

        } catch (Exception e) {
            log.error("Failed to analyze fraud: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze fraud: " + e.getMessage());
        }
    }


    private FraudAlertResponse mapToResponse(FraudAlertEntity entity) {
        FraudAlertResponse response = new FraudAlertResponse();
        response.setVacancyId(entity.getVacancyId());
        response.setAlertType(entity.getAlertType());
        response.setSuspectCount(entity.getSuspectCount());
        response.setPatternHash(entity.getPatternHash());
        response.setTimestamp(entity.getTimestamp());
        return response;
    }
}
