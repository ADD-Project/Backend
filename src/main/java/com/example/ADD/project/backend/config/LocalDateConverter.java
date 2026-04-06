package com.example.ADD.project.backend.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute != null ? attribute.toString() : null;
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;
        
        // 혹시 잘못 저장된 epoch 밀리초(예: "1775489920685")가 있다면 예외 처리
        if (dbData.matches("\\d{13}")) {
            return Instant.ofEpochMilli(Long.parseLong(dbData)).atZone(ZoneId.systemDefault()).toLocalDate();
        }

        // "2012-06-30" 또는 "2012-06-30 10:00:00" 형태의 문자열에서 날짜 부분만 추출
        if (dbData.length() >= 10) {
            return LocalDate.parse(dbData.substring(0, 10));
        }
        return null;
    }
}