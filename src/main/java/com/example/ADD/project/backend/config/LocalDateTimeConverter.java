package com.example.ADD.project.backend.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        if (attribute == null) return null;
        // SQLite에 저장할 때 명확한 문자열(YYYY-MM-DD HH:MM:SS) 포맷으로 변환하여 저장
        return attribute.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;

        // 혹시 잘못 저장된 epoch 밀리초(예: "1775489920685")가 있다면 예외 처리
        if (dbData.matches("\\d{13}")) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(dbData)), ZoneId.systemDefault());
        }

        // ISO 포맷인 'T'를 공백으로 치환 ("2026-04-05T10:00:00" -> "2026-04-05 10:00:00")
        String normalized = dbData.replace("T", " ");
        
        // 시간 부분이 아예 없다면 추가 ("2012-06-30" -> "2012-06-30 00:00:00")
        if (normalized.length() == 10) {
            normalized += " 00:00:00";
        }

        // 소수점 이하 밀리초가 포함되어 있다면 제거 (.000)
        if (normalized.contains(".")) {
            normalized = normalized.substring(0, normalized.indexOf("."));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(normalized, formatter);
    }
}