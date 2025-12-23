package com.jobchain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private UUID id;
    private UUID vacancyId;
    private String candidateName;
    private String email;
    private String category;
    private String appHash;
    private String status;
    private String blockchainTxHash;
    private LocalDateTime createdAt;
}
