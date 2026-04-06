package com.example.ADD.project.backend.repository;

import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.DepartmentNameHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DepartmentNameHistoryRepository extends JpaRepository<DepartmentNameHistory, Long> {

    List<DepartmentNameHistory> findByDepartmentOrderByStartDateAsc(Department department);

    Optional<DepartmentNameHistory> findFirstByDepartmentAndDeptNameOrderByStartDateAsc(Department department, String deptName);

    @Query("SELECT dnh.deptName FROM DepartmentNameHistory dnh " +
           "WHERE dnh.department.departmentId = :departmentId " +
           "AND dnh.startDate <= :targetDate AND (dnh.endDate IS NULL OR dnh.endDate >= :targetDate)")
    Optional<String> findDeptNameAtTime(@Param("departmentId") Long departmentId, @Param("targetDate") LocalDate targetDate);
}