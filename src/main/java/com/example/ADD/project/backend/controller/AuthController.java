package com.example.ADD.project.backend.controller;

import com.example.ADD.project.backend.dto.auth.LoginRequestDto;
import com.example.ADD.project.backend.dto.ApiResponse;
import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/admin/login")
    public ApiResponse<String> adminLogin(@RequestBody LoginRequestDto request, HttpServletRequest servletRequest) {
        // 관리자는 password 필드에 memberCode("00000000" 등)를 담아서 보낸다고 가정
        boolean success = authService.adminLogin(request.getPassword());
        if (success) {
            HttpSession session = servletRequest.getSession();
            session.setAttribute("LOGIN_MEMBER_ROLE", "ADMIN");
            session.setAttribute("LOGIN_MEMBER_ID", 1L); // 관리자 계정 ID (1L)를 하드코딩
            return ApiResponse.success("200", "관리자 로그인 성공", null);
        }
        return ApiResponse.error("401", "인증 실패");
    }

    @PostMapping("/member/login")
    public ApiResponse<Long> memberLogin(@RequestBody LoginRequestDto request, HttpServletRequest servletRequest) {
        Member member = authService.memberLogin(request.getMemberCode());
        HttpSession session = servletRequest.getSession();
        session.setAttribute("LOGIN_MEMBER_ROLE", member.getRole().name());
        session.setAttribute("LOGIN_MEMBER_ID", member.getMemberId());
        return ApiResponse.success("200", "회원 로그인 성공", member.getMemberId());
    }
    
    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.success("200", "로그아웃 성공", null);
    }

    @PostMapping("/admin/password")
    public ApiResponse<String> changeAdminPassword(@RequestBody Map<String, String> request) {
        log.info("관리자 비밀번호 변경");
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        
        if (currentPassword == null || newPassword == null) {
            return ApiResponse.error("400", "비밀번호 값이 전달되지 않았습니다.");
        }
        
        authService.changeAdminPassword(currentPassword, newPassword);
        return ApiResponse.success("200", "관리자 비밀번호 변경 성공", null);
    }
}