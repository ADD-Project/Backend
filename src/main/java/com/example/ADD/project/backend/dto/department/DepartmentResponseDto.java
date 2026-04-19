package com.example.ADD.project.backend.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponseDto {
    private Long departmentId;
    private String deptCd;
    private String deptName;
    private LocalDate startDate;
    private Long departmentNameHistoryId;
}