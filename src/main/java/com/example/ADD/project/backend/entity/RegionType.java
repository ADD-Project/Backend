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
        for (RegionType type : values()) {
            if (type.description.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 지역명입니다: " + value);
    }
}