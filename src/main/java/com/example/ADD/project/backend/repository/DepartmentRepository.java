package com.example.ADD.project.backend.repository;

import com.example.ADD.project.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByDeptCd(String deptCd);
}