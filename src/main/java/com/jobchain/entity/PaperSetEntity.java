package com.jobchain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "paper_sets", indexes = {
        @Index(name = "idx_paper_sets_vacancy_id", columnList = "vacancy_id"),
        @Index(name = "idx_set_id", columnList = "set_id"),
        @Index(name = "idx_is_locked", columnList = "is_locked")
})
public class PaperSetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vacancy_id", nullable = false)
    private UUID vacancyId;

    @Column(name = "set_id", nullable = false, length = 10)
    private String setId; // A, B, C, D, E

    @Column(name = "paper_hash", length = 64, nullable = false)
    private String paperHash;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean isLocked = false;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    private String centerId;
}