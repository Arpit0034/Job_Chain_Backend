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
@Table(name = "fraud_alerts", indexes = {
        @Index(name = "idx_fraud_alerts_vacancy_id", columnList = "vacancy_id"),
        @Index(name = "idx_alert_type", columnList = "alert_type"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class FraudAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vacancy_id", nullable = false)
    private UUID vacancyId;

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType; // PAPER_LEAK, OMR_TAMPER, MARKS_ANOMALY

    @Column(name = "suspect_count", nullable = false)
    private int suspectCount;

    @Column(name = "pattern_hash", length = 64, nullable = false)
    private String patternHash;

    @Column(name = "evidence_hash", length = 64)
    private String evidenceHash;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}