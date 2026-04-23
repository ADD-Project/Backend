package com.example.ADD.project.backend.controller;

import com.example.ADD.project.backend.dto.department.DepartmentRequestDto;
import com.example.ADD.project.backend.dto.department.DepartmentResponseDto;
import com.example.ADD.project.backend.dto.ApiResponse;
import com.example.ADD.project.backend.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PutMapping("/departments/{departmentNameHistoryId}")
    public ApiResponse<String> updateDepartmentHistory(@PathVariable Long departmentNameHistoryId, @RequestBody DepartmentRequestDto request) {
        departmentService.updateDepartmentHistory(departmentNameHistoryId, request);
        return ApiResponse.success("200", "부서 수정 성공", null);
    }

    @GetMapping("/departments")
    public ApiResponse<List<DepartmentResponseDto>> getAllDepartments() {
        return ApiResponse.success("200", "부서 조회 성공", departmentService.getAllDepartments());
    }

    @DeleteMapping("/departments/{departmentNameHistoryId}")
    public ApiResponse<String> deleteDepartmentHistory(@PathVariable Long departmentNameHistoryId) {
        departmentService.deleteDepartmentHistory(departmentNameHistoryId);
        return ApiResponse.success("200", "부서 이력 삭제 성공", null);
    }

    @PostMapping("/departments/excel")
    public ApiResponse<String> uploadDepartmentsExcel(@RequestParam("file") MultipartFile file) {
        try {
            departmentService.importDepartmentsByExcel(file);
            return ApiResponse.success("200", "부서 엑셀 업로드 성공", null);
        } catch (Exception e) {
            return ApiResponse.error("400", e.getMessage());
        }
    }
}
