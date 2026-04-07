package com.example.ADD.project.backend.dto.member;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class DepartmentHistoryDto {
    private String departmentName;
    private LocalDate startDate;
}