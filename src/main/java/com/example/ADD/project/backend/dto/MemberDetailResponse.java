package com.example.ADD.project.backend.dto;

import java.time.LocalDate;
import java.util.List;

public class MemberDetailResponse {
    private Long memberId;
    private String memberCode;
    private String name;
    private String profileImageUrl;
    private LocalDate admissionDate;
    private DepartmentDto admissionDepartment;
    private List<DepartmentMemberDto> departmentMembers;

    public MemberDetailResponse() {}

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }
    public DepartmentDto getAdmissionDepartment() { return admissionDepartment; }
    public void setAdmissionDepartment(DepartmentDto admissionDepartment) { this.admissionDepartment = admissionDepartment; }
    public List<DepartmentMemberDto> getDepartmentMembers() { return departmentMembers; }
    public void setDepartmentMembers(List<DepartmentMemberDto> departmentMembers) { this.departmentMembers = departmentMembers; }

    public static class DepartmentDto {
        private Long departmentId;
        private String departmentCode;
        private String departmentName;

        public DepartmentDto() {}

        public DepartmentDto(Long departmentId, String departmentCode, String departmentName) {
            this.departmentId = departmentId;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
        }

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    }

    public static class DepartmentMemberDto {
        private Long memberId;
        private String memberCode;
        private String name;
        private String profileImageUrl;

        public DepartmentMemberDto() {}

        public DepartmentMemberDto(Long memberId, String memberCode, String name, String profileImageUrl) {
            this.memberId = memberId;
            this.memberCode = memberCode;
            this.name = name;
            this.profileImageUrl = profileImageUrl;
        }

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public String getMemberCode() { return memberCode; }
        public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    }
}