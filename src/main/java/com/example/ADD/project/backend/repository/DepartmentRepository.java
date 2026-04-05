package com.example.ADD.project.backend.repository;

import com.example.ADD.project.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}