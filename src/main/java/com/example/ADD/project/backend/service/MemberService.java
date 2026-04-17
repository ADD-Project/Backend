package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.dto.member.*;
import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.DepartmentNameHistory;
import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.entity.MemberDepartmentHistory;
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
    
    @Transactional(readOnly = true)
    public AdmissionYearRangeDto getAdmissionYearRange() {
        LocalDate minDate = historyRepository.findMinAdmissionDate();
        LocalDate maxDate = historyRepository.findMaxAdmissionDate();
        
        return AdmissionYearRangeDto.builder()
                .minYear(minDate != null ? minDate.getYear() : null)
                .maxYear(maxDate != null ? maxDate.getYear() : null)
                .build();
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
        int admissionYear = 0;

        if (!histories.isEmpty()) {
            MemberDepartmentHistory firstHistory = histories.get(0);
            Long joinDeptId = firstHistory.getDepartment().getDepartmentId();
            LocalDate joinDate = firstHistory.getStartDate();
            admissionYear = joinDate.getYear();
            List<String> joinDeptNames = departmentNameHistoryRepository.findDeptNameAtTime(joinDeptId, joinDate);
            joinDeptName = joinDeptNames.isEmpty() ? firstHistory.getDepartment().getDeptCd() : joinDeptNames.get(0);
        }

        return MemberSearchResponseDto.builder()
                .memberId(member.getMemberId())
                .memberCode(member.getMemberCode())
                .name(member.getName())
                .profileImagePath(member.getProfileImagePath())
                .joinDepartmentName(joinDeptName)
                .admissionYear(admissionYear)
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
                            .profileImagePath(request.getProfileImagePath()) 
                            .build();
                    return memberRepository.save(newMember);
                });

        // 사원의 모든 부서 이력을 리스트로 순회하며 한 번에 처리
        if (request.getHistories() != null && !request.getHistories().isEmpty()) {
            for (MemberDepartmentHistoryRequestDto histReq : request.getHistories()) {
                
                // 1. 부서 검증 (deptCode 기준)
                Department dept = departmentRepository.findByDeptCd(histReq.getDeptCode())
                        .orElseThrow(() -> new RuntimeException("존재하지 않는 부서코드입니다: " + histReq.getDeptCode()));

                LocalDate startDate = histReq.getStartDate() != null ? histReq.getStartDate() : LocalDate.now();

                // 2. 부서명 시점 검증 (사이드이펙트 방지: 부서명을 마음대로 생성하지 않고 예외처리)
                if (histReq.getDeptName() != null && !histReq.getDeptName().trim().isEmpty()) {
                    List<String> deptNamesAtTime = departmentNameHistoryRepository.findDeptNameAtTime(dept.getDepartmentId(), startDate);
                    
                    if (deptNamesAtTime.isEmpty()) {
                        throw new RuntimeException(startDate + " 기준 부서명 이력이 없습니다. 부서코드: " + dept.getDeptCd());
                    }
                    
                    String actualDeptName = deptNamesAtTime.get(0);
                    if (!actualDeptName.equals(histReq.getDeptName())) {
                        throw new RuntimeException("입력한 부서명과 해당 일자의 실제 부서명이 일치하지 않습니다. "
                                + "[입력값=" + histReq.getDeptName() + ", 실제값=" + actualDeptName + "]");
                    }
                }

                // 3. 중복 이력 검사 (종료일 고려 없이 시작일과 부서ID로만 정확히 판별)
                boolean alreadyExists = historyRepository.findByMemberOrderByStartDateAscEndDateAsc(member).stream()
                        .anyMatch(h -> h.getDepartment().getDepartmentId().equals(dept.getDepartmentId())
                                && h.getStartDate().equals(startDate));

                if (alreadyExists) {
                    continue; // 이미 동일한 날짜에 동일한 부서 이력이 있다면 중복 저장 방지
                }

                // 4. 새 부서 이력 저장 (endDate 무시)
                MemberDepartmentHistory history = MemberDepartmentHistory.builder()
                        .member(member)
                        .department(dept)
                        .startDate(startDate)
                        .build();
                historyRepository.save(history);
            }
        }
    }

    /**
     * 1. 부서 엑셀 업로드
     * 형식:
     * 1열: 부서코드
     * 2열: 부서명
     * 3열: 시작일
     */
    @Transactional
    public void importDepartmentsByExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                int rowIndex = i + 1; // 엑셀의 실제 행 번호 (1-based, 헤더 포함)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String deptCode = getCellValueAsString(row.getCell(0)).trim();
                String deptName = getCellValueAsString(row.getCell(1)).trim();
                LocalDate startDate = parseLocalDate(row.getCell(2));

                if (deptCode.isEmpty() || deptName.isEmpty()) {
                    throw new RuntimeException(rowIndex + "행: 부서코드와 부서명은 필수입니다.");
                }
                if (startDate == null) {
                    throw new RuntimeException(rowIndex + "행: 시작일이 올바르지 않습니다.");
                }

                Department department = departmentRepository.findByDeptCd(deptCode)
                        .orElseGet(() -> departmentRepository.save(
                                Department.builder()
                                        .deptCd(deptCode)
                                        .build()
                        ));

                boolean alreadyExists = departmentNameHistoryRepository
                        .findByDepartmentOrderByStartDateAsc(department)
                        .stream()
                        .anyMatch(history ->
                                history.getDeptName().equals(deptName)
                                        && history.getStartDate().equals(startDate)
                        );

                if (alreadyExists) {
                    continue;
                }

                DepartmentNameHistory history = DepartmentNameHistory.builder()
                        .department(department)
                        .deptName(deptName)
                        .startDate(startDate)
                        .build();

                departmentNameHistoryRepository.save(history);
            }

        } catch (Exception e) {
            throw new RuntimeException("부서 엑셀 파일 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 2. 사원 엑셀 업로드
     * 형식:
     * 1열: 사번
     * 2열: 사원명
     * 3열: 부서코드
     * 4열: 부서명
     * 5열: 입소일자
     */
    @Transactional
    public void importMembersByExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            log.info("시트의 마지막 row 줄 : {}", sheet.getLastRowNum());

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                final int rowIndex = i + 1; // 엑셀의 실제 행 번호 (1-based, 헤더 포함)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String memberCode = getCellValueAsString(row.getCell(0)).trim();
                if (memberCode.isEmpty()) continue;

                String name = getCellValueAsString(row.getCell(1)).trim();
                String deptCode = getCellValueAsString(row.getCell(2)).trim();
                String deptName = getCellValueAsString(row.getCell(3)).trim();
                LocalDate startDate = parseLocalDate(row.getCell(4));

                if (startDate == null) {
                    throw new RuntimeException(rowIndex + "행의 입소일자가 올바르지 않습니다.");
                }

                Member member = memberRepository.findByMemberCode(memberCode)
                        .orElseGet(() -> memberRepository.save(
                                Member.builder()
                                        .memberCode(memberCode)
                                        .name(name)
                                        .profileImagePath(null)
                                        .build()
                        ));

                if (deptCode.isEmpty()) {
                    continue;
                }

                Department department = departmentRepository.findByDeptCd(deptCode)
                        .orElseThrow(() ->
                                new RuntimeException(rowIndex + "행: 존재하지 않는 부서코드입니다. deptCode=" + deptCode));

                // 선택 검증: 입소일 기준 부서코드에 대응하는 당시 부서명이 엑셀 값과 맞는지 확인
                if (!deptName.isEmpty()) {
                    List<String> deptNamesAtTime = departmentNameHistoryRepository
                            .findDeptNameAtTime(department.getDepartmentId(), startDate);

                    if (deptNamesAtTime.isEmpty()) {
                        throw new RuntimeException(rowIndex + "행: " + startDate + " 기준 부서명 이력이 없습니다. deptCode=" + deptCode);
                    }

                    String actualDeptName = deptNamesAtTime.get(0);
                    if (!actualDeptName.equals(deptName)) {
                        throw new RuntimeException(rowIndex + "행: 부서코드와 부서명이 일치하지 않습니다. "
                                + "[요청값=" + deptName + ", DB기준=" + actualDeptName + "]");
                    }
                }

                boolean alreadyExists = historyRepository
                        .findByMemberOrderByStartDateAscEndDateAsc(member)
                        .stream()
                        .anyMatch(history ->
                                history.getDepartment().getDepartmentId().equals(department.getDepartmentId())
                                        && history.getStartDate().equals(startDate)
                        );

                if (alreadyExists) {
                    continue;
                }

                MemberDepartmentHistory history = MemberDepartmentHistory.builder()
                        .member(member)
                        .department(department)
                        .startDate(startDate)
                        .build();

                historyRepository.save(history);
            }

        } catch (Exception e) {
            throw new RuntimeException("사원 엑셀 파일 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 날짜 Cell 파싱
     */
    private LocalDate parseLocalDate(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }

                double excelDate = cell.getNumericCellValue();
                Date date = DateUtil.getJavaDate(excelDate);
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }

            if (cell.getCellType() == CellType.STRING) {
                String dateStr = cell.getStringCellValue().trim();
                if (dateStr.isEmpty()) return null;

                if (dateStr.matches("\\d+")) {
                    double excelDate = Double.parseDouble(dateStr);
                    Date date = DateUtil.getJavaDate(excelDate);
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }

                dateStr = dateStr.replace(".", "-").replace("/", "-");
                return LocalDate.parse(dateStr);
            }
        } catch (Exception e) {
            return null;
        }

        return null;
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

        // 부서 이력이 요청에 포함된 경우 기존 이력 전체 삭제 후 새로 동기화 (Full Sync)
        if (request.getHistories() != null) {
            List<MemberDepartmentHistory> existingHistories = historyRepository.findByMemberOrderByStartDateAscEndDateAsc(member);
            historyRepository.deleteAll(existingHistories);
            
            // JPA 지연 쓰기로 인한 중복 제약조건 충돌을 방지하기 위해 delete 쿼리를 먼저 DB에 날려줍니다.
            historyRepository.flush();

            for (MemberDepartmentHistoryRequestDto histReq : request.getHistories()) {
                // 1. 부서 검증 (deptCode 기준)
                Department dept = departmentRepository.findByDeptCd(histReq.getDeptCode())
                        .orElseThrow(() -> new RuntimeException("존재하지 않는 부서코드입니다: " + histReq.getDeptCode()));

                LocalDate startDate = histReq.getStartDate() != null ? histReq.getStartDate() : LocalDate.now();

                // 2. 부서명 시점 검증 (사이드이펙트 방지: 부서명을 마음대로 생성하지 않고 예외처리)
                if (histReq.getDeptName() != null && !histReq.getDeptName().trim().isEmpty()) {
                    List<String> deptNamesAtTime = departmentNameHistoryRepository.findDeptNameAtTime(dept.getDepartmentId(), startDate);

                    if (deptNamesAtTime.isEmpty()) {
                        throw new RuntimeException(startDate + " 기준 부서명 이력이 없습니다. 부서코드: " + dept.getDeptCd());
                    }

                    String actualDeptName = deptNamesAtTime.get(0);
                    if (!actualDeptName.equals(histReq.getDeptName())) {
                        throw new RuntimeException("입력한 부서명과 해당 일자의 실제 부서명이 일치하지 않습니다. "
                                + "[입력값=" + histReq.getDeptName() + ", 실제값=" + actualDeptName + "]");
                    }
                }

                // 3. 새 부서 이력 저장
                MemberDepartmentHistory history = MemberDepartmentHistory.builder()
                        .member(member)
                        .department(dept)
                        .startDate(startDate)
                        .build();
                historyRepository.save(history);
            }
        }
    }

    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        
        // 연관된 MemberDepartmentHistory 데이터도 삭제가 필요한 경우 추가
        // member가 삭제될 때 JPA Cascade나 DB의 ON DELETE CASCADE가 설정되어 있지 않다면 수동으로 삭제해야 합니다.
        List<MemberDepartmentHistory> histories = historyRepository.findByMemberOrderByStartDateAscEndDateAsc(member);
        historyRepository.deleteAll(histories);
        
        memberRepository.delete(member);
    }
}
