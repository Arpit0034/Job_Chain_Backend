package com.jobchain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlertResponse {

    private UUID vacancyId;
    private String alertType;
    private int suspectCount;
    private String patternHash;
    private LocalDateTime timestamp;
}
