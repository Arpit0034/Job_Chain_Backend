package com.jobchain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing OMR (Optical Mark Recognition) sheet records.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "omr_records", indexes = {
        @Index(name = "idx_candidate_id", columnList = "candidate_id"),
        @Index(name = "idx_omr_records_vacancy_id", columnList = "vacancy_id"),
        @Index(name = "idx_scan_timestamp", columnList = "scan_timestamp")
})
public class OMRRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "candidate_id", nullable = false, length = 100)
    private String candidateId;

    @Column(name = "vacancy_id", nullable = false)
    private UUID vacancyId;

    @Lob
    @Column(name = "omr_json", columnDefinition = "TEXT", nullable = false)
    private String omrJson;

    @Column(name = "omr_hash", length = 64, nullable = false)
    private String omrHash;

    @Column(name = "qr_code_data", length = 500, nullable = false)
    private String qrCodeData;

    @Column(name = "scan_timestamp", nullable = false)
    private LocalDateTime scanTimestamp;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;
}