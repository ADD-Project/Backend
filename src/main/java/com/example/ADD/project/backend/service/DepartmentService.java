package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.dto.department.DepartmentRequestDto;
import com.example.ADD.project.backend.dto.department.DepartmentResponseDto;
import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.DepartmentNameHistory;
import com.example.ADD.project.backend.repository.DepartmentNameHistoryRepository;
import com.example.ADD.project.backend.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentNameHistoryRepository departmentNameHistoryRepository;

    /**
     * 부서 전체 조회
     * Soft Delete 된 부서(closedAt이 null이 아닌 부서)는 제외하고 활성화된 부서만 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments() {
        return departmentRepository.findByClosedAtIsNull().stream()
                .map(d -> new DepartmentResponseDto(d.getDepartmentId(), d.getDeptCd(), d.getClosedAt()))
                .collect(Collectors.toList());
    }

    /**
     * 신규 부서 생성
     */
    @Transactional
    public void createDepartment(DepartmentRequestDto request) {
        String deptCode = request.getDeptCd();
        
        // 1. 부서코드를 입력하지 않는 경우, 임의로 'RANDOM_XXXX' 형식으로 세팅
        if (deptCode == null || deptCode.trim().isEmpty()) {
            deptCode = "RANDOM_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        } else {
            if (departmentRepository.findByDeptCd(deptCode).isPresent()) {
                throw new RuntimeException("이미 존재하는 부서 코드입니다.");
            }
        }
        
        Department department = Department.builder().deptCd(deptCode).build();
        departmentRepository.save(department);

        // 2. 부서명이 함께 제공된 경우, 부서명 이력 생성
        if (request.getDeptName() != null && !request.getDeptName().trim().isEmpty()) {
            LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
            DepartmentNameHistory nameHistory = DepartmentNameHistory.builder()
                    .department(department).deptName(request.getDeptName()).startDate(startDate)
                    .build();
            departmentNameHistoryRepository.save(nameHistory);
        }
    }

    /**
     * 기존 부서 수정
     */
    @Transactional
    public void updateDepartment(Long departmentId, DepartmentRequestDto request) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다."));

        // 1. RANDOM으로 시작하는 기존 부서의 부서코드 수정 로직
        if (department.getDeptCd().startsWith("RANDOM") 
            && request.getDeptCd() != null && !request.getDeptCd().trim().isEmpty()) {
            
            // 입력하려는 부서코드가 이미 존재하는지 확인
            Optional<Department> existDept = departmentRepository.findByDeptCd(request.getDeptCd());
            if (existDept.isPresent() && !existDept.get().getDepartmentId().equals(departmentId)) {
                throw new RuntimeException("이미 존재하는 부서 코드입니다.");
            }
            
            department.updateDeptCd(request.getDeptCd());
        }

        // 2. 부서명 변경 이력 추가 로직
        if (request.getDeptName() != null && !request.getDeptName().trim().isEmpty()) {
            
            // 현재 부서의 가장 최근 이력을 가져온다 (startDate 기준 오름차순으로 정렬한 뒤 가장 마지막 요소)
            DepartmentNameHistory lastHistory = departmentNameHistoryRepository.findByDepartmentOrderByStartDateAsc(department)
                    .stream().reduce((first, second) -> second).orElse(null);
            
            LocalDate newStartDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

            if (lastHistory == null) {
                // 이전 이력이 아예 없는 경우 새롭게 생성
                DepartmentNameHistory newHistory = DepartmentNameHistory.builder()
                        .department(department).deptName(request.getDeptName()).startDate(newStartDate)
                        .build();
                departmentNameHistoryRepository.save(newHistory);
            } else if (!lastHistory.getDeptName().equals(request.getDeptName())) {
                // 부서 이름이 달라졌을 경우
                
                // 입력한 변경 일자가 DB에 저장된 가장 최근 이력의 시작일보다 '이전'인지 체크
                if (newStartDate.isBefore(lastHistory.getStartDate())) {
                    // 과거 날짜의 이력을 중간에 삽입하는 케이스 (요구사항 반영)
                    // 이 경우, 삽입할 과거 이력의 종료일은 세팅하지 않음(null로 둠)
                    DepartmentNameHistory newHistory = DepartmentNameHistory.builder()
                            .department(department)
                            .deptName(request.getDeptName())
                            .startDate(newStartDate)
                            // endDate는 세팅하지 않음 (null)
                            .build();
                    departmentNameHistoryRepository.save(newHistory);
                    
                } else if (newStartDate.isEqual(lastHistory.getStartDate())) {
                    // 동일한 날짜에 부서명이 바뀌는 경우 -> 기존 이력을 수정 (또는 에러 처리)
                    throw new RuntimeException("가장 최근 부서 이름 변경일과 동일한 날짜로 이력을 추가할 수 없습니다.");
                } else {
                    // 정상적인 최신 미래 날짜 변경: 기존 이력을 닫지 않고 새 이력만 추가함
                    DepartmentNameHistory newHistory = DepartmentNameHistory.builder()
                            .department(department)
                            .deptName(request.getDeptName())
                            .startDate(newStartDate)
                            .build();
                    departmentNameHistoryRepository.save(newHistory);
                }
            }
        }
    }

    /**
     * 부서 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다."));
        department.updateClosedAt(LocalDate.now());
    }
}