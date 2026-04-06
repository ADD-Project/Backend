package com.example.ADD.project.backend.controller;

import com.example.ADD.project.backend.dto.department.DepartmentRequestDto;
import com.example.ADD.project.backend.dto.department.DepartmentResponseDto;
import com.example.ADD.project.backend.dto.ApiResponse;
import com.example.ADD.project.backend.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping("/departments")
    public ApiResponse<String> createDepartment(@RequestBody DepartmentRequestDto request) {
        departmentService.createDepartment(request);
        return ApiResponse.success("200", "부서 생성 성공", null);
    }

    @PutMapping("/departments/{departmentId}")
    public ApiResponse<String> updateDepartment(@PathVariable Long departmentId, @RequestBody DepartmentRequestDto request) {
        departmentService.updateDepartment(departmentId, request);
        return ApiResponse.success("200", "부서 수정 성공", null);
    }

    @GetMapping("/departments")
    public ApiResponse<List<DepartmentResponseDto>> getAllDepartments() {
        return ApiResponse.success("200", "부서 조회 성공", departmentService.getAllDepartments());
    }
}