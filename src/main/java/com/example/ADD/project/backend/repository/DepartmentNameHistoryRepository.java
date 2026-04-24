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

    Optional<DepartmentNameHistory>
    findTopByDeptNameAndStartDateLessThanEqualOrderByStartDateDesc(
            String deptName,
            LocalDate startDate
    );

    Optional<DepartmentNameHistory> findFirstByDeptNameAndStartDateLessThanEqualOrderByStartDateDesc(String deptName, LocalDate targetDate);

    Optional<DepartmentNameHistory> findFirstByDeptNameOrderByStartDateAsc(String deptName);

    Optional<DepartmentNameHistory> findFirstByDepartmentOrderByStartDateAsc(Department department);
    
    // 추가된 쿼리 메서드
    Optional<DepartmentNameHistory> findFirstByDepartmentAndStartDateLessThanEqualOrderByStartDateDesc(Department department, LocalDate targetDate);
    
    Optional<DepartmentNameHistory> findFirstByDeptNameAndStartDateGreaterThanOrderByStartDateAsc(String deptName, LocalDate targetDate);
    
    List<DepartmentNameHistory> findByDeptName(String deptName);
}
