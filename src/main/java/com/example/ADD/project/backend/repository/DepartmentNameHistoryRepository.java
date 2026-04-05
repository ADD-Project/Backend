package com.example.ADD.project.backend.repository;

import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.DepartmentNameHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentNameHistoryRepository extends JpaRepository<DepartmentNameHistory, Long> {

    List<DepartmentNameHistory> findByDepartmentOrderByStartDateAsc(Department department);

    Optional<DepartmentNameHistory> findFirstByDepartmentAndDeptNameOrderByStartDateAsc(Department department, String deptName);
}