package com.example.ADD.project.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "member",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_member_code", columnNames = "member_code")
    }
)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_code", nullable = false, length = 30)
    private String memberCode;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "profile_image_path", length = 255)
    private String profileImagePath;

    @Builder
    public Member(String memberCode, String name, String profileImagePath) {
        this.memberCode = memberCode;
        this.name = name;
        this.profileImagePath = profileImagePath;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }
}