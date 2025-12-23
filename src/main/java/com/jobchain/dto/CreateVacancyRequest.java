package com.jobchain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVacancyRequest {

    @NotBlank
    private String title;

    @Positive
    private int totalPosts;
}
