package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.dto.member.*;
import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.entity.MemberDepartmentHistory;
import com.example.ADD.project.backend.repository.DepartmentRepository;
import com.example.ADD.project.backend.repository.MemberDepartmentHistoryRepository;
import com.example.ADD.project.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final DepartmentRepository departmentRepository;
    private final MemberDepartmentHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public MemberDetailResponseDto getMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 회원의 첫 번째 부서 이력(입소 정보) 조회
        List<MemberDepartmentHistory> histories = historyRepository.findByMemberOrderByStartDateAsc(member);
        if (histories.isEmpty()) {
            return MemberDetailResponseDto.builder()
                    .memberId(member.getMemberId()).name(member.getName()).profileImagePath(member.getProfileImagePath())
                    .build();
        }

        MemberDepartmentHistory firstHistory = histories.get(0);
        
        // 입소 시 부서에 있던 직원들 리스트 조회
        List<Member> colleagues = historyRepository.findColleaguesAtTime(
                firstHistory.getDepartment().getDepartmentId(), 
                firstHistory.getStartDate(), 
                member.getMemberId());

        List<ColleagueDto> colleagueDtos = colleagues.stream()
                .map(c -> ColleagueDto.builder().memberId(c.getMemberId()).name(c.getName()).profileImagePath(c.getProfileImagePath()).build())
                .collect(Collectors.toList());

        return MemberDetailResponseDto.builder()
                .memberId(member.getMemberId())
                .name(member.getName())
                .profileImagePath(member.getProfileImagePath())
                .joinDate(firstHistory.getStartDate())
                .joinDepartmentName(firstHistory.getDepartment().getDeptCd()) // 스키마 상 임시 표기
                .colleaguesAtJoin(colleagueDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MemberSearchResponseDto> searchMembers(String name) {
        return memberRepository.findByNameContaining(name).stream()
                .map(m -> MemberSearchResponseDto.builder().memberId(m.getMemberId()).name(m.getName()).profileImagePath(m.getProfileImagePath()).build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemberSearchResponseDto> getMembersByAdmissionYear(int year) {
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        return memberRepository.findByCreatedAtBetween(start, end).stream()
                .map(m -> MemberSearchResponseDto.builder().memberId(m.getMemberId()).name(m.getName()).profileImagePath(m.getProfileImagePath()).build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemberSearchResponseDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(m -> MemberSearchResponseDto.builder().memberId(m.getMemberId()).name(m.getName()).profileImagePath(m.getProfileImagePath()).build())
                .collect(Collectors.toList());
    }
}