package com.jobchain.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetMeritResponse {

    private List<MeritItemResponse> meritList;
    private String meritHash;
    private String blockchainTxHash;
    private boolean verified;
}
