package com.jobchain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "exam_scores", indexes = {
        @Index(name = "idx_exam_scores_vacancy_id", columnList = "vacancy_id"),
        @Index(name = "idx_application_id", columnList = "application_id"),
        @Index(name = "idx_marks", columnList = "marks")
})
public class ExamScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vacancy_id", nullable = false)
    private UUID vacancyId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "marks", nullable = false)
    private double marks;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "marking_json", columnDefinition = "TEXT")
    private String markingJson;

    @Column(name = "marking_hash", length = 64, nullable = false)
    private String markingHash;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}