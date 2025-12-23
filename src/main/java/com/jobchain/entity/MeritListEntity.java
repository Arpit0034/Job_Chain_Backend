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
@Table(name = "merit_lists", indexes = {
        @Index(name = "idx_merit_lists_vacancy_id", columnList = "vacancy_id"),
        @Index(name = "idx_verified", columnList = "verified")
})
public class MeritListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vacancy_id", nullable = false, unique = true)
    private UUID vacancyId;

    @Lob
    @Column(name = "merit_json", columnDefinition = "TEXT", nullable = false)
    private String meritJson;

    @Column(name = "merit_hash", length = 64, nullable = false)
    private String meritHash;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}