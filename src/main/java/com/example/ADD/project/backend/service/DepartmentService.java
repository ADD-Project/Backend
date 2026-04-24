package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.dto.department.DepartmentRequestDto;
import com.example.ADD.project.backend.dto.department.DepartmentResponseDto;
import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.DepartmentNameHistory;
import com.example.ADD.project.backend.entity.MemberDepartmentHistory;
import com.example.ADD.project.backend.repository.DepartmentNameHistoryRepository;
import com.example.ADD.project.backend.repository.DepartmentRepository;
import com.example.ADD.project.backend.repository.MemberDepartmentHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentNameHistoryRepository departmentNameHistoryRepository;
    private final MemberDepartmentHistoryRepository memberDepartmentHistoryRepository;

    /**
     * 부서 전체 조회
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .flatMap(d -> {
                    List<DepartmentNameHistory> histories =
                            departmentNameHistoryRepository.findByDepartmentOrderByStartDateAsc(d);

                    if (histories.isEmpty()) {
                        return java.util.stream.Stream.of(
                                DepartmentResponseDto.builder()
                                        .departmentId(d.getDepartmentId())
                                        .deptCd(d.getDeptCd())
                                        .deptName(d.getDeptCd())
                                        .build()
                        );
                    }

                    return histories.stream().map(h ->
                            DepartmentResponseDto.builder()
                                    .departmentId(d.getDepartmentId())
                                    .deptCd(d.getDeptCd())
                                    .deptName(h.getDeptName())
                                    .startDate(h.getStartDate())
                                    .departmentNameHistoryId(h.getDeptNameHistId())
                                    .build()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 신규 부서 생성
     */
    @Transactional
    public void createDepartment(DepartmentRequestDto request) {
        String deptCode = request.getDeptCd();

        if (deptCode == null || deptCode.trim().isEmpty()) {
            deptCode = "RANDOM_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        } else {
            if (departmentRepository.findByDeptCd(deptCode).isPresent()) {
                throw new RuntimeException("이미 존재하는 부서 코드입니다.");
            }
        }

        Department department = Department.builder()
                .deptCd(deptCode)
                .build();
        departmentRepository.save(department);

        if (request.getDeptName() != null && !request.getDeptName().isEmpty()) {
            LocalDate startDate = request.getStartDate() != null
                    ? request.getStartDate()
                    : LocalDate.now();

            DepartmentNameHistory nameHistory = DepartmentNameHistory.builder()
                    .department(department)
                    // 부서명 공백 제거 처리
                    .deptName(request.getDeptName().replaceAll("\\s+", ""))
                    .startDate(startDate)
                    .build();

            departmentNameHistoryRepository.save(nameHistory);
        }
    }

    /**
     * 부서 이름 변경 이력 수정
     */
    @Transactional
    public void updateDepartmentHistory(Long historyId, DepartmentRequestDto request) {
        DepartmentNameHistory history = departmentNameHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("해당 부서 이력을 찾을 수 없습니다."));

        Department department = history.getDepartment();

        if (request.getDeptCd() != null
                && !request.getDeptCd().trim().isEmpty()
                && !department.getDeptCd().equals(request.getDeptCd())) {

            if (department.getDeptCd().startsWith("RANDOM")) {
                Optional<Department> existDept = departmentRepository.findByDeptCd(request.getDeptCd());
                if (existDept.isPresent()
                        && !existDept.get().getDepartmentId().equals(department.getDepartmentId())) {
                    throw new RuntimeException("이미 존재하는 부서 코드입니다.");
                }
                department.updateDeptCd(request.getDeptCd());
            } else {
                throw new RuntimeException("기존에 지정된 정상 부서코드는 변경할 수 없습니다.");
            }
        }

        if (request.getDeptName() != null && !request.getDeptName().isEmpty()) {
            // 부서명 공백 제거 처리
            history.updateDeptName(request.getDeptName().replaceAll("\\s+", ""));
        }

        if (request.getStartDate() != null) {
            history.updateStartDate(request.getStartDate());
        }
    }

    /**
     * 부서 이름 이력 개별 삭제
     */
    @Transactional
    public void deleteDepartmentHistory(Long historyId) {
        DepartmentNameHistory history = departmentNameHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("해당 부서 이력을 찾을 수 없습니다."));

        Department department = history.getDepartment();
        departmentNameHistoryRepository.delete(history);

        List<DepartmentNameHistory> remainingHistories =
                departmentNameHistoryRepository.findByDepartmentOrderByStartDateAsc(department);

        if (remainingHistories.isEmpty()) {
            List<MemberDepartmentHistory> memberHistories =
                    memberDepartmentHistoryRepository.findByDepartment(department);

            if (!memberHistories.isEmpty()) {
                memberDepartmentHistoryRepository.deleteAll(memberHistories);
            }

            departmentRepository.delete(department);
        }
    }

    /**
     * 부서 엑셀 업로드
     * 형식:
     * 1열: ORGAN_CD(부서코드)
     * 2열: START_DATE(시작일)
     * 3열: ORGAN_NM(부서명)
     */
    @Transactional
    public void importDepartmentsByExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            log.info("department excel sheetName={}", sheet.getSheetName());

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                int rowIndex = i + 1;
                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                String deptCode = getCellValueAsString(row.getCell(0)).trim();
                String rawDateStr = getCellValueAsString(row.getCell(1)).trim();
                LocalDate startDate = getCellValueAsLocalDate(row.getCell(1));
                
                // 부서명을 읽어올 때 모든 띄어쓰기(공백)를 완전히 제거합니다.
                String rawDeptName = getCellValueAsString(row.getCell(2));
                String deptName = rawDeptName.replaceAll("\\s+", "");

                log.info(
                        "rowIndex={}, deptCode='{}', rawDateStr='{}', parsedStartDate={}, deptName='{}'",
                        rowIndex, deptCode, rawDateStr, startDate, deptName
                );

                // 완전 빈 행 스킵
                if (deptCode.isEmpty() && rawDateStr.isEmpty() && deptName.isEmpty()) {
                    continue;
                }

                // 헤더 중복 행 스킵
                if ("ORGAN_CD".equalsIgnoreCase(deptCode)
                        || "START_DATE".equalsIgnoreCase(rawDateStr)
                        || "ORGAN_NM".equalsIgnoreCase(rawDeptName.trim())) {
                    continue;
                }

                if (deptCode.isEmpty() || deptName.isEmpty()) {
                    throw new RuntimeException(rowIndex + "행: 부서코드와 부서명은 필수입니다.");
                }

                if (startDate == null) {
                    throw new RuntimeException(
                            rowIndex + "행: 시작일이 올바르지 않습니다. (원본 데이터: '" + rawDateStr + "')"
                    );
                }

                Department department = departmentRepository.findByDeptCd(deptCode)
                        .orElseGet(() -> departmentRepository.save(
                                Department.builder()
                                        .deptCd(deptCode)
                                        .build()
                        ));

                Optional<DepartmentNameHistory> existingHistoryOpt =
                        departmentNameHistoryRepository.findByDepartmentOrderByStartDateAsc(department)
                                .stream()
                                .filter(history -> history.getStartDate().equals(startDate))
                                .findFirst();

                if (existingHistoryOpt.isPresent()) {
                    DepartmentNameHistory existingHistory = existingHistoryOpt.get();
                    existingHistory.updateDeptName(deptName);
                } else {
                    DepartmentNameHistory history = DepartmentNameHistory.builder()
                            .department(department)
                            .deptName(deptName)
                            .startDate(startDate)
                            .build();

                    departmentNameHistoryRepository.save(history);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("부서 엑셀 파일 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 날짜 셀을 LocalDate로 안전하게 변환
     * 우선순위:
     * 1) 진짜 엑셀 날짜 셀
     * 2) 숫자 셀(엑셀 일련번호)
     * 3) 문자열 파싱
     */
    private LocalDate getCellValueAsLocalDate(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate();
                }

                double numericValue = cell.getNumericCellValue();
                Date date = DateUtil.getJavaDate(numericValue);
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }

            String dateStr = getCellValueAsString(cell);
            return parseLocalDateFromString(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 문자열 기반 날짜 파싱
     * 대응 예:
     * - 1974.2.1
     * - 1974-2-1
     * - 1974/2/1
     * - 2/1/74  (엑셀 표시 형식)
     */
    private LocalDate parseLocalDateFromString(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        dateStr = dateStr.trim().replaceAll("\\s+", "");

        try {
            // M/d/yy 또는 M/d/yyyy 형태 먼저 처리
            if (dateStr.contains("/")) {
                String[] slashParts = dateStr.split("/");
                if (slashParts.length == 3) {
                    int first = Integer.parseInt(slashParts[0]);
                    int second = Integer.parseInt(slashParts[1]);
                    int third = Integer.parseInt(slashParts[2]);

                    // 2/1/74 같은 형식
                    if (third < 100) {
                        int year = (third >= 50 ? 1900 : 2000) + third;
                        return LocalDate.of(year, first, second);
                    }

                    // 1974/2/1 같은 형식
                    if (slashParts[0].length() == 4) {
                        return LocalDate.of(first, second, third);
                    }

                    // 일반적인 M/d/yyyy 형식
                    return LocalDate.of(third, first, second);
                }
            }

            // . 또는 / 를 - 로 통일
            dateStr = dateStr.replace(".", "-").replace("/", "-");

            if (dateStr.endsWith("-")) {
                dateStr = dateStr.substring(0, dateStr.length() - 1);
            }

            String[] parts = dateStr.split("-");
            if (parts.length >= 3) {
                String p1 = parts[0].replaceAll("[^0-9]", "");
                String p2 = parts[1].replaceAll("[^0-9]", "");
                String p3 = parts[2].replaceAll("[^0-9]", "");

                if (p1.isEmpty() || p2.isEmpty() || p3.isEmpty()) {
                    return null;
                }

                int year;
                int month;
                int day;

                // yyyy-M-d
                if (p1.length() == 4) {
                    year = Integer.parseInt(p1);
                    month = Integer.parseInt(p2);
                    day = Integer.parseInt(p3);
                } else {
                    // M-d-yy 또는 M-d-yyyy
                    month = Integer.parseInt(p1);
                    day = Integer.parseInt(p2);
                    year = Integer.parseInt(p3);

                    if (year < 100) {
                        year += (year >= 50 ? 1900 : 2000);
                    }
                }

                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /**
     * Cell 값을 문자열로 안전하게 변환
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();
        String val = formatter.formatCellValue(cell);

        return val != null ? val : "";
    }
}