package com.example.ADD.project.backend.repository;

import com.example.ADD.project.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 회원 데이터베이스 접근을 위한 레포지토리 인터페이스
 * JpaRepository를 상속받아 기본적인 CRUD 메서드를 제공합니다.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 회원 코드로 회원을 조회합니다. (로그인 시 사용)
     * @param userCode 회원 고유 코드
     * @return 조회된 회원 (Optional)
     */
    Optional<User> findByUserCode(String userCode);
    
    /**
     * 이름에 특정 문자열이 포함된 회원들을 검색합니다.
     * @param name 검색할 이름 (부분 일치)
     * @return 검색된 회원 리스트
     */
    List<User> findByNameContaining(String name);

    /**
     * 특정 부서에 속해있으며, 특정 입소일자 이전(또는 같은 날)에 입소한 부서원들을 조회합니다.
     * @param departmentId 부서 ID
     * @param admissionDate 기준 입소 일자
     * @param excludeUserId 제외할 사용자 ID (본인 제외용)
     * @return 부서원 리스트
     */
    @Query("SELECT u FROM User u WHERE u.department.id = :departmentId AND u.admissionDate <= :admissionDate AND u.id != :excludeUserId")
    List<User> findDepartmentMembersBeforeOrAtAdmissionDate(
            @Param("departmentId") Long departmentId,
            @Param("admissionDate") LocalDate admissionDate,
            @Param("excludeUserId") Long excludeUserId);
}