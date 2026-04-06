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

    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
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
}