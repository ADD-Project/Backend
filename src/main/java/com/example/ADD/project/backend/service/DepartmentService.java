package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.dto.department.DepartmentResponseDto;
import com.example.ADD.project.backend.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentResponseDto(d.getId(), d.getDeptCd(), d.getClosedAt()))
                .collect(Collectors.toList());
    }
    
    // 부서 생성, 수정 등 기타 로직...
}