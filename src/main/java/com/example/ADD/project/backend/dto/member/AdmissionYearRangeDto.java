package com.example.ADD.project.backend.dto.member;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdmissionYearRangeDto {
    private Integer minYear;
    private Integer maxYear;
}