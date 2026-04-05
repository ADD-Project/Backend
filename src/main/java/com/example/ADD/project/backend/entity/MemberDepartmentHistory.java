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
    name = "member_department_history",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_member_department_history",
            columnNames = {"member_id", "department_id", "start_date"}
        )
    }
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

    @Enumerated(EnumType.STRING)
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