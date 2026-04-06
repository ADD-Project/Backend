package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.dto.member.*;
import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.entity.MemberDepartmentHistory;
import com.example.ADD.project.backend.repository.DepartmentNameHistoryRepository;
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
    private final DepartmentNameHistoryRepository departmentNameHistoryRepository;
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
        Long deptId = firstHistory.getDepartment().getDepartmentId();
        LocalDate joinDate = firstHistory.getStartDate();
        
        // 당시 부서명 조회 (해당 일자 기준 이력에서 이름 추출)
        String joinDeptName = departmentNameHistoryRepository.findDeptNameAtTime(deptId, joinDate)
                .orElse(firstHistory.getDepartment().getDeptCd()); // 이력이 없으면 부서 코드로 대체

        // 입소 시 부서에 있던 직원들 리스트 조회
        List<Member> colleagues = historyRepository.findColleaguesAtTime(
                deptId, 
                joinDate, 
                member.getMemberId());

        List<ColleagueDto> colleagueDtos = colleagues.stream()
                .map(c -> ColleagueDto.builder().memberId(c.getMemberId()).name(c.getName()).profileImagePath(c.getProfileImagePath()).build())
                .collect(Collectors.toList());

        return MemberDetailResponseDto.builder()
                .memberId(member.getMemberId())
                .name(member.getName())
                .profileImagePath(member.getProfileImagePath())
                .joinDate(joinDate)
                .joinDepartmentName(joinDeptName)
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
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        return historyRepository.findMembersByAdmissionYear(startOfYear, endOfYear).stream()
                .map(m -> MemberSearchResponseDto.builder().memberId(m.getMemberId()).name(m.getName()).profileImagePath(m.getProfileImagePath()).build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemberSearchResponseDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(m -> MemberSearchResponseDto.builder().memberId(m.getMemberId()).name(m.getName()).profileImagePath(m.getProfileImagePath()).build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void registerSingleMember(SingleMemberRegisterRequestDto request) {
        if (memberRepository.findByMemberCode(request.getMemberCode()).isPresent()) {
            throw new RuntimeException("이미 존재하는 고유번호입니다.");
        }
        Member member = Member.builder()
                .memberCode(request.getMemberCode())
                .name(request.getName())
                .profileImagePath(request.getProfileImagePath())
                .build();
        memberRepository.save(member);

        if (request.getInitialDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getInitialDepartmentId())
                    .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다."));
            MemberDepartmentHistory history = MemberDepartmentHistory.builder()
                    .member(member)
                    .department(dept)
                    .regionName(com.example.ADD.project.backend.entity.RegionType.from(request.getRegionName()))
                    .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                    .build();
            historyRepository.save(history);
        }
    }

    @Transactional
    public void updateMember(Long memberId, MemberUpdateRequestDto request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        if (request.getName() != null) member.updateName(request.getName());
        if (request.getProfileImagePath() != null) member.updateProfileImagePath(request.getProfileImagePath());
    }
}