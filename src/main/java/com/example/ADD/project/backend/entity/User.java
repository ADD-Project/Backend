package com.example.ADD.project.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * 회원 엔티티 클래스
 * 데이터베이스의 'users' 테이블과 매핑됩니다.
 */
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 회원 고유 ID (PK)

    private String name; // 회원 이름
    private String email; // 회원 이메일
    private String userCode; // 회원 로그인용 고유 코드
    private String profileImageUrl; // 회원 프로필 이미지 URL
    private LocalDate admissionDate; // 입소(입사) 일자

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department; // 소속 부서

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
}