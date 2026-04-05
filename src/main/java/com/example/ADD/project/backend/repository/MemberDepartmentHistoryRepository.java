package com.example.ADD.project.backend.repository;

import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.entity.MemberDepartmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface MemberDepartmentHistoryRepository extends JpaRepository<MemberDepartmentHistory, Long> {
    List<MemberDepartmentHistory> findByMemberIdOrderByStartDateAsc(Long memberId);

    @Query("SELECT h.member FROM MemberDepartmentHistory h WHERE h.department.id = :departmentId " +
           "AND h.startDate <= :targetDate AND (h.endDate IS NULL OR h.endDate >= :targetDate) " +
           "AND h.member.id != :memberId")
    List<Member> findColleaguesAtTime(@Param("departmentId") Long departmentId, @Param("targetDate") LocalDate targetDate, @Param("memberId") Long memberId);
}