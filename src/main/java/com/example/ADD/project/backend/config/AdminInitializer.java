package com.example.ADD.project.backend.config;

import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.entity.Role;
import com.example.ADD.project.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // ddl-auto: create로 인해 테이블이 초기화되거나 
        // 아직 관리자 계정이 없는 경우 애플리케이션 시작 시점에 자동으로 생성합니다.
        if (memberRepository.findByMemberCode("00000000").isEmpty()) {
            Member admin = Member.builder()
                    .memberCode("00000000")
                    .name("관리자")
                    .role(Role.ADMIN)
                    .build();
            
            // 첫 번째로 저장되는 엔티티이므로 Auto Increment 전략에 의해 자동으로 member_id = 1이 부여됩니다.
            memberRepository.save(admin);
        }
    }
}