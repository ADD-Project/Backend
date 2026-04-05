package com.example.ADD.project.backend.service;

import com.example.ADD.project.backend.entity.Admin;
import com.example.ADD.project.backend.repository.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Transactional
    public Admin saveAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    public List<Admin> findAllAdmins() {
        return adminRepository.findAll();
    }
    
    public Admin findById(Long id) {
        return adminRepository.findById(id).orElse(null);
    }

    public Admin login(String password) {
        return adminRepository.findByPassword(password).orElse(null);
    }
}