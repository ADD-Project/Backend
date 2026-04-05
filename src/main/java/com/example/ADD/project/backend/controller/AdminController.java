package com.example.ADD.project.backend.controller;

import com.example.ADD.project.backend.dto.AdminDto;
import com.example.ADD.project.backend.dto.AdminLoginRequest;
import com.example.ADD.project.backend.entity.Admin;
import com.example.ADD.project.backend.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<AdminDto> createAdmin(@RequestBody AdminDto adminDto) {
        Admin admin = new Admin();
        admin.setName(adminDto.getName());
        admin.setEmail(adminDto.getEmail());
        
        Admin savedAdmin = adminService.saveAdmin(admin);
        
        return ResponseEntity.ok(new AdminDto(savedAdmin.getId(), savedAdmin.getName(), savedAdmin.getEmail()));
    }

    @GetMapping
    public ResponseEntity<List<AdminDto>> getAllAdmins() {
        List<Admin> admins = adminService.findAllAdmins();
        List<AdminDto> adminDtos = admins.stream()
                .map(admin -> new AdminDto(admin.getId(), admin.getName(), admin.getEmail()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(adminDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminDto> getAdmin(@PathVariable Long id) {
        Admin admin = adminService.findById(id);
        if (admin == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new AdminDto(admin.getId(), admin.getName(), admin.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AdminLoginRequest request) {
        Admin admin = adminService.login(request.getPassword());
        
        if (admin != null) {
            // TODO: JWT 토큰 생성 또는 세션 처리 등 실제 인증 로직 추가
            return ResponseEntity.ok("관리자 로그인 성공 (ID: " + admin.getId() + ")");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("비밀번호가 일치하지 않습니다.");
        }
    }
}