package com.example.ADD.project.backend.dto.member;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AdminMemberDetailResponseDto {
    private String memberCode;
    private String joinDepartmentName;
    private LocalDate joinDate;
    private List<DepartmentHistoryDto> departmentHistories;
}