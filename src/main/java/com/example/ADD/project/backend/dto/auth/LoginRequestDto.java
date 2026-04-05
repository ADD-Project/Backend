package com.example.ADD.project.backend.dto.auth;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String password; // 관리자용 (memberCode)
    private String memberCode; // 일반 회원용
}