package com.example.ADD.project.backend.dto.member;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberSearchResponseDto {
    private Long memberId;
    private String memberCode;
    private String name;
    private String profileImagePath;
    private String joinDepartmentName;
}