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
@Table(name = "vacancies")
public class VacancyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "total_posts", nullable = false)
    private int totalPosts;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    @Column(name = "paper_hash", length = 64)
    private String paperHash;
}

