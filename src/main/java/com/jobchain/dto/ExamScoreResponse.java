package com.jobchain.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamScoreResponse {

    private UUID id;
    private UUID applicationId;
    private double marks;
    private String markingHash;
    private String blockchainTxHash;
}
