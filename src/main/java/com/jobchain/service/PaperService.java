package com.jobchain.service;

import com.jobchain.entity.PaperSetEntity;
import com.jobchain.repository.PaperSetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing question paper sets.
 * Handles paper generation, distribution, locking, and blockchain integration.
 */
@Service
@Slf4j
@Transactional
public class PaperService {

    @Autowired
    private PaperSetRepository paperSetRepository;

    @Autowired
    private BlockchainService blockchainService;

    private static final String[] PAPER_SETS = {"A", "B", "C", "D", "E"};

    /**
     * Generates 5 question paper sets (A, B, C, D, E) for a vacancy.
     * Each set has different question order for exam security.
     * Records each set on blockchain.
     *
     * @param vacancyId UUID of the vacancy/exam
     * @return List of generated PaperSetEntity
     */
    public List<PaperSetEntity> generatePaperSets(UUID vacancyId) {
        try {
            log.info("Generating paper sets for vacancy: {}", vacancyId);

            if (vacancyId == null) {
                throw new IllegalArgumentException("Vacancy ID cannot be null");
            }

            // Check if paper sets already exist
            List<PaperSetEntity> existingSets = paperSetRepository.findByVacancyId(vacancyId);
            if (!existingSets.isEmpty()) {
                throw new IllegalStateException("Paper sets already exist for this vacancy");
            }

            List<PaperSetEntity> generatedSets = new ArrayList<>();

            // Generate each paper set (A, B, C, D, E)
            for (String setId : PAPER_SETS) {
                log.info("Generating paper set: {}", setId);

                // Generate paper content (in production, this would be actual question paper)
                String paperContent = generatePaperContent(vacancyId, setId);

                // Calculate paper hash
                String paperHash = sha256(paperContent);

                // Record on blockchain
                String txHash = blockchainService.distributePaperOnChain(vacancyId, setId, paperHash);

                // Create and save paper set entity
                PaperSetEntity paperSet = PaperSetEntity.builder()
                        .vacancyId(vacancyId)
                        .setId(setId)
                        .paperHash(paperHash)
                        .isLocked(false)
                        .timestamp(LocalDateTime.now())
                        .blockchainTxHash(txHash)
                        .build();

                PaperSetEntity savedSet = paperSetRepository.save(paperSet);
                generatedSets.add(savedSet);

                log.info("Paper set {} generated: id={}, txHash={}", setId, savedSet.getId(), txHash);
            }

            log.info("Successfully generated {} paper sets for vacancy", generatedSets.size());
            return generatedSets;

        } catch (Exception e) {
            log.error("Failed to generate paper sets: {}", e.getMessage());
            throw new RuntimeException("Paper set generation failed", e);
        }
    }

    /**
     * Locks all paper sets for a vacancy to prevent tampering.
     * Called when papers are distributed to exam centers.
     *
     * @param vacancyId UUID of the vacancy
     * @param centerId Exam center identifier
     */
    public void lockPaper(UUID vacancyId, String centerId) {
        try {
            log.info("Locking papers for vacancy: {}, center: {}", vacancyId, centerId);

            if (vacancyId == null || centerId == null || centerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Vacancy ID and Center ID are required");
            }

            // Fetch all paper sets for this vacancy
            List<PaperSetEntity> paperSets = paperSetRepository.findByVacancyId(vacancyId);

            if (paperSets.isEmpty()) {
                throw new IllegalArgumentException("No paper sets found for vacancy: " + vacancyId);
            }

            // Lock each paper set
            for (PaperSetEntity paperSet : paperSets) {
                if (paperSet.isLocked()) {
                    log.warn("Paper set {} is already locked", paperSet.getSetId());
                    continue;
                }
                paperSet.setCenterId(centerId);
                paperSet.setLocked(true);
                paperSet.setTimestamp(LocalDateTime.now());
                paperSetRepository.save(paperSet);

                log.info("Paper set {} locked for center: {}", paperSet.getSetId(), centerId);
            }

            log.info("All paper sets locked successfully for center: {}", centerId);

        } catch (Exception e) {
            log.error("Failed to lock papers: {}", e.getMessage());
            throw new RuntimeException("Paper locking failed", e);
        }
    }

    /**
     * Retrieves all paper sets for a vacancy.
     *
     * @param vacancyId UUID of the vacancy
     * @return List of PaperSetEntity
     */
    public List<PaperSetEntity> getPaperSets(UUID vacancyId) {
        try {
            log.info("Fetching paper sets for vacancy: {}", vacancyId);

            List<PaperSetEntity> paperSets = paperSetRepository.findByVacancyId(vacancyId);

            if (paperSets.isEmpty()) {
                log.warn("No paper sets found for vacancy: {}", vacancyId);
            } else {
                log.info("Retrieved {} paper sets", paperSets.size());
            }

            return paperSets;

        } catch (Exception e) {
            log.error("Failed to fetch paper sets: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve paper sets", e);
        }
    }

    /**
     * Retrieves a specific paper set by set ID.
     *
     * @param vacancyId UUID of the vacancy
     * @param setId Paper set identifier (A, B, C, D, E)
     * @return Optional containing PaperSetEntity if found
     */
    public Optional<PaperSetEntity> getPaperSetBySetId(UUID vacancyId, String setId) {
        try {
            log.info("Fetching paper set: vacancyId={}, setId={}", vacancyId, setId);

            List<PaperSetEntity> paperSets = paperSetRepository.findByVacancyId(vacancyId);

            return paperSets.stream()
                    .filter(ps -> ps.getSetId().equals(setId))
                    .findFirst();

        } catch (Exception e) {
            log.error("Failed to fetch paper set: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve paper set", e);
        }
    }

    /**
     * Verifies paper set integrity by recalculating hash.
     *
     * @param paperSetId UUID of the paper set
     * @return true if paper is untampered, false if tampered
     */
    public boolean verifyPaperIntegrity(UUID paperSetId) {
        try {
            log.info("Verifying paper integrity: {}", paperSetId);

            Optional<PaperSetEntity> paperSetOpt = paperSetRepository.findById(paperSetId);
            if (!paperSetOpt.isPresent()) {
                log.warn("Paper set not found for verification");
                return false;
            }

            PaperSetEntity paperSet = paperSetOpt.get();

            // Regenerate paper content
            String paperContent = generatePaperContent(paperSet.getVacancyId(), paperSet.getSetId());

            // Recalculate hash
            String recalculatedHash = sha256(paperContent);

            // Compare with stored hash
            boolean isValid = recalculatedHash.equals(paperSet.getPaperHash());

            if (isValid) {
                log.info("Paper integrity verified: VALID");
            } else {
                log.error("Paper integrity verification FAILED: TAMPERING DETECTED!");
            }

            return isValid;

        } catch (Exception e) {
            log.error("Failed to verify paper integrity: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Unlocks paper sets (admin operation for corrections).
     *
     * @param vacancyId UUID of the vacancy
     */
    public void unlockPapers(UUID vacancyId) {
        try {
            log.warn("ADMIN ACTION: Unlocking papers for vacancy: {}", vacancyId);

            List<PaperSetEntity> paperSets = paperSetRepository.findByVacancyId(vacancyId);

            for (PaperSetEntity paperSet : paperSets) {
                paperSet.setLocked(false);
                paperSetRepository.save(paperSet);
            }

            log.warn("All paper sets unlocked for vacancy: {}", vacancyId);

        } catch (Exception e) {
            log.error("Failed to unlock papers: {}", e.getMessage());
            throw new RuntimeException("Paper unlocking failed", e);
        }
    }

    /**
     * Generates paper content for a specific set.
     * In production, this would fetch actual questions from question bank.
     *
     * @param vacancyId UUID of the vacancy
     * @param setId Paper set identifier
     * @return Paper content as string
     */
    private String generatePaperContent(UUID vacancyId, String setId) {
        // In production, this would:
        // 1. Fetch questions from question bank
        // 2. Randomize question order based on setId
        // 3. Generate complete paper with instructions

        // For now, generate mock content
        StringBuilder content = new StringBuilder();
        content.append("VACANCY ID: ").append(vacancyId).append("\n");
        content.append("PAPER SET: ").append(setId).append("\n");
        content.append("TOTAL QUESTIONS: 100\n");
        content.append("DURATION: 3 hours\n");
        content.append("TIMESTAMP: ").append(LocalDateTime.now()).append("\n");

        // Add mock questions
        for (int i = 1; i <= 100; i++) {
            content.append("Q").append(i).append(": Question ").append(i)
                    .append(" for set ").append(setId).append("\n");
        }

        return content.toString();
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

    /**
     * Gets count of paper sets for a vacancy.
     *
     * @param vacancyId UUID of the vacancy
     * @return Count of paper sets
     */
    public int getPaperSetCount(UUID vacancyId) {
        try {
            List<PaperSetEntity> paperSets = paperSetRepository.findByVacancyId(vacancyId);
            return paperSets.size();
        } catch (Exception e) {
            log.error("Failed to get paper set count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Checks if all paper sets are locked for a vacancy.
     *
     * @param vacancyId UUID of the vacancy
     * @return true if all sets are locked, false otherwise
     */
    public boolean areAllPapersLocked(UUID vacancyId) {
        try {
            List<PaperSetEntity> paperSets = paperSetRepository.findByVacancyId(vacancyId);

            if (paperSets.isEmpty()) {
                return false;
            }

            return paperSets.stream().allMatch(PaperSetEntity::isLocked);

        } catch (Exception e) {
            log.error("Failed to check paper lock status: {}", e.getMessage());
            return false;
        }
    }
}