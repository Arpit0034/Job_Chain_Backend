package com.jobchain.service;

import com.jobchain.entity.*;
import com.jobchain.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class ExamService {

    @Autowired
    private ExamScoreRepository examScoreRepository;

    @Autowired
    private MeritListRepository meritListRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Records exam score for a candidate and stores on blockchain.
     *
     * @param vacancyId UUID of the vacancy/exam
     * @param applicationId UUID of the candidate's application
     * @param marks Marks obtained
     * @param markingJson Detailed marking breakdown in JSON format
     * @return Saved ExamScoreEntity
     */
    public ExamScoreEntity recordExamScore(UUID vacancyId, UUID applicationId, double marks, String markingJson) {
        try {
            log.info("Recording exam score: vacancyId={}, applicationId={}, marks={}", vacancyId, applicationId, marks);

            // Validation
            if (marks < 0 || marks > 100) {
                throw new IllegalArgumentException("Marks must be between 0 and 100");
            }

            // Verify application exists
            Optional<ApplicationEntity> application = applicationRepository.findById(applicationId);
            if (!application.isPresent()) {
                throw new IllegalArgumentException("Application not found: " + applicationId);
            }

            if (markingJson == null || markingJson.trim().isEmpty()) {
                throw new IllegalArgumentException("markingJson is mandatory to record exam score");
            }

            // Calculate hash of marking JSON
            String markingHash = sha256(markingJson);

            // Record on blockchain
            String txHash = blockchainService.recordExamScoreOnChain(vacancyId, (int) marks, markingHash);

            // Save to database
            ExamScoreEntity examScore = ExamScoreEntity.builder()
                    .vacancyId(vacancyId)
                    .applicationId(applicationId)
                    .marks(marks)
                    .markingJson(markingJson)
                    .markingHash(markingHash)
                    .blockchainTxHash(txHash)
                    .build();

            ExamScoreEntity savedScore = examScoreRepository.save(examScore);
            log.info("Exam score recorded successfully: id={}, txHash={}", savedScore.getId(), txHash);

            return savedScore;

        } catch (Exception e) {
            log.error("Failed to record exam score: {}", e.getMessage());
            throw new RuntimeException("Exam score recording failed", e);
        }
    }

    /**
     * Publishes merit list for a vacancy.
     * Fetches all applications and scores, sorts by marks, and records on blockchain.
     *
     * @param vacancyId UUID of the vacancy
     * @return Published MeritListEntity
     */
    public MeritListEntity publishMerit(UUID vacancyId) {
        if (meritListRepository.existsByVacancyId(vacancyId)) {
            throw new IllegalStateException(
                    "Merit list already published for this vacancy"
            );
        }
        try {
            log.info("Publishing merit list for vacancyId: {}", vacancyId);

            // Fetch applications (optional validation)
            List<ApplicationEntity> applications =
                    applicationRepository.findByVacancyId(vacancyId);

            if (applications.isEmpty()) {
                throw new IllegalArgumentException(
                        "No applications found for vacancy: " + vacancyId);
            }

            // ✅ FETCH ONLY REQUIRED DATA (NO LOB)
            List<Object[]> scoreRows =
                    examScoreRepository.findApplicationIdAndMarksByVacancyId(vacancyId);

            if (scoreRows.isEmpty()) {
                throw new IllegalArgumentException(
                        "No exam scores found for vacancy: " + vacancyId);
            }

            // Build merit list
            List<Map<String, Object>> meritData = scoreRows.stream()
                    .map(row -> {
                        UUID applicationId = (UUID) row[0];
                        Double marks = (Double) row[1];

                        Map<String, Object> entry = new HashMap<>();
                        entry.put("applicationId", applicationId.toString());
                        entry.put("marks", marks);
                        return entry;
                    })
                    .sorted((m1, m2) ->
                            Double.compare(
                                    (Double) m2.get("marks"),
                                    (Double) m1.get("marks")
                            ))
                    .collect(Collectors.toList());

            // Assign ranks
            for (int i = 0; i < meritData.size(); i++) {
                meritData.get(i).put("rank", i + 1);
            }

            // Convert to JSON
            String meritJson = objectMapper.writeValueAsString(meritData);

            // Hash merit list
            String meritHash = sha256(meritJson);

            // Record on blockchain
            String txHash =
                    blockchainService.publishMeritOnChain(vacancyId, meritHash);

            // Run fraud detection (safe now)
            fraudDetectionService.detectPaperLeak(vacancyId);
            fraudDetectionService.detectMarksAnomaly(vacancyId);

            // Save to DB
            MeritListEntity meritList = MeritListEntity.builder()
                    .vacancyId(vacancyId)
                    .meritJson(meritJson)
                    .meritHash(meritHash)
                    .blockchainTxHash(txHash)
                    .verified(true)
                    .build();

            MeritListEntity saved =
                    meritListRepository.save(meritList);

            log.info("Merit list published successfully: id={}, txHash={}",
                    saved.getId(), txHash);

            return saved;

        } catch (Exception e) {
            log.error("Failed to publish merit list: {}", e.getMessage(), e);
            throw new RuntimeException("Merit list publication failed", e);
        }
    }


    /**
     * Retrieves merit list for a vacancy.
     *
     * @param vacancyId UUID of the vacancy
     * @return MeritListEntity if found
     */
    public MeritListEntity getMeritList(UUID vacancyId) {
        try {
            log.info("Fetching merit list for vacancyId: {}", vacancyId);

            Optional<MeritListEntity> meritList = meritListRepository.findByVacancyId(vacancyId);
            if (!meritList.isPresent()) {
                throw new IllegalArgumentException("Merit list not found for vacancy: " + vacancyId);
            }

            log.info("Merit list retrieved successfully");
            return meritList.get();

        } catch (Exception e) {
            log.error("Failed to fetch merit list: {}", e.getMessage());
            throw new RuntimeException("Merit list retrieval failed", e);
        }
    }

    /**
     * Verifies integrity of merit list by recalculating and comparing hash.
     *
     * @param vacancyId UUID of the vacancy
     * @return true if merit list is untampered, false if tampered
     */
    public boolean verifyMeritIntegrity(UUID vacancyId) {
        try {
            log.info("Verifying merit list integrity for vacancyId: {}", vacancyId);

            // Fetch merit list from database
            Optional<MeritListEntity> meritListOpt = meritListRepository.findByVacancyId(vacancyId);
            if (!meritListOpt.isPresent()) {
                log.warn("Merit list not found for verification");
                return false;
            }

            MeritListEntity meritList = meritListOpt.get();

            // Recalculate hash from current merit JSON
            String recalculatedHash = sha256(meritList.getMeritJson());

            // Compare with stored hash
            boolean isValid = recalculatedHash.equals(meritList.getMeritHash());

            if (isValid) {
                log.info("Merit list integrity verified: VALID");
            } else {
                log.error("Merit list integrity verification FAILED: TAMPERING DETECTED!");
            }

            return isValid;

        } catch (Exception e) {
            log.error("Failed to verify merit list integrity: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Utility method to calculate SHA-256 hash.
     */
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