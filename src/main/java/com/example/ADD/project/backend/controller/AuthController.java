package com.example.ADD.project.backend.controller;

import com.example.ADD.project.backend.dto.auth.LoginRequestDto;
import com.example.ADD.project.backend.dto.ApiResponse;
import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/admin/login")
    public ApiResponse<String> adminLogin(@RequestBody LoginRequestDto request) {
        boolean success = authService.adminLogin(request.getPassword());
        if (success) return ApiResponse.success("200", "관리자 로그인 성공", null);
        return ApiResponse.error("401", "인증 실패");
    }

    @PostMapping("/member/login")
    public ApiResponse<Long> memberLogin(@RequestBody LoginRequestDto request) {
        Member member = authService.memberLogin(request.getMemberCode());
        return ApiResponse.success("200", "회원 로그인 성공", member.getMemberId());
    }
}