package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.dto.member.*;
import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.DepartmentNameHistory;
import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.entity.MemberDepartmentHistory;
import com.example.ADD.project.backend.entity.RegionType;
import com.example.ADD.project.backend.repository.DepartmentNameHistoryRepository;
import com.example.ADD.project.backend.repository.DepartmentRepository;
import com.example.ADD.project.backend.repository.MemberDepartmentHistoryRepository;
import com.example.ADD.project.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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

        // 회원의 부서 이력 조회 (시작일 오름차순, 시작일이 같으면 종료일 오름차순)
        List<MemberDepartmentHistory> histories = historyRepository.findByMemberOrderByStartDateAscEndDateAsc(member);
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
        List<String> joinDeptNames = departmentNameHistoryRepository.findDeptNameAtTime(joinDeptId, joinDate);
        String joinDeptName = joinDeptNames.isEmpty() ? firstHistory.getDepartment().getDeptCd() : joinDeptNames.get(0);

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
     * - 사번, 사원명, 프로필이미지, 입소부서, 입소날짜, 부서이동이력(변경 부서, 변경 날짜) 리스트가 포함됩니다.
     */
    @Transactional(readOnly = true)
    public AdminMemberDetailResponseDto getAdminMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 부서 이동 이력 전체 조회 (시작일 오름차순, 시작일이 같으면 종료일 오름차순)
        List<MemberDepartmentHistory> histories = historyRepository.findByMemberOrderByStartDateAscEndDateAsc(member);

        if (histories.isEmpty()) {
            return AdminMemberDetailResponseDto.builder()
                    .memberCode(member.getMemberCode())
                    .name(member.getName())
                    .profileImagePath(member.getProfileImagePath())
                    .build();
        }

        // 입소 정보 (0번째 인덱스가 첫 입사 부서)
        MemberDepartmentHistory firstHistory = histories.get(0);
        Long joinDeptId = firstHistory.getDepartment().getDepartmentId();
        LocalDate joinDate = firstHistory.getStartDate();

        // 처음 입사했을 당시의 부서명 조회
        List<String> joinDeptNames = departmentNameHistoryRepository.findDeptNameAtTime(joinDeptId, joinDate);
        String joinDeptName = joinDeptNames.isEmpty() ? firstHistory.getDepartment().getDeptCd() : joinDeptNames.get(0);

        // 부서 이동 이력 리스트 매핑 (각 이력 시작일 시점의 부서명 조회)
        List<DepartmentHistoryDto> historyDtos = histories.stream()
                .map(h -> {
                    List<String> deptNames = departmentNameHistoryRepository.findDeptNameAtTime(h.getDepartment().getDepartmentId(), h.getStartDate());
                    String deptName = deptNames.isEmpty() ? h.getDepartment().getDeptCd() : deptNames.get(0);
                    return DepartmentHistoryDto.builder()
                            .departmentName(deptName)
                            .startDate(h.getStartDate())
                            .build();
                })
                .collect(Collectors.toList());

        return AdminMemberDetailResponseDto.builder()
                .memberCode(member.getMemberCode())
                .name(member.getName())
                .profileImagePath(member.getProfileImagePath())
                .joinDepartmentName(joinDeptName)
                .joinDate(joinDate)
                .departmentHistories(historyDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MemberSearchResponseDto> searchMembers(String name) {
        return memberRepository.findByNameContaining(name).stream()
                .map(this::createMemberSearchResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemberSearchResponseDto> getMembersByAdmissionYear(int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        return historyRepository.findMembersByAdmissionYear(startOfYear, endOfYear).stream()
                .map(this::createMemberSearchResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * [관리자용] 회원 전체 목록 조회 (페이지네이션 적용)
     */
    @Transactional(readOnly = true)
    public Page<MemberSearchResponseDto> getAllMembersAdmin(Pageable pageable) {
        return memberRepository.findAll(pageable)
                .map(this::createMemberSearchResponseDto);
    }

    @Transactional(readOnly = true)
    public List<MemberSearchResponseDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(this::createMemberSearchResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 사원 목록 조회 시 공통으로 사용되는 DTO 생성 메서드
     * 사원의 최초 입사 부서를 조회하여 DTO에 포함시킵니다.
     */
    private MemberSearchResponseDto createMemberSearchResponseDto(Member member) {
        List<MemberDepartmentHistory> histories = historyRepository.findByMemberOrderByStartDateAscEndDateAsc(member);
        String joinDeptName = null;

        if (!histories.isEmpty()) {
            MemberDepartmentHistory firstHistory = histories.get(0);
            Long joinDeptId = firstHistory.getDepartment().getDepartmentId();
            LocalDate joinDate = firstHistory.getStartDate();
            List<String> joinDeptNames = departmentNameHistoryRepository.findDeptNameAtTime(joinDeptId, joinDate);
            joinDeptName = joinDeptNames.isEmpty() ? firstHistory.getDepartment().getDeptCd() : joinDeptNames.get(0);
        }

        return MemberSearchResponseDto.builder()
                .memberId(member.getMemberId())
                .memberCode(member.getMemberCode())
                .name(member.getName())
                .profileImagePath(member.getProfileImagePath())
                .joinDepartmentName(joinDeptName)
                .build();
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
                            .profileImagePath(null) // 단일 등록 시 프로필 이미지는 기본 null
                            .build();
                    return memberRepository.save(newMember);
                });

        // 초기 부서 정보가 주어진 경우 부서 이력(History) 추가
        if (request.getInitialDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getInitialDepartmentId())
                    .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다."));

            LocalDate newStartDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

            // 사원의 가장 최근 부서 이력을 확인하여, 새 이력 추가 및 기존 이력 종료일 업데이트 처리
            Optional<MemberDepartmentHistory> lastHistoryOpt = historyRepository.findTopByMemberOrderByStartDateDesc(member);

            if (lastHistoryOpt.isPresent()) {
                MemberDepartmentHistory lastHistory = lastHistoryOpt.get();
                Long lastDeptId = lastHistory.getDepartment().getDepartmentId();
                List<String> lastDeptNames = departmentNameHistoryRepository.findDeptNameAtTime(lastDeptId, lastHistory.getStartDate());
                String lastDeptNameAtStart = lastDeptNames.isEmpty() ? lastHistory.getDepartment().getDeptCd() : lastDeptNames.get(0);

                List<String> newDeptNames = departmentNameHistoryRepository.findDeptNameAtTime(dept.getDepartmentId(), newStartDate);
                String newDeptNameAtStart = newDeptNames.isEmpty() ? dept.getDeptCd() : newDeptNames.get(0);

                // 현재 처리하려는 이력이 이전 이력과 완전히 동일한지 확인 (부서 ID, 부서명, 시작일 모두 동일)
                if (lastDeptId.equals(dept.getDepartmentId()) && lastDeptNameAtStart.equals(newDeptNameAtStart) &&
                        lastHistory.getStartDate().equals(newStartDate)) {
                    throw new RuntimeException("현재 소속된 부서와 완전히 동일한 부서 이력을 동일한 시작일로 추가할 수 없습니다.");
                }

                // 부서 이름 이력에 맞추어 종료일 업데이트 로직 삭제
            }

            // 새 부서 이력 저장
            MemberDepartmentHistory history = MemberDepartmentHistory.builder()
                    .member(member)
                    .department(dept)
                    .regionName(RegionType.from(request.getRegionName()))
                    .startDate(newStartDate)
                    .build();
            historyRepository.save(history);
        }
    }

    /**
     * 엑셀 파일 파싱 및 대량 등록 (고유번호, 성명, 당시운영부서코드, 당시운영부서명, 발령코드, 발령명, 시작일, 지역명)
     */
    @Transactional
    public void importMembersByExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트 사용
            log.info("시트의 마지막 row 줄 : " + sheet.getLastRowNum());
            // 헤더(0번 Row)를 제외하고 1번 Row부터 읽기
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String memberCode = getCellValueAsString(row.getCell(0));
                if (memberCode == null || memberCode.trim().isEmpty()) continue; // 고유번호가 없으면 건너뜀

                String name = getCellValueAsString(row.getCell(1));
                String deptCode = getCellValueAsString(row.getCell(2));
                String deptName = getCellValueAsString(row.getCell(3));

                LocalDate startDate = LocalDate.now(); // 기본값 현재 날짜

                // 날짜 컬럼 파싱 (인덱스 6)
                Cell dateCell = row.getCell(7);
                if (dateCell != null) {
                    if (dateCell.getCellType() == CellType.NUMERIC) {
                        if(DateUtil.isCellDateFormatted(dateCell)) {
                            Date date = dateCell.getDateCellValue();
                            if (date != null) {
                                startDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            }
                        } else {
                            try {
                                double excelDate = dateCell.getNumericCellValue();
                                Date date = DateUtil.getJavaDate(excelDate);
                                if (date != null) {
                                    startDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                }
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    } else if(dateCell.getCellType() == CellType.STRING) {
                        String dateStr = dateCell.getStringCellValue();
                        try {
                            if (dateStr.matches("\\d+")) {
                                double excelDate = Double.parseDouble(dateStr);
                                Date date = DateUtil.getJavaDate(excelDate);
                                if (date != null) {
                                    startDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                }
                            } else {
                                // 엑셀에서 날짜 포맷이 'yyyy.MM.dd' 혹은 'yyyy/MM/dd' 등일 경우를 대비하여 하이픈으로 통일
                                dateStr = dateStr.replace(".", "-").replace("/", "-");
                                startDate = LocalDate.parse(dateStr);
                            }
                        } catch (Exception e) {
                            // 날짜 파싱 실패 시 현재 날짜로 대체하거나 에러 처리
                        }
                    }
                }

                String regionNameStr = getCellValueAsString(row.getCell(8));

                // 1. 사원 처리 (없으면 생성)
                Member member = memberRepository.findByMemberCode(memberCode).orElse(null);
                if (member == null) {
                    member = Member.builder()
                            .memberCode(memberCode)
                            .name(name)
                            .profileImagePath(null) // 엑셀 업로드 시 프로필 이미지는 기본 null
                            .build();
                    member = memberRepository.save(member);
                }

                // 2. 부서 처리 (부서 코드로 조회, 없으면 생성)
                if (deptCode != null && !deptCode.trim().isEmpty()) {
                    Department department = departmentRepository.findByDeptCd(deptCode).orElse(null);
                    if (department == null) {
                        department = Department.builder()
                                .deptCd(deptCode)
                                .build();
                        department = departmentRepository.save(department);
                    }

                    // 부서명 이력 처리
                    if (deptName != null && !deptName.trim().isEmpty()) {
                        DepartmentNameHistory lastNameHistory = departmentNameHistoryRepository.findByDepartmentOrderByStartDateAsc(department)
                                .stream().reduce((first, second) -> second).orElse(null);

                        if (lastNameHistory == null || !lastNameHistory.getDeptName().equals(deptName)) {
                            DepartmentNameHistory newNameHistory = DepartmentNameHistory.builder()
                                    .department(department)
                                    .deptName(deptName)
                                    .startDate(startDate)
                                    .build();
                            departmentNameHistoryRepository.save(newNameHistory);
                        }
                    }

                    // 3. 사원 부서 배치 이력 처리
                    final Department finalDept = department;

                    Optional<MemberDepartmentHistory> lastHistoryOpt = historyRepository.findTopByMemberOrderByStartDateDesc(member);
                    boolean skipCurrentRow = false; // 현재 행을 건너뛸지 여부 플래그

                    if (lastHistoryOpt.isPresent()) {
                        MemberDepartmentHistory lastHistory = lastHistoryOpt.get();
                        Long lastDeptId = lastHistory.getDepartment().getDepartmentId();
                        List<String> lastDeptNames = departmentNameHistoryRepository.findDeptNameAtTime(lastDeptId, lastHistory.getStartDate());
                        String lastDeptNameAtStart = lastDeptNames.isEmpty() ? lastHistory.getDepartment().getDeptCd() : lastDeptNames.get(0);

                        List<String> newDeptNames = departmentNameHistoryRepository.findDeptNameAtTime(finalDept.getDepartmentId(), startDate);
                        String newDeptNameAtStart = newDeptNames.isEmpty() ? finalDept.getDeptCd() : newDeptNames.get(0);

                        // 현재 처리하려는 이력이 이전 이력과 완전히 동일한지 확인 (부서 ID, 부서명, 시작일 모두 동일)
                        if (lastDeptId.equals(finalDept.getDepartmentId()) && lastDeptNameAtStart.equals(newDeptNameAtStart) && lastHistory.getStartDate().equals(startDate)) {
                            skipCurrentRow = true; // 완전히 동일한 이력이 이미 존재하면, 이 행은 건너뜀
                        }
                    }

                    if (skipCurrentRow) {
                        continue; // 다음 엑셀 행으로 이동
                    }

                    // RegionType 에러 방지. 지역명이 매칭되지 않으면 BUSAN으로 기본 처리.
                    RegionType regionType;
                    try {
                        regionType = RegionType.from(regionNameStr);
                        if (regionType == null) {
                            regionType = RegionType.BUSAN; // 기본값
                        }
                    } catch (IllegalArgumentException e) {
                        regionType = RegionType.BUSAN; // 예외 발생 시 기본값
                    }

                    // 새 부서 이력 저장
                    MemberDepartmentHistory history = MemberDepartmentHistory.builder()
                            .member(member)
                            .department(finalDept)
                            .regionName(regionType)
                            .startDate(startDate)
                            .build();
                    historyRepository.save(history);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("엑셀 파일 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * Cell의 값을 String으로 안전하게 변환합니다.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 일반 숫자는 String으로 변환 (사번 등이 숫자일 경우를 대비해 long 캐스팅)
                double value = cell.getNumericCellValue();
                if(value == (long) value) {
                    return String.valueOf((long) value);
                }
                return String.valueOf(value);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: // 수식 셀의 경우 계산된 값을 가져옴
                Workbook workbook = cell.getSheet().getWorkbook();
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                return getCellValueAsString(evaluator.evaluateInCell(cell));
            default:
                return "";
        }
    }

    @Transactional
    public void updateMember(Long memberId, MemberUpdateRequestDto request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        if (request.getName() != null) member.updateName(request.getName());
        if (request.getProfileImagePath() != null) member.updateProfileImagePath(request.getProfileImagePath());
    }
}