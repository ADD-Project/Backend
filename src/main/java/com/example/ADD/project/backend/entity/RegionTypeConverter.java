package com.example.ADD.project.backend.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RegionTypeConverter implements AttributeConverter<RegionType, String> {

    @Override
    public String convertToDatabaseColumn(RegionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDescription();
    }

    @Override
    public RegionType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return RegionType.from(dbData);
    }
}