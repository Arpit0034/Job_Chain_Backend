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
@Table(name = "applications", indexes = {
        @Index(name = "idx_applications_vacancy_id", columnList = "vacancy_id"),
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_status", columnList = "status")
})
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vacancy_id", nullable = false)
    private UUID vacancyId;

    @Column(name = "candidate_name", nullable = false, length = 200)
    private String candidateName;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "category", nullable = false, length = 10)
    private String category; // UR, OBC, SC, ST

    @Column(name = "marks_10", nullable = false)
    private double marks10;

    @Column(name = "marks_12", nullable = false)
    private double marks12;

    @Lob
    @Column(name = "app_json", columnDefinition = "TEXT")
    private String appJson;

    @Column(name = "app_hash", length = 64, nullable = false)
    private String appHash;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "SUBMITTED";

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}