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

    @Query("SELECT dnh.deptName FROM DepartmentNameHistory dnh " +
            "WHERE dnh.department.departmentId = :departmentId " +
            "AND dnh.startDate <= :targetDate " +
            "ORDER BY dnh.startDate DESC")
    List<String> findDeptNameAtTime(@Param("departmentId") Long departmentId,
                                    @Param("targetDate") LocalDate targetDate);

    // 부서명으로 특정 시점에 가장 가까운(이전) 부서 찾기 (단일 건 조회)
    @Query(value = "SELECT dnh.department FROM DepartmentNameHistory dnh " +
            "WHERE dnh.deptName = :deptName AND dnh.startDate <= :targetDate " +
            "ORDER BY dnh.startDate DESC LIMIT 1")
    Optional<Department> findDepartmentByNameAtTime(@Param("deptName") String deptName,
                                                    @Param("targetDate") LocalDate targetDate);
}
