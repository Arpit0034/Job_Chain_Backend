package com.jobchain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordExamScoreRequest {

    @NotNull
    private UUID vacancyId;

    @NotNull
    private UUID applicationId;

    @Positive
    private double marks;

    @NotBlank
    private String markingJson;

}
