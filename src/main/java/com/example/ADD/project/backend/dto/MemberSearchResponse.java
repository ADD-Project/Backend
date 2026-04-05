package com.example.ADD.project.backend.dto;

public class MemberSearchResponse {
    private String name;
    private String profileImageUrl;

    public MemberSearchResponse() {
    }

    public MemberSearchResponse(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}