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
    name = "member_department_history"
    // UNIQUE 제약조건 해제: 동일한 시작일(startDate)에 여러 부서 이동 이력이 발생할 수 있으므로 제거함
)
public class MemberDepartmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_department_history_id")
    private Long memberDepartmentHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "region_name", nullable = false, length = 20)
    private RegionType regionName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MemberDepartmentHistory(Member member, Department department, RegionType regionName,
                                   LocalDate startDate, LocalDate endDate) {
        this.member = member;
        this.department = department;
        this.regionName = regionName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void updateRegionName(RegionType regionName) {
        this.regionName = regionName;
    }
}