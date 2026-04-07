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
import java.util.stream.Collectors;

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
    
    @Transactional
    public void createDepartment(DepartmentRequestDto request) {
        if (departmentRepository.findByDeptCd(request.getDeptCd()).isPresent()) {
            throw new RuntimeException("이미 존재하는 부서 코드입니다.");
        }
        Department department = Department.builder().deptCd(request.getDeptCd()).build();
        departmentRepository.save(department);

        if (request.getDeptName() != null) {
            DepartmentNameHistory nameHistory = DepartmentNameHistory.builder()
                    .department(department).deptName(request.getDeptName()).startDate(LocalDate.now())
                    .build();
            departmentNameHistoryRepository.save(nameHistory);
        }
    }

    @Transactional
    public void updateDepartment(Long departmentId, DepartmentRequestDto request) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다."));

        if (request.getDeptName() != null) {
            DepartmentNameHistory lastHistory = departmentNameHistoryRepository.findByDepartmentOrderByStartDateAsc(department)
                    .stream().reduce((first, second) -> second).orElse(null);
            
            if (lastHistory == null || !lastHistory.getDeptName().equals(request.getDeptName())) {
                if (lastHistory != null && lastHistory.getEndDate() == null) lastHistory.updateEndDate(LocalDate.now().minusDays(1));
                
                DepartmentNameHistory newHistory = DepartmentNameHistory.builder()
                        .department(department).deptName(request.getDeptName()).startDate(LocalDate.now())
                        .build();
                departmentNameHistoryRepository.save(newHistory);
            }
        }
    }

    /**
     * 부서 삭제 (Soft Delete)
     * 물리적으로 데이터를 삭제(DELETE)하지 않고, closedAt에 삭제 일자를 기록합니다.
     * 이를 통해 사원의 과거 소속 부서 이력을 계속해서 조회할 수 있습니다.
     */
    @Transactional
    public void deleteDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다."));
        department.updateClosedAt(LocalDate.now());
    }
}