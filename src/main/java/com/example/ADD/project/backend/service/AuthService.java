package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final MemberRepository memberRepository;

    public boolean adminLogin(String password) {
        Member admin = memberRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("관리자 계정(ID:1)이 세팅되어 있지 않습니다."));
        return admin.getMemberCode().equals(password);
    }

    public Member memberLogin(String memberCode) {
        return memberRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new RuntimeException("해당 고유번호를 가진 회원이 존재하지 않습니다."));
    }
}