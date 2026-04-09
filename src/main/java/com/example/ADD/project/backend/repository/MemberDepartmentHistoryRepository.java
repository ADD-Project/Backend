package com.example.ADD.project.backend.repository;

import com.example.ADD.project.backend.entity.Member;
import com.example.ADD.project.backend.entity.MemberDepartmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberDepartmentHistoryRepository extends JpaRepository<MemberDepartmentHistory, Long> {

    // 사원의 부서 이력을 시작일 오름차순, 시작일이 같으면 종료일 오름차순으로 조회
    List<MemberDepartmentHistory> findByMemberOrderByStartDateAscEndDateAsc(Member member);

    @Query("SELECT h.member FROM MemberDepartmentHistory h WHERE h.department.departmentId = :departmentId " +
           "AND h.startDate <= :targetDate AND (h.endDate IS NULL OR h.endDate >= :targetDate) " +
           "AND h.member.memberId != :memberId")
    List<Member> findColleaguesAtTime(@Param("departmentId") Long departmentId, @Param("targetDate") LocalDate targetDate, @Param("memberId") Long memberId);

    @Query("SELECT h.member FROM MemberDepartmentHistory h " +
            "WHERE h.startDate >= :startOfYear AND h.startDate <= :endOfYear " +
            "AND h.startDate = (SELECT MIN(h2.startDate) FROM MemberDepartmentHistory h2 WHERE h2.member = h.member)")
    List<Member> findMembersByAdmissionYear(@Param("startOfYear") LocalDate startOfYear, @Param("endOfYear") LocalDate endOfYear);

    Optional<MemberDepartmentHistory> findTopByMemberOrderByStartDateDesc(Member member);
}