package com.jobchain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyResponse {
    private UUID id;
    private String title;
    private int totalPosts;
    private LocalDateTime createdAt;
    private String blockchainTxHash;
}
