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

/**
 * REST Controller for fraud detection and security monitoring.
 * Provides endpoints for analyzing fraud patterns and retrieving fraud alerts.
 *
 * Base URL: /api/fraud
 *
 * This controller handles two main operations:
 * 1. Retrieving existing fraud alerts for a vacancy
 * 2. Running fraud analysis to detect new fraud patterns
 */
@RestController  // Marks this class as a REST API controller
@RequestMapping("/api/fraud")  // All endpoints start with /api/fraud
@Slf4j  // Lombok annotation - generates a logger named 'log'
public class FraudController {

    @Autowired  // Spring automatically injects the FraudDetectionService
    private FraudDetectionService fraudDetectionService;

    /**
     * ENDPOINT 1: GET /api/fraud/{vacancyId}
     *
     * Purpose: Retrieves all fraud alerts for a specific vacancy
     * Use Case: Admin/Auditor wants to see if any fraud was detected for an exam
     *
     * Example Request:
     *   GET http://localhost:8080/api/fraud/123e4567-e89b-12d3-a456-426614174000
     *
     * Example Response:
     *   [
     *     {
     *       "vacancyId": "123e4567-e89b-12d3-a456-426614174000",
     *       "alertType": "PAPER_LEAK",
     *       "suspectCount": 650,
     *       "patternHash": "abc123...",
     *       "timestamp": "2024-01-15T10:30:00"
     *     }
     *   ]
     *
     * @param vacancyId UUID of the vacancy (extracted from URL path)
     * @return List of fraud alerts wrapped in ResponseEntity
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{vacancyId}")
    public ResponseEntity<List<FraudAlertResponse>> getFraudAlerts(@PathVariable UUID vacancyId) {
        try {
            // Log the request for debugging and audit trail
            log.info("GET /api/fraud/{} - Fetching fraud alerts", vacancyId);

            // Step 1: Call service to fetch all fraud alerts from database
            List<FraudAlertEntity> alerts = fraudDetectionService.getFraudAlerts(vacancyId);

            // Step 2: Convert entities to DTOs (Data Transfer Objects)
            // Why? We don't want to expose internal entity structure to frontend
            List<FraudAlertResponse> responses = alerts.stream()
                    .map(this::mapToResponse)  // Call mapToResponse for each entity
                    .collect(Collectors.toList());

            // Step 3: Log different messages based on results
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

    /**
     * ENDPOINT 2: POST /api/fraud/analyze?vacancyId={vacancyId}
     *
     * Purpose: Runs fraud detection algorithms on exam data
     * Use Case: After exam results are published, admin triggers fraud analysis
     *
     * This endpoint does TWO types of fraud detection:
     * 1. Paper Leak Detection - Finds if 500+ candidates have identical answer patterns
     * 2. Marks Anomaly Detection - Finds if unusually high number of candidates scored >90%
     *
     * Example Request:
     *   POST http://localhost:8080/api/fraud/analyze?vacancyId=123e4567-e89b-12d3-a456-426614174000
     *
     * Example Response (if fraud detected):
     *   [
     *     {
     *       "id": "...",
     *       "vacancyId": "...",
     *       "alertType": "PAPER_LEAK",
     *       "suspectCount": 650,
     *       "patternHash": "abc123...",
     *       "evidenceHash": "def456...",
     *       "timestamp": "2024-01-15T10:30:00"
     *     },
     *     {
     *       "id": "...",
     *       "vacancyId": "...",
     *       "alertType": "MARKS_ANOMALY",
     *       "suspectCount": 450,
     *       "patternHash": "ghi789...",
     *       "evidenceHash": "jkl012...",
     *       "timestamp": "2024-01-15T10:30:05"
     *     }
     *   ]
     *
     * @param vacancyId UUID of the vacancy to analyze
     * @return List of detected fraud alerts (empty if no fraud found)
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/analyze")
    public ResponseEntity<List<FraudAlertEntity>> analyzeFraud(@RequestParam UUID vacancyId) {
        try {
            log.info("POST /api/fraud/analyze - Analyzing fraud for vacancy: {}", vacancyId);

            // Initialize empty list to collect all fraud alerts
            List<FraudAlertEntity> alerts = new ArrayList<>();

            // FRAUD CHECK 1: Paper Leak Detection
            // Algorithm: Groups all exam scores by their marking pattern hash
            // If 500+ candidates have IDENTICAL answer patterns → PAPER LEAK
            log.info("Running paper leak detection...");
            List<FraudAlertEntity> paperLeakAlerts = fraudDetectionService.detectPaperLeak(vacancyId);
            alerts.addAll(paperLeakAlerts);

            if (!paperLeakAlerts.isEmpty()) {
                log.error("🚨 PAPER LEAK DETECTED: {} alerts", paperLeakAlerts.size());
            }

            // FRAUD CHECK 2: Marks Anomaly Detection
            // Algorithm: Calculates percentage of candidates scoring >90%
            // If >30% of candidates scored above 90 → SUSPICIOUS
            log.info("Running marks anomaly detection...");
            List<FraudAlertEntity> marksAlerts = fraudDetectionService.detectMarksAnomaly(vacancyId);
            alerts.addAll(marksAlerts);

            if (!marksAlerts.isEmpty()) {
                log.error("🚨 MARKS ANOMALY DETECTED: {} alerts", marksAlerts.size());
            }

            // Log final summary
            if (alerts.isEmpty()) {
                log.info("✅ Fraud analysis complete: No fraud detected");
            } else {
                log.error("🚨 Fraud analysis complete: {} total alerts generated", alerts.size());
            }

            // Return all detected fraud alerts
            // Note: These are already saved to database by the service methods
            return ResponseEntity.ok(alerts);

        } catch (Exception e) {
            log.error("Failed to analyze fraud: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze fraud: " + e.getMessage());
        }
    }

    /**
     * HELPER METHOD: Maps FraudAlertEntity to FraudAlertResponse DTO
     *
     * Why do we need this?
     * - Entity (FraudAlertEntity) contains all database fields
     * - DTO (FraudAlertResponse) contains only fields frontend needs
     * - This separation follows best practices for API design
     *
     * What gets mapped:
     * - vacancyId → Which exam/vacancy this fraud relates to
     * - alertType → Type of fraud (PAPER_LEAK, OMR_TAMPER, MARKS_ANOMALY)
     * - suspectCount → How many candidates are involved
     * - patternHash → Hash of the fraud pattern (for blockchain verification)
     * - timestamp → When fraud was detected
     *
     * @param entity FraudAlertEntity from database
     * @return FraudAlertResponse DTO for API response
     */
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

/*
 * =====================================================================
 * DETAILED EXPLANATION OF HOW THIS CONTROLLER WORKS
 * =====================================================================
 *
 * SCENARIO 1: Admin wants to check if fraud was detected
 * --------------------------------------------------------
 * 1. Admin clicks "View Fraud Alerts" button in frontend
 * 2. Frontend sends: GET http://localhost:8080/api/fraud/{vacancyId}
 * 3. Spring routes request to getFraudAlerts() method
 * 4. Controller calls fraudDetectionService.getFraudAlerts(vacancyId)
 * 5. Service queries database for all fraud alerts
 * 6. Controller converts entities to DTOs using mapToResponse()
 * 7. Controller returns JSON response with fraud alerts
 * 8. Frontend displays alerts to admin
 *
 *
 * SCENARIO 2: Admin triggers fraud analysis after exam
 * --------------------------------------------------------
 * 1. Admin publishes exam results
 * 2. Admin clicks "Analyze Fraud" button
 * 3. Frontend sends: POST http://localhost:8080/api/fraud/analyze?vacancyId=...
 * 4. Spring routes request to analyzeFraud() method
 * 5. Controller calls fraudDetectionService.detectPaperLeak(vacancyId)
 *    - Service fetches all exam scores
 *    - Groups by marking pattern hash
 *    - If any group > 500 → Creates FraudAlertEntity
 *    - Records on blockchain
 *    - Saves to database
 * 6. Controller calls fraudDetectionService.detectMarksAnomaly(vacancyId)
 *    - Service calculates score statistics
 *    - If >30% scored above 90 → Creates FraudAlertEntity
 *    - Saves to database
 * 7. Controller combines all alerts and returns to frontend
 * 8. Frontend shows fraud detection results to admin
 *
 *
 * KEY FRAUD DETECTION ALGORITHMS
 * --------------------------------
 *
 * PAPER LEAK DETECTION:
 * - Groups candidates by markingHash (hash of their answer pattern)
 * - If 500+ candidates have IDENTICAL answers → Paper was leaked
 * - Example: 650 candidates all answered Q1=A, Q2=C, Q3=B, etc.
 * - This is statistically impossible without paper leak
 *
 * MARKS ANOMALY DETECTION:
 * - Calculates: (candidates with marks > 90) / total candidates
 * - If percentage > 30% → Suspicious
 * - Example: 450 out of 1000 candidates scored above 90%
 * - This indicates either paper leak or answer key leak
 *
 *
 * BLOCKCHAIN INTEGRATION
 * ----------------------
 * When fraud is detected:
 * 1. FraudAlertEntity is created in memory
 * 2. BlockchainService.detectPaperLeakOnChain() is called
 * 3. Transaction is sent to Ethereum smart contract
 * 4. Transaction hash is returned
 * 5. FraudAlertEntity is saved to database with blockchain TX hash
 *
 * This creates an IMMUTABLE audit trail:
 * - Fraud detection can't be hidden or deleted
 * - Anyone can verify on blockchain
 * - Government officials can audit independently
 *
 *
 * ERROR HANDLING
 * --------------
 * Both methods use try-catch:
 * - Catches any exception that occurs
 * - Logs the error with details
 * - Throws RuntimeException with user-friendly message
 * - Spring automatically converts to HTTP 500 error
 *
 *
 * LOGGING LEVELS USED
 * -------------------
 * log.info()  → Normal operations (request received, results returned)
 * log.warn()  → Fraud alerts found (warning level)
 * log.error() → Fraud detected or errors occurred (error level)
 *
 * These logs help with:
 * - Debugging issues
 * - Auditing system usage
 * - Monitoring fraud detection
 *
 * =====================================================================
 */