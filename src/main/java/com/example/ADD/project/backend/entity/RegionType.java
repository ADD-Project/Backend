package com.example.ADD.project.backend.entity;

public enum RegionType {
    BUSAN("부산"),
    MASAN("마산");

    private final String description;

    RegionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static RegionType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            // 값이 없는 경우 기본값으로 처리하거나 null 반환을 고려할 수 있습니다.
            // 여기서는 예외를 던지기 전에 null 체크를 추가하여 오류를 방지합니다.
            return null; // 또는 throw new IllegalArgumentException("지역명이 비어있습니다.");
        }
        for (RegionType type : values()) {
            if (type.description.equals(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 지역명입니다: " + value);
    }
}
