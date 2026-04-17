package com.example.ADD.project.backend.dto.member;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MemberDepartmentHistoryRequestDto {
    private String deptCode;
    private String deptName;
    private LocalDate startDate;
}