package com.jobchain.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApplicationRequest {

    @NotNull
    private UUID vacancyId;

    @NotBlank
    private String candidateName;

    @Email
    private String email;

    @NotBlank
    private String category;

    @Positive
    private double marks10;

    @Positive
    private double marks12;
}
