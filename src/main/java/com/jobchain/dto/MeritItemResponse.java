package com.jobchain.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeritItemResponse {

    private int rank;
    private String candidateName;
    private double marks;
    private String category;
}
