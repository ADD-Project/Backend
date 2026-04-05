package com.example.ADD.project.backend.dto.member;

import lombok.Data;

@Data
public class MemberUpdateRequestDto {
    private String name;
    private String profileImagePath;
}