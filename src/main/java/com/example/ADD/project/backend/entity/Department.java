package com.example.ADD.project.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "department",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_department_dept_cd", columnNames = "dept_cd")
    }
)
public class Department {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "dept_cd", nullable = false, length = 30)
    private String deptCd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDate closedAt;

    @Builder
    public Department(String deptCd, LocalDate closedAt) {
        this.deptCd = deptCd;
        this.closedAt = closedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateClosedAt(LocalDate closedAt) {
        this.closedAt = closedAt;
    }
}