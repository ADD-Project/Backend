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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentNameHistoryRepository departmentNameHistoryRepository;
    private final MemberDepartmentHistoryRepository historyRepository;

    /**
     * [일반 사용자용] 회원 상세 조회
     * - 회원이 처음 입사했을 당시의 부서 정보를 조회합니다.
     * - 현재 속한 부서가 아닌, "처음 입부했던 부서명"을 반환하는 것이 핵심 요구사항입니다.
     */
    @Transactional(readOnly = true)
    public MemberDetailResponseDto getMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 회원의 부서 이력 조회 (오름차순: 0번째가 첫 부서, 마지막 요소가 현재 부서)
        List<MemberDepartmentHistory> histories = historyRepository.findByMemberOrderByStartDateAsc(member);
        if (histories.isEmpty()) {
            return MemberDetailResponseDto.builder()
                    .memberId(member.getMemberId())
                    .name(member.getName())
                    .profileImagePath(member.getProfileImagePath())
                    .build();
        }

        // 입소 시 부서 정보 (첫 번째 이력)
        MemberDepartmentHistory firstHistory = histories.get(0);
        Long joinDeptId = firstHistory.getDepartment().getDepartmentId();
        LocalDate joinDate = firstHistory.getStartDate();
        
        // 입소 당시 부서명 조회 (해당 일자 기준 부서 이름 이력에서 추출)
        // 부서가 Soft Delete(closedAt) 되었더라도 과거 이력은 유지되므로 정상 조회됨
        String joinDeptName = departmentNameHistoryRepository.findDeptNameAtTime(joinDeptId, joinDate)
                .orElse(firstHistory.getDepartment().getDeptCd()); // 이력이 없으면 부서 코드로 대체

        // 부서 코드 조회
        String joinDeptCode = firstHistory.getDepartment().getDeptCd();

        // 입소 시 해당 부서에 함께 있던 직원(동료)들 리스트 조회
        List<Member> colleagues = historyRepository.findColleaguesAtTime(
                joinDeptId, 
                joinDate, 
                member.getMemberId());

        List<ColleagueDto> colleagueDtos = colleagues.stream()
                .map(c -> ColleagueDto.builder()
                        .memberId(c.getMemberId())
                        .memberCode(c.getMemberCode())
                        .name(c.getName())
                        .profileImagePath(c.getProfileImagePath())
                        .build())
                .collect(Collectors.toList());

        return MemberDetailResponseDto.builder()
                .memberId(member.getMemberId())
                .name(member.getName())
                .profileImagePath(member.getProfileImagePath())
                .joinDate(joinDate)
                .joinDepartmentName(joinDeptName)
                .joinDepartmentCode(joinDeptCode)
                .colleaguesAtJoin(colleagueDtos)
                .build();
    }

    /**
     * [관리자용] 회원 1명 상세 조회
     * - 사번, 입소부서, 입소날짜, 부서이동이력(변경 부서, 변경 날짜) 리스트가 포함됩니다.
     */
    @Transactional(readOnly = true)
    public AdminMemberDetailResponseDto getAdminMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 부서 이동 이력 전체 조회
        List<MemberDepartmentHistory> histories = historyRepository.findByMemberOrderByStartDateAsc(member);
        
        if (histories.isEmpty()) {
            return AdminMemberDetailResponseDto.builder()
                    .memberCode(member.getMemberCode())
                    .build();
        }

        // 입소 정보 (0번째 인덱스가 첫 입사 부서)
        MemberDepartmentHistory firstHistory = histories.get(0);
        Long joinDeptId = firstHistory.getDepartment().getDepartmentId();
        LocalDate joinDate = firstHistory.getStartDate();
        
        // 처음 입사했을 당시의 부서명 조회
        String joinDeptName = departmentNameHistoryRepository.findDeptNameAtTime(joinDeptId, joinDate)
                .orElse(firstHistory.getDepartment().getDeptCd());

        // 부서 이동 이력 리스트 매핑 (각 이력 시작일 시점의 부서명 조회)
        List<DepartmentHistoryDto> historyDtos = histories.stream()
                .map(h -> {
                    String deptName = departmentNameHistoryRepository.findDeptNameAtTime(h.getDepartment().getDepartmentId(), h.getStartDate())
                            .orElse(h.getDepartment().getDeptCd());
                    return DepartmentHistoryDto.builder()
                            .departmentName(deptName)
                            .startDate(h.getStartDate())
                            .build();
                })
                .collect(Collectors.toList());

        return AdminMemberDetailResponseDto.builder()
                .memberCode(member.getMemberCode())
                .joinDepartmentName(joinDeptName)
                .joinDate(joinDate)
                .departmentHistories(historyDtos)
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

    /**
     * [관리자용] 회원 전체 목록 조회 (페이지네이션 적용)
     */
    @Transactional(readOnly = true)
    public Page<MemberSearchResponseDto> getAllMembersAdmin(Pageable pageable) {
        return memberRepository.findAll(pageable)
                .map(m -> MemberSearchResponseDto.builder()
                        .memberId(m.getMemberId())
                        .name(m.getName())
                        .profileImagePath(m.getProfileImagePath())
                        .build());
    }

    @Transactional(readOnly = true)
    public List<MemberSearchResponseDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(m -> MemberSearchResponseDto.builder().memberId(m.getMemberId()).name(m.getName()).profileImagePath(m.getProfileImagePath()).build())
                .collect(Collectors.toList());
    }

    /**
     * 사원 단일 등록 (및 부서 배치)
     */
    @Transactional
    public void registerSingleMember(SingleMemberRegisterRequestDto request) {
        // 이미 등록된 사원(memberCode)인지 확인 후 없으면 신규 저장, 있으면 기존 사원 객체 재사용
        Member member = memberRepository.findByMemberCode(request.getMemberCode())
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .memberCode(request.getMemberCode())
                            .name(request.getName())
                            .profileImagePath(request.getProfileImagePath())
                            .build();
                    return memberRepository.save(newMember);
                });

        // 초기 부서 정보가 주어진 경우 부서 이력(History) 추가
        if (request.getInitialDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getInitialDepartmentId())
                    .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다."));
            
            LocalDate newStartDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

            // 사원의 가장 최근 부서 이력을 확인하여, 새 이력 추가 및 기존 이력 종료일 업데이트 처리
            historyRepository.findTopByMemberOrderByStartDateDesc(member).ifPresent(lastHistory -> {
                Long lastDeptId = lastHistory.getDepartment().getDepartmentId();
                // 이전 이력 시작일 당시의 부서명
                String lastDeptNameAtStart = departmentNameHistoryRepository.findDeptNameAtTime(lastDeptId, lastHistory.getStartDate())
                        .orElse(lastHistory.getDepartment().getDeptCd());
                
                // 새로 배치될 부서의 (입력된 시작일 기준) 부서명
                String newDeptNameAtStart = departmentNameHistoryRepository.findDeptNameAtTime(dept.getDepartmentId(), newStartDate)
                        .orElse(dept.getDeptCd());

                // 핵심 조건: 이전 부서 ID가 다르거나, 부서 ID는 같지만 부서 이름이 달라진 경우 새로운 이력으로 인정
                if (!lastDeptId.equals(dept.getDepartmentId()) || !lastDeptNameAtStart.equals(newDeptNameAtStart)) {
                    // 정상적인 부서 이동(또는 부서명 변경에 따른 갱신)이므로 이전 이력의 종료일을 (새 시작일 - 1일)로 닫음
                    if (lastHistory.getEndDate() == null) {
                        lastHistory.updateEndDate(newStartDate.minusDays(1));
                    }
                } else {
                    // 이전 이력과 부서 ID 및 부서명이 완전히 동일한 경우 중복 삽입 방지
                    throw new RuntimeException("현재 소속된 부서와 완전히 동일한 부서 이력을 추가할 수 없습니다.");
                }
            });

            // 새 부서 이력 저장
            MemberDepartmentHistory history = MemberDepartmentHistory.builder()
                    .member(member)
                    .department(dept)
                    .regionName(com.example.ADD.project.backend.entity.RegionType.from(request.getRegionName()))
                    .startDate(newStartDate)
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