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
@Table(name = "answer_keys", indexes = {
        @Index(name = "idx_vacancy_id", columnList = "vacancy_id"),
        @Index(name = "idx_is_finalized", columnList = "is_finalized")
})
public class AnswerKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vacancy_id", nullable = false)
    private UUID vacancyId;

    @Lob
    @Column(name = "answer_key_json", columnDefinition = "TEXT", nullable = false)
    private String answerKeyJson;

    @Column(name = "answer_key_hash", length = 64, nullable = false)
    private String answerKeyHash;

    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private boolean isFinalized = false;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}