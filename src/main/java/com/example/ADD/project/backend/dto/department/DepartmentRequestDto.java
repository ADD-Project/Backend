package com.example.ADD.project.backend.dto.department;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DepartmentRequestDto {
    private String deptCd;
    private String deptName; // DepartmentNameHistory 처리를 위해 포함
    private LocalDate startDate; // 부서명 변경 혹은 생성 시점
    private Boolean isNew; // 신규 생성 여부 (선택적)
}