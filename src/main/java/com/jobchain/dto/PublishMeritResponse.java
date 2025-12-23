package com.jobchain.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishMeritResponse {

    private UUID vacancyId;
    private List<MeritItemResponse> meritList;
    private String meritHash;
    private String blockchainTxHash;
}
