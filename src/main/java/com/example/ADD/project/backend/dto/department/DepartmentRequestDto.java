package com.example.ADD.project.backend.dto.department;

import lombok.Data;

@Data
public class DepartmentRequestDto {
    private String deptCd;
    private String deptName; // DepartmentNameHistory 처리를 위해 임시 포함
}