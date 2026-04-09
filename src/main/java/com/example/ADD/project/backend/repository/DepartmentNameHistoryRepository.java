package com.example.ADD.project.backend.repository;

import com.example.ADD.project.backend.entity.Department;
import com.example.ADD.project.backend.entity.DepartmentNameHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface DepartmentNameHistoryRepository extends JpaRepository<DepartmentNameHistory, Long> {

    List<DepartmentNameHistory> findByDepartmentOrderByStartDateAsc(Department department);

    // Optional 대신 List로 반환하여 2개 이상 나올 경우 가장 첫 번째 요소를 사용할 수 있도록 수정
    @Query("SELECT dnh.deptName FROM DepartmentNameHistory dnh " +
           "WHERE dnh.department.departmentId = :departmentId " +
           "AND dnh.startDate <= :targetDate AND (dnh.endDate IS NULL OR dnh.endDate >= :targetDate) " +
           "ORDER BY dnh.startDate DESC")
    List<String> findDeptNameAtTime(@Param("departmentId") Long departmentId, @Param("targetDate") LocalDate targetDate);
}
