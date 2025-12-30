package com.jobchain.service;

import com.jobchain.entity.ExamScoreEntity;
import com.jobchain.entity.FraudAlertEntity;
import com.jobchain.repository.ExamScoreRepository;
import com.jobchain.repository.FraudAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting and managing fraud in recruitment process.
 * Implements algorithms to detect paper leaks, OMR tampering, and suspicious patterns.
 * All fraud incidents are recorded on blockchain for immutable audit trail.
 *
 * @author JobChain System
 * @version 1.0
 */
@Service
@Slf4j
public class FraudDetectionService {

    @Autowired
    private ExamScoreRepository examScoreRepository;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @Autowired
    private BlockchainService blockchainService;

    private static final int PAPER_LEAK_THRESHOLD = 500;

    public List<FraudAlertEntity> detectPaperLeak(UUID vacancyId) {
        try {
            log.info("Running paper leak detection for vacancyId: {}", vacancyId);

            List<String> hashes =
                    examScoreRepository.findMarkingHashesByVacancyId(vacancyId);

            if (hashes == null || hashes.isEmpty()) {
                log.info("No exam scores found for fraud detection");
                return Collections.emptyList();
            }

            log.info("Analyzing {} answer hashes", hashes.size());

            Map<String, Long> patternCounts = hashes.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(
                            h -> h,
                            Collectors.counting()
                    ));

            List<FraudAlertEntity> fraudAlerts = new ArrayList<>();

            for (Map.Entry<String, Long> entry : patternCounts.entrySet()) {

                int suspectCount = entry.getValue().intValue();
                String patternHash = entry.getKey();

                // 🚨 PAPER LEAK CONDITION
                if (suspectCount > PAPER_LEAK_THRESHOLD) {

                    log.warn("⚠️ PAPER LEAK DETECTED: {} candidates with identical pattern",
                            suspectCount);

                    // Blockchain proof (immutable)
                    String txHash = blockchainService.detectPaperLeakOnChain(
                            vacancyId,
                            suspectCount,
                            patternHash
                    );

                    FraudAlertEntity alert = FraudAlertEntity.builder()
                            .vacancyId(vacancyId)
                            .alertType("PAPER_LEAK")
                            .suspectCount(suspectCount)
                            .patternHash(patternHash)
                            .evidenceHash(sha256(patternHash))
                            .build();

                    FraudAlertEntity saved = fraudAlertRepository.save(alert);
                    fraudAlerts.add(saved);

                    log.error("🚨 Paper leak alert created: id={}, txHash={}",
                            saved.getId(), txHash);
                }
            }

            if (fraudAlerts.isEmpty()) {
                log.info("✅ No paper leak detected");
            }

            return fraudAlerts;

        } catch (Exception e) {
            log.error("Failed to detect paper leak: {}", e.getMessage(), e);
            throw new RuntimeException("Paper leak detection failed", e);
        }
    }

    public boolean detectOMRTampering(String storedQrHash, String scannedQrHash) {
        try {
            log.info("Checking OMR sheet authenticity");

            if (storedQrHash == null || scannedQrHash == null) {
                log.error("Invalid hash values provided for OMR verification");
                return false;
            }

            // Compare hashes
            boolean isGenuine = storedQrHash.equals(scannedQrHash);

            if (isGenuine) {
                log.info("✅ OMR sheet is GENUINE - QR hashes match");
            } else {
                log.error("🚨 OMR TAMPERING DETECTED - QR hashes DO NOT match");
                log.error("Stored hash: {}", storedQrHash);
                log.error("Scanned hash: {}", scannedQrHash);
            }

            return isGenuine;

        } catch (Exception e) {
            log.error("Failed to detect OMR tampering: {}", e.getMessage(), e);
            return false; // Return false if verification fails
        }
    }

    public List<FraudAlertEntity> getFraudAlerts(UUID vacancyId) {
        try {
            log.info("Fetching fraud alerts for vacancyId: {}", vacancyId);

            List<FraudAlertEntity> alerts = fraudAlertRepository.findByVacancyId(vacancyId);

            if (alerts.isEmpty()) {
                log.info("No fraud alerts found for vacancy");
            } else {
                log.warn("Found {} fraud alerts for vacancy", alerts.size());
            }

            return alerts;

        } catch (Exception e) {
            log.error("Failed to fetch fraud alerts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve fraud alerts", e);
        }
    }

    public List<FraudAlertEntity> detectMarksAnomaly(UUID vacancyId) {
        try {
            log.info("Running marks anomaly detection for vacancyId: {}", vacancyId);

            // ✅ FETCH ONLY MARKS (NO LOB)
            List<Double> marksList =
                    examScoreRepository.findMarksByVacancyId(vacancyId);

            if (marksList == null || marksList.isEmpty()) {
                log.info("No scores found for anomaly detection");
                return Collections.emptyList();
            }

            // Calculate statistics
            double avgMarks = marksList.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            long highScorers = marksList.stream()
                    .filter(m -> m > 90)
                    .count();

            double highScorerPercentage =
                    (highScorers * 100.0) / marksList.size();

            log.info("Marks statistics - Avg: {}, High scorers (>90): {} ({}%)",
                    avgMarks, highScorers, highScorerPercentage);

            List<FraudAlertEntity> alerts = new ArrayList<>();

            // 🚨 MARKS ANOMALY CONDITION
            if (highScorerPercentage > 30.0) {

                log.warn("⚠️ MARKS ANOMALY DETECTED: {}% candidates scored above 90",
                        highScorerPercentage);

                String patternHash =
                        sha256("MARKS_ANOMALY_" + avgMarks + "_" + highScorerPercentage);

                String evidenceHash =
                        sha256("AVG:" + avgMarks + "|HIGH:" + highScorers);

                FraudAlertEntity alert = FraudAlertEntity.builder()
                        .vacancyId(vacancyId)
                        .alertType("MARKS_ANOMALY")
                        .suspectCount((int) highScorers)
                        .patternHash(patternHash)
                        .evidenceHash(evidenceHash)
                        .build();

                FraudAlertEntity saved = fraudAlertRepository.save(alert);
                alerts.add(saved);

                log.error("🚨 Marks anomaly alert created: id={}", saved.getId());
            } else {
                log.info("✅ No marks anomaly detected. Score distribution is normal.");
            }

            return alerts;

        } catch (Exception e) {
            log.error("Failed to detect marks anomaly: {}", e.getMessage(), e);
            throw new RuntimeException("Marks anomaly detection failed", e);
        }
    }

    private String generateEvidenceHash(List<ExamScoreEntity> suspects) {
        try {
            // Concatenate all application IDs
            String evidence = suspects.stream()
                    .map(s -> s.getApplicationId().toString())
                    .sorted()
                    .collect(Collectors.joining(","));

            return sha256(evidence);

        } catch (Exception e) {
            log.error("Failed to generate evidence hash: {}", e.getMessage());
            return "ERROR_GENERATING_HASH";
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
            log.error("Failed to calculate SHA-256 hash: {}", e.getMessage());
            throw new RuntimeException("Hash calculation failed", e);
        }
    }

    public boolean hasFraudAlert(UUID vacancyId, String alertType) {
        try {
            List<FraudAlertEntity> alerts = fraudAlertRepository.findByVacancyId(vacancyId);

            boolean exists = alerts.stream()
                    .anyMatch(alert -> alert.getAlertType().equals(alertType));

            if (exists) {
                log.warn("Fraud alert exists: vacancyId={}, type={}", vacancyId, alertType);
            }

            return exists;

        } catch (Exception e) {
            log.error("Failed to check fraud alert: {}", e.getMessage());
            return false;
        }
    }

    public int getFraudAlertCount(UUID vacancyId) {
        try {
            List<FraudAlertEntity> alerts = fraudAlertRepository.findByVacancyId(vacancyId);
            return alerts.size();
        } catch (Exception e) {
            log.error("Failed to get fraud alert count: {}", e.getMessage());
            return 0;
        }
    }
}