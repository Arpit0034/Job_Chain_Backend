package com.jobchain.controller;

import com.jobchain.entity.PaperSetEntity;
import com.jobchain.service.PaperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/paper")
@Slf4j
public class PaperController {

    @Autowired
    private PaperService paperService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/generate-sets")
    public ResponseEntity<List<PaperSetEntity>> generatePaperSets(
            @RequestParam UUID vacancyId) {
        try {
            log.info("POST /api/paper/generate-sets - Generating paper sets for vacancy: {}",
                    vacancyId);

            List<PaperSetEntity> paperSets = paperService.generatePaperSets(vacancyId);

            log.info("Generated {} paper sets successfully", paperSets.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(paperSets);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw new RuntimeException("Validation failed: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.error("State error: {}", e.getMessage());
            throw new RuntimeException("Operation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate paper sets: {}", e.getMessage());
            throw new RuntimeException("Failed to generate paper sets: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/lock")
    public ResponseEntity<String> lockPaper(
            @RequestParam UUID vacancyId,
            @RequestParam String centerId) {
        try {
            log.info("POST /api/paper/lock - Locking paper for vacancy: {}, center: {}",
                    vacancyId, centerId);

            paperService.lockPaper(vacancyId, centerId);

            String message = "Paper sets locked successfully for center: " + centerId;
            log.info(message);
            return ResponseEntity.ok(message);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw new RuntimeException("Validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to lock paper: {}", e.getMessage());
            throw new RuntimeException("Failed to lock paper: " + e.getMessage());
        }
    }

    @GetMapping("/{vacancyId}")
    public ResponseEntity<List<PaperSetEntity>> getPaperSets(@PathVariable UUID vacancyId) {
        try {
            log.info("GET /api/paper/{} - Fetching paper sets", vacancyId);

            List<PaperSetEntity> paperSets = paperService.getPaperSets(vacancyId);

            log.info("Retrieved {} paper sets", paperSets.size());
            return ResponseEntity.ok(paperSets);

        } catch (Exception e) {
            log.error("Failed to fetch paper sets: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch paper sets: " + e.getMessage());
        }
    }
}