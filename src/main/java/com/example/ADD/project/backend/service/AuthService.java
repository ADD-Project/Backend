package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.entity.Role;
import com.example.ADD.project.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public boolean adminLogin(String memberCode) {
        Member admin = memberRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("관리자 계정(ID:1)이 세팅되어 있지 않습니다."));
        
        if (admin.getRole() != Role.ADMIN) {
             throw new RuntimeException("해당 계정은 관리자 권한이 없습니다.");
        }

        return admin.getMemberCode().equals(memberCode);
    }

    @Transactional(readOnly = true)
    public Member memberLogin(String memberCode) {
        return memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new RuntimeException("해당 고유번호를 가진 회원이 존재하지 않습니다."));
    }

    @Transactional
    public void changeAdminPassword(String currentPassword, String newPassword) {
        Member admin = memberRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("관리자 계정을 찾을 수 없습니다."));

        if (!admin.getMemberCode().equals(currentPassword)) {
            throw new RuntimeException("현재 비밀번호(고유번호)가 일치하지 않습니다.");
        }
        
        // 비밀번호 유효성 검사: 영문자와 숫자를 포함하여 8자리 이상 (특수문자는 허용)
        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d\\W_]{8,}$";
        if (!Pattern.matches(passwordPattern, newPassword)) {
            throw new RuntimeException("비밀번호는 영문자와 숫자를 포함하여 8자리 이상이어야 합니다.");
        }

        admin.updateMemberCode(newPassword);
    }
}