package com.example.ADD.project.backend.dto.member;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class MemberDetailResponseDto {
    private Long memberId;
    private String name;
    private String profileImagePath;
    private LocalDate joinDate;
    private String joinDepartmentName;
    private List<ColleagueDto> colleaguesAtJoin;
}