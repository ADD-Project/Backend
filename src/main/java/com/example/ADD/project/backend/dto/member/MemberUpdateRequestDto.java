package com.example.ADD.project.backend.dto.member;

import lombok.Data;
import java.util.List;

@Data
public class MemberUpdateRequestDto {
    private String memberCode;
    private String name;
    private List<MemberDepartmentHistoryRequestDto> histories;
}
