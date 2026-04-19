package com.example.ADD.project.backend.dto.member;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ColleagueDto {
    private Long memberId;
    private String memberCode;
    private String name;
}