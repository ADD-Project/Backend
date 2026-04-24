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
                        .build())
                .collect(Collectors.toList());

        return MemberDetailResponseDto.builder()
                .memberId(member.getMemberId())
                .name(member.getName())
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
                .joinDepartmentName(joinDeptName)
                .admissionYear(admissionYear)
                .build();
    }

    /**
     * 사원 단일 등록 (및 부서 배치)
     */
    @Transactional
    public void registerSingleMember(SingleMemberRegisterRequestDto request) {
        // 이미 등록된 사원(memberCode)인지 확인 후 존재하면 예외 처리
        memberRepository.findByMemberCode(request.getMemberCode()).ifPresent(existingMember -> {
            throw new RuntimeException(existingMember.getName() + "의 고유번호로 사용되고 있는 값입니다.");
        });

        Member member = memberRepository.save(
                Member.builder()
                        .memberCode(request.getMemberCode())
                        .name(request.getName())
                        .build()
        );

        // 사원의 모든 부서 이력을 리스트로 순회하며 한 번에 처리
        if (request.getHistories() != null && !request.getHistories().isEmpty()) {
            for (MemberDepartmentHistoryRequestDto histReq : request.getHistories()) {
                LocalDate startDate = histReq.getStartDate() != null ? histReq.getStartDate() : LocalDate.now();
                Department dept = null;

                // 1. 부서 검증 (deptCode가 있으면 deptCode 우선, 없으면 deptName으로 찾기)
                if (histReq.getDeptCode() != null && !histReq.getDeptCode().trim().isEmpty()) {
                    dept = departmentRepository.findByDeptCd(histReq.getDeptCode())
                            .orElseThrow(() -> new RuntimeException("존재하지 않는 부서코드입니다: " + histReq.getDeptCode()));
                } else if (histReq.getDeptName() != null && !histReq.getDeptName().trim().isEmpty()) {
                    // 부서명 검증 시 띄어쓰기를 모두 없앤 후 비교 (DB에도 띄어쓰기 없는 상태로 저장됨)
                    String normalizedDeptName = histReq.getDeptName().replaceAll("\\s+", "");
                    Optional<DepartmentNameHistory> nameHistoryOpt = departmentNameHistoryRepository
                            .findFirstByDeptNameAndStartDateLessThanEqualOrderByStartDateDesc(normalizedDeptName, startDate);
                    
                    // 만약 특정 시점에 해당하는 게 없으면 가장 예전 이력을 가져오거나, 이름으로 찾을 수 있는 첫 번째 이력을 찾음
                    if (nameHistoryOpt.isEmpty()) {
                        nameHistoryOpt = departmentNameHistoryRepository.findFirstByDeptNameOrderByStartDateAsc(normalizedDeptName);
                    }
                    
                    dept = nameHistoryOpt.map(DepartmentNameHistory::getDepartment)
                            .orElseThrow(() -> new RuntimeException("해당 일자(" + startDate + ") 기준 해당 부서명(" + histReq.getDeptName() + ")을 가진 부서를 찾을 수 없습니다."));
                } else {
                    throw new RuntimeException("부서코드나 부서명 중 하나는 필수입니다.");
                }

                // 2. 부서명 시점 검증 (사이드이펙트 방지: 부서명을 마음대로 생성하지 않고 예외처리)
                if (histReq.getDeptCode() != null && !histReq.getDeptCode().trim().isEmpty() &&
                    histReq.getDeptName() != null && !histReq.getDeptName().trim().isEmpty()) {
                    
                    List<String> deptNamesAtTime = departmentNameHistoryRepository.findDeptNameAtTime(dept.getDepartmentId(), startDate);
                    
                    String actualDeptName = "";
                    if (deptNamesAtTime.isEmpty()) {
                        // 해당 시점의 이력이 없다면, 최초 이력을 가져와서 비교
                        Optional<DepartmentNameHistory> firstHistory = departmentNameHistoryRepository.findFirstByDepartmentOrderByStartDateAsc(dept);
                        if (firstHistory.isPresent()) {
                            actualDeptName = firstHistory.get().getDeptName();
                        } else {
                            throw new RuntimeException(startDate + " 기준 부서명 이력이 없습니다. 부서코드: " + dept.getDeptCd());
                        }
                    } else {
                        actualDeptName = deptNamesAtTime.get(0);
                    }
                    
                    String normalizedReqDeptName = histReq.getDeptName().replaceAll("\\s+", "");
                    if (!actualDeptName.equals(normalizedReqDeptName)) {
                        throw new RuntimeException("입력한 부서명과 해당 일자의 실제 부서명이 일치하지 않습니다. "
                                + "[입력값=" + histReq.getDeptName() + ", 실제값=" + actualDeptName + "]");
                    }
                }

                // 3. 중복 이력 검사 (종료일 고려 없이 시작일과 부서ID로만 정확히 판별)
                Department finalDept = dept;
                Optional<MemberDepartmentHistory> existingHistoryOpt = historyRepository
                        .findByMemberOrderByStartDateAscEndDateAsc(member)
                        .stream()
                        .filter(history -> history.getStartDate().equals(startDate))
                        .findFirst();

                if (existingHistoryOpt.isPresent()) {
                    historyRepository.delete(existingHistoryOpt.get());
                    historyRepository.flush(); // 즉시 삭제 반영
                }

                // 4. 새 부서 이력 저장 (endDate 무시)
                MemberDepartmentHistory history = MemberDepartmentHistory.builder()
                        .member(member)
                        .department(finalDept)
                        .startDate(startDate)
                        .build();
                historyRepository.save(history);
            }
        }
    }

    /**
     * 2. 사원 엑셀 업로드
     * 형식:
     * 1열: 고유번호
     * 2열: 성명
     * 3열: 당시운영부서코드 (비어있을 수 있음)
     * 4열: 당시운영부서명 (비어있을 수 있음)
     * 5열: 시작일
     */
    @Transactional
    public void importMembersByExcel(MultipartFile file) {
        System.out.println("=== importMembersByFile controller 진입 ===");
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            log.info("시트의 마지막 row 줄 : {}", sheet.getLastRowNum());

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                final int rowIndex = i + 1; // 엑셀의 실제 행 번호 (1-based, 헤더 포함)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 행에서 유효한 데이터를 담을 리스트 (빈 셀이나 숨긴 열 무시를 위해)
                List<String> rowData = new java.util.ArrayList<>();
                for (int j = 0; j < Math.max(10, row.getLastCellNum()); j++) {
                    // 셀이 날짜 형식일 경우를 먼저 체크하여 처리
                    Cell cell = row.getCell(j);
                    String val = "";
                    if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        LocalDate ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        val = ld.toString(); // yyyy-MM-dd
                    } else {
                        val = getCellValueAsString(cell);
                    }
                    rowData.add(val.trim()); // 빈 값이더라도 인덱스 유지를 위해 add
                }

                // 데이터가 5개 이상 채워진 행만 유효한 데이터로 간주 (사번, 성명, 시작일은 필수)
                if (rowData.size() < 5) continue;

                String memberCode = rowData.get(0);
                if (memberCode.isEmpty()) continue; // 사번 없으면 스킵

                String name = rowData.get(1);
                String deptCode = rowData.get(2);
                String deptName = rowData.get(3);
                String rawDateStr = rowData.get(4);
                
                LocalDate startDate = parseLocalDateFromString(rawDateStr);

                if (startDate == null) {
                    throw new RuntimeException(rowIndex + "행의 입소일자가 올바르지 않습니다. (원본 데이터: '" + rawDateStr + "')");
                }

                Member member = memberRepository.findByMemberCode(memberCode)
                        .orElseGet(() -> memberRepository.save(
                                Member.builder()
                                        .memberCode(memberCode)
                                        .name(name)
                                        .build()
                        ));

                Department department = null;

                // 부서코드나 부서명이 하나라도 있어야 이력을 추가함
                if (!deptCode.isEmpty() || !deptName.isEmpty()) {
                    if (!deptCode.isEmpty()) {
                        // 1순위: 부서 코드로 조회
                        department = departmentRepository.findByDeptCd(deptCode)
                                .orElseThrow(() -> new RuntimeException(rowIndex + "행: 존재하지 않는 부서코드입니다. deptCode=" + deptCode));
                        
                        // 부서명도 있다면 검증
                        if (!deptName.isEmpty()) {
                            List<String> deptNamesAtTime = departmentNameHistoryRepository
                                    .findDeptNameAtTime(department.getDepartmentId(), startDate);

                            String actualDeptName = "";
                            if (deptNamesAtTime.isEmpty()) {
                                Optional<DepartmentNameHistory> firstHistory = departmentNameHistoryRepository.findFirstByDepartmentOrderByStartDateAsc(department);
                                if (firstHistory.isPresent()) {
                                    actualDeptName = firstHistory.get().getDeptName();
                                } else {
                                    throw new RuntimeException(rowIndex + "행: " + startDate + " 기준 부서명 이력이 없습니다. deptCode=" + deptCode);
                                }
                            } else {
                                actualDeptName = deptNamesAtTime.get(0);
                            }

                            // 검증 시에도 띄어쓰기를 모두 없앤 후 비교합니다.
                            String normalizedReqDeptName = deptName.replaceAll("\\s+", "");
                            if (!actualDeptName.equals(normalizedReqDeptName)) {
                                throw new RuntimeException(rowIndex + "행: 부서코드와 부서명이 일치하지 않습니다. "
                                        + "[요청값=" + deptName + ", DB기준=" + actualDeptName + "]");
                            }
                        }
                    } else { // 2순위: 부서 코드가 비어있고 부서명만 있는 경우
                        String normalizedDeptName = deptName.replaceAll("\\s+", "");
                        Optional<DepartmentNameHistory> nameHistoryOpt = departmentNameHistoryRepository
                                .findFirstByDeptNameAndStartDateLessThanEqualOrderByStartDateDesc(normalizedDeptName, startDate);

                        if (nameHistoryOpt.isEmpty()) {
                            nameHistoryOpt = departmentNameHistoryRepository.findFirstByDeptNameOrderByStartDateAsc(normalizedDeptName);
                        }

                        Optional<Department> departmentOpt = nameHistoryOpt.map(DepartmentNameHistory::getDepartment);

                        // 부서명으로도 부서를 찾지 못했다면, 로그를 남기고 다음 행으로 넘어감
                        if (departmentOpt.isEmpty()) {
                            log.warn("{}행: 부서명 '{}'에 해당하는 부서를 찾을 수 없어 이력 추가를 건너뜁니다.", rowIndex, deptName);
                            continue; // 에러를 발생시키지 않고 다음 행으로 넘어갑니다.
                        }

                        department = departmentOpt.get();
                }

                    // 요구사항: 엑셀 파일에 동일한 사원에 대해 '동일한 날짜'로 입소한 부서 이력이 여러 개 있다면,
                    // 맨 아래에 나온 이력만 DB에 저장되도록 한다. 
                    // 위에서 아래로 처리하므로 동일한 날짜의 이력이 발견되면 기존 것을 삭제하고 현재 행의 데이터로 새로 저장한다.
                    Optional<MemberDepartmentHistory> existingHistoryOpt = historyRepository
                            .findByMemberOrderByStartDateAscEndDateAsc(member)
                            .stream()
                            .filter(history -> history.getStartDate().equals(startDate))
                            .findFirst();

                    if (existingHistoryOpt.isPresent()) {
                        // 기존 이력 삭제 (가장 마지막 데이터만 남기기 위함)
                        historyRepository.delete(existingHistoryOpt.get());
                        historyRepository.flush(); // 즉시 삭제 반영
                    }

                    // 새 이력 저장
                    MemberDepartmentHistory history = MemberDepartmentHistory.builder()
                            .member(member)
                            .department(department)
                            .startDate(startDate)
                            .build();

                    historyRepository.save(history);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("사원 엑셀 파일 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 문자열 기반의 날짜 파싱 ("1974.2.1" 등 대응)
     */
    private LocalDate parseLocalDateFromString(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        dateStr = dateStr.trim();
        
        // 날짜가 숫자로만 되어있을 경우 (엑셀 날짜 일련번호)
        if (dateStr.matches("\\d+")) {
            try {
                double excelDate = Double.parseDouble(dateStr);
                Date date = DateUtil.getJavaDate(excelDate);
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception e) {
                return null;
            }
        }

        // mm/dd/yy 형식 (예: 3/31/05 -> 2005-03-31) 대응 추가
        if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{2}")) {
            String[] parts = dateStr.split("/");
            String month = parts[0];
            String day = parts[1];
            String year = parts[2];
            
            int y = Integer.parseInt(year);
            year = (y >= 50 ? "19" : "20") + year;
            if (month.length() == 1) month = "0" + month;
            if (day.length() == 1) day = "0" + day;
            
            try {
                return LocalDate.parse(year + "-" + month + "-" + day);
            } catch (Exception e) {
                return null;
            }
        }

        // 구분자가 온점(.) 혹은 슬래시(/)인 경우 하이픈(-)으로 통일
        dateStr = dateStr.replaceAll("\\s+", "").replace(".", "-").replace("/", "-");
        
        // 간혹 "1974-2-1-" 처럼 끝에 쓰레기값이 붙는 경우 제거
        if (dateStr.endsWith("-")) {
            dateStr = dateStr.substring(0, dateStr.length() - 1);
        }

        String[] parts = dateStr.split("-");
        if (parts.length >= 3) {
            // 숫자 외의 문자가 섞여 있다면 제거
            String year = parts[0].replaceAll("[^0-9]", "");
            String month = parts[1].replaceAll("[^0-9]", "");
            String day = parts[2].replaceAll("[^0-9]", "");
            
            // 연도가 2자리인 경우 4자리로 추정 (예: 99 -> 1999, 21 -> 2021)
            if (year.length() == 2) {
                int y = Integer.parseInt(year);
                year = (y >= 50 ? "19" : "20") + year;
            }
            
            // 월과 일이 한자리인 경우 앞에 0을 붙여줌
            if (month.length() == 1) {
                month = "0" + month;
            }
            if (day.length() == 1) {
                day = "0" + day;
            }
            
            try {
                return LocalDate.parse(year + "-" + month + "-" + day);
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Cell의 값을 String으로 안전하게 변환합니다.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        // DataFormatter를 사용하면 엑셀의 "보이는" 값 그대로를 가져옵니다.
        DataFormatter formatter = new DataFormatter();
        String val = formatter.formatCellValue(cell);
        return val != null ? val : "";
    }

    @Transactional
    public void updateMember(Long memberId, MemberUpdateRequestDto request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
        if (request.getName() != null) member.updateName(request.getName());

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
                if (histReq.getDeptName() != null && !histReq.getDeptName().isEmpty()) {
                    List<String> deptNamesAtTime = departmentNameHistoryRepository.findDeptNameAtTime(dept.getDepartmentId(), startDate);

                    String actualDeptName = "";
                    if (deptNamesAtTime.isEmpty()) {
                        // 해당 시점의 이력이 없다면, 최초 이력을 가져와서 비교
                        Optional<DepartmentNameHistory> firstHistory = departmentNameHistoryRepository.findFirstByDepartmentOrderByStartDateAsc(dept);
                        if (firstHistory.isPresent()) {
                            actualDeptName = firstHistory.get().getDeptName();
                        } else {
                            throw new RuntimeException(startDate + " 기준 부서명 이력이 없습니다. 부서코드: " + dept.getDeptCd());
                        }
                    } else {
                        actualDeptName = deptNamesAtTime.get(0);
                    }

                    // 검증 시에도 띄어쓰기를 모두 없앤 후 비교
                    String normalizedReqDeptName = histReq.getDeptName().replaceAll("\\s+", "");
                    if (!actualDeptName.equals(normalizedReqDeptName)) {
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
